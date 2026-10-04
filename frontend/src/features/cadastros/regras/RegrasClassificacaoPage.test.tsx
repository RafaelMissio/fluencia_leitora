import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { RegraClassificacaoResponse } from '../../../api/types'
import { RegrasClassificacaoPage } from './RegrasClassificacaoPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function regra(id: number, min: number, max: number | null, fase: RegraClassificacaoResponse['fase'], nivel: number | null): RegraClassificacaoResponse {
  return { id, serie: 1, quantidadeMinimaAcertos: min, quantidadeMaximaAcertos: max, fase, nivel, ativo: true }
}

const REGRAS = [
  regra(1, 0, 14, 'PRE_LEITOR', 1),
  regra(2, 15, 29, 'LEITOR_INICIANTE', null),
  regra(3, 30, null, 'LEITOR_FLUENTE', null),
]

function mockApi(put?: () => Response): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url.startsWith('/api/v1/regras-classificacao?serie=') && method === 'GET') return jsonResponse(200, REGRAS)
    if (url.startsWith('/api/v1/regras-classificacao/series/') && method === 'PUT') return put!()
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <RegrasClassificacaoPage />
    </QueryClientProvider>,
  )
}

describe('RegrasClassificacaoPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the 0-60 ruler with one color/legend per fase and no warnings for a complete set', async () => {
    mockApi()
    renderPage()

    expect(await screen.findByText('PRE_LEITOR (nível 1): 0–14 acertos')).toBeInTheDocument()
    expect(screen.getByText('LEITOR_INICIANTE: 15–29 acertos')).toBeInTheDocument()
    expect(screen.getByText('LEITOR_FLUENTE: 30–60 acertos')).toBeInTheDocument()
    expect(screen.queryByRole('note')).not.toBeInTheDocument()
  })

  it('highlights a gap before sending when a faixa leaves acertos uncovered', async () => {
    mockApi()
    const user = userEvent.setup()
    renderPage()
    const minimo = await screen.findByLabelText('Mínimo da faixa 2')

    await user.clear(minimo)
    await user.type(minimo, '20')

    expect(screen.getByText('Lacuna: 15–19 acertos')).toBeInTheDocument()
    expect(screen.getByRole('note')).toHaveTextContent('lacunas')
  })

  it('highlights an overlap before sending', async () => {
    mockApi()
    const user = userEvent.setup()
    renderPage()
    const minimo = await screen.findByLabelText('Mínimo da faixa 2')

    await user.clear(minimo)
    await user.type(minimo, '10')

    expect(screen.getByText('Sobreposição: 10–14 acertos')).toBeInTheDocument()
    expect(screen.getByRole('note')).toHaveTextContent('sobreposição')
  })

  it('saves a valid set, showing "Salvo com sucesso" and sending the faixas', async () => {
    mockApi(() => jsonResponse(200, REGRAS))
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Mínimo da faixa 1')

    await user.click(screen.getByRole('button', { name: 'Salvar faixas' }))

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    const put = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(String(put?.[0])).toBe('/api/v1/regras-classificacao/series/1')
    expect(JSON.parse(String(put?.[1]?.body)).faixas).toEqual([
      { quantidadeMinimaAcertos: 0, quantidadeMaximaAcertos: 14, fase: 'PRE_LEITOR', nivel: 1 },
      { quantidadeMinimaAcertos: 15, quantidadeMaximaAcertos: 29, fase: 'LEITOR_INICIANTE', nivel: null },
      { quantidadeMinimaAcertos: 30, quantidadeMaximaAcertos: null, fase: 'LEITOR_FLUENTE', nivel: null },
    ])
  })

  it('shows the backend 422 message at the top and keeps the edits', async () => {
    mockApi(() => jsonResponse(422, { code: 'FAIXA_COM_LACUNA', detail: 'Há uma lacuna entre as faixas' }))
    const user = userEvent.setup()
    renderPage()
    const minimo = await screen.findByLabelText('Mínimo da faixa 2')
    await user.clear(minimo)
    await user.type(minimo, '20')

    await user.click(screen.getByRole('button', { name: 'Salvar faixas' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Há uma lacuna entre as faixas')
    expect(screen.getByLabelText('Mínimo da faixa 2')).toHaveValue(20)
  })
})
