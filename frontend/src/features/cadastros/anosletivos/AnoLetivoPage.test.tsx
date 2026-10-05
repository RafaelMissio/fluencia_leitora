import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnoLetivoResponse, ConfiguracaoAvaliacaoResponse } from '../../../api/types'
import { AnoLetivoPage } from './AnoLetivoPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const ANO_2025: AnoLetivoResponse = {
  id: 1, ano: 2025, dataInicio: '2025-02-01', dataFim: '2025-12-15', situacao: 'ATIVO', ativo: true,
}
const ANO_2026: AnoLetivoResponse = {
  id: 2, ano: 2026, dataInicio: '2026-02-01', dataFim: '2026-12-15', situacao: 'PLANEJADO', ativo: true,
}
const CONFIGURACOES: ConfiguracaoAvaliacaoResponse[] = [
  { id: 10, serie: 1, quantidadeMinima: 15, quantidadeMaxima: 20 },
  { id: 11, serie: 2, quantidadeMinima: 20, quantidadeMaxima: 60 },
]

interface Estado {
  anos: AnoLetivoResponse[]
  post?: () => Response
  put?: () => Response
}

function mockApi(estado: Estado): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url === '/api/v1/anos-letivos' && method === 'GET') return jsonResponse(200, estado.anos)
    if (url === '/api/v1/anos-letivos' && method === 'POST') return estado.post!()
    if (url.endsWith('/configuracoes') && method === 'GET') return jsonResponse(200, CONFIGURACOES)
    if (url.includes('/configuracoes/') && method === 'PUT') return estado.put!()
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage(modo: 'cadastrar' | 'listar' = 'listar') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AnoLetivoPage modo={modo} />
    </QueryClientProvider>,
  )
}

async function preencherEEnviar(user: ReturnType<typeof userEvent.setup>): Promise<void> {
  await user.type(screen.getByLabelText('Ano'), '2026')
  await user.type(screen.getByLabelText('Data de início'), '2026-02-01')
  await user.type(screen.getByLabelText('Data de fim'), '2026-12-15')
  await user.click(screen.getByRole('button', { name: 'Criar ano letivo' }))
}

describe('AnoLetivoPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates a valid ano letivo, shows "Salvo com sucesso" and lists it', async () => {
    const estado: Estado = { anos: [ANO_2025] }
    estado.post = () => {
      estado.anos = [ANO_2026, ANO_2025]
      return jsonResponse(201, ANO_2026)
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage('cadastrar')

    await preencherEEnviar(user)

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
  })

  it('shows the 409 code message at the top of the form', async () => {
    mockApi({
      anos: [ANO_2025],
      post: () => jsonResponse(409, { code: 'ANO_LETIVO_DUPLICADO', detail: 'Ano letivo 2026 já cadastrado' }),
    })
    const user = userEvent.setup()
    renderPage('cadastrar')

    await preencherEEnviar(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('Ano letivo 2026 já cadastrado')
    expect(screen.queryByText('Salvo com sucesso')).not.toBeInTheDocument()
  })

  it('shows the 422 message next to the offending field', async () => {
    mockApi({
      anos: [ANO_2025],
      post: () =>
        jsonResponse(422, { errors: [{ field: 'ano', message: 'deve ser maior ou igual a 2000' }] }),
    })
    const user = userEvent.setup()
    renderPage('cadastrar')

    await preencherEEnviar(user)

    expect(await screen.findByText('deve ser maior ou igual a 2000')).toBeInTheDocument()
  })

  it('edits the min/max of a série and shows "Salvo com sucesso"', async () => {
    mockApi({
      anos: [ANO_2025],
      put: () => jsonResponse(200, { id: 10, serie: 1, quantidadeMinima: 16, quantidadeMaxima: 20 }),
    })
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Configurar 2025' }))

    const minimo = await screen.findByLabelText('Mínimo da série 1')
    await user.clear(minimo)
    await user.type(minimo, '16')
    await user.click(screen.getByRole('button', { name: 'Salvar série 1' }))

    await waitFor(() => expect(screen.getByText('Salvo com sucesso')).toBeInTheDocument())
    const putCall = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(String(putCall?.[0])).toBe('/api/v1/anos-letivos/1/configuracoes/1')
    expect(JSON.parse(String(putCall?.[1]?.body))).toEqual({ quantidadeMinima: 16, quantidadeMaxima: 20 })
  })
})
