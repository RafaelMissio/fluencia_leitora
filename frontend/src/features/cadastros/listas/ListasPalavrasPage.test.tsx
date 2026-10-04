import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ListaPalavrasResponse } from '../../../api/types'
import { ListasPalavrasPage } from './ListasPalavrasPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

interface Resumo { id: number; nome: string; quantidadePalavras: number }

interface Estado {
  listas: Resumo[]
  post?: () => Response
  put?: () => Response
}

const DETALHE: ListaPalavrasResponse = {
  id: 1, nome: 'Lista A', serie: 2, tipoLeitura: 'PALAVRA', tipoPalavra: 'CANONICA', texto: null,
  ativo: true, quantidadePalavras: 2, version: 4,
  itens: [{ palavra: 'gato', tipoPalavra: 'CANONICA', ordem: 1 }, { palavra: 'bola', tipoPalavra: 'CANONICA', ordem: 2 }],
}

function mockApi(estado: Estado): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url.startsWith('/api/v1/listas-palavras?') && method === 'GET') return jsonResponse(200, estado.listas)
    if (url === '/api/v1/listas-palavras' && method === 'POST') return estado.post!()
    if (url === '/api/v1/listas-palavras/1' && method === 'GET') return jsonResponse(200, DETALHE)
    if (url === '/api/v1/listas-palavras/1' && method === 'PUT') return estado.put!()
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ListasPalavrasPage />
    </QueryClientProvider>,
  )
}

async function preencher(user: ReturnType<typeof userEvent.setup>, serie: string): Promise<void> {
  await user.type(screen.getByLabelText('Nome'), 'Lista Nova')
  await user.selectOptions(screen.getByLabelText('Série', { selector: '#lista-serie' }), serie)
  await user.type(screen.getByLabelText('Palavras (separadas por espaço)'), 'gato bola')
}

describe('ListasPalavrasPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates a valid lista (série 2), allows NAO_CANONICA and shows "Salvo com sucesso"', async () => {
    const estado: Estado = { listas: [], post: () => jsonResponse(201, { ...DETALHE, id: 5 }) }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()

    await preencher(user, '2')
    const opcao = screen.getByRole('option', { name: 'NAO_CANONICA' })
    expect(opcao).toBeEnabled()
    await user.selectOptions(screen.getByLabelText('Tipo de palavra'), 'NAO_CANONICA')
    await user.click(screen.getByRole('button', { name: 'Criar lista' }))

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    const post = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'POST')
    expect(JSON.parse(String(post?.[1]?.body))).toEqual({
      nome: 'Lista Nova', serie: 2, tipoLeitura: 'PALAVRA', tipoPalavra: 'NAO_CANONICA',
      itens: [{ palavra: 'gato', tipoPalavra: 'NAO_CANONICA' }, { palavra: 'bola', tipoPalavra: 'NAO_CANONICA' }],
    })
  })

  it('disables NAO_CANONICA for the 1º ano and sends CANONICA', async () => {
    mockApi({ listas: [], post: () => jsonResponse(201, { ...DETALHE, id: 5, serie: 1 }) })
    const user = userEvent.setup()
    renderPage()

    await preencher(user, '1')
    expect(screen.getByRole('option', { name: 'NAO_CANONICA' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Criar lista' }))

    await screen.findByText('Salvo com sucesso')
    const post = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'POST')
    expect(JSON.parse(String(post?.[1]?.body)).itens[0].tipoPalavra).toBe('CANONICA')
  })

  it('shows the 409 message at the top and the 422 message next to the field', async () => {
    const estado: Estado = {
      listas: [],
      post: () => jsonResponse(409, { code: 'LISTA_DUPLICADA', detail: 'Já existe uma lista com esse nome' }),
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()

    await preencher(user, '2')
    await user.click(screen.getByRole('button', { name: 'Criar lista' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe uma lista com esse nome')

    estado.post = () => jsonResponse(422, { errors: [{ field: 'nome', message: 'tamanho deve ser entre 3 e 100' }] })
    await user.click(screen.getByRole('button', { name: 'Criar lista' }))
    expect(await screen.findByText('tamanho deve ser entre 3 e 100')).toBeInTheDocument()
  })

  it('edits an existing lista sending the version from the detail', async () => {
    mockApi({
      listas: [{ id: 1, nome: 'Lista A', quantidadePalavras: 2 }],
      put: () => jsonResponse(200, DETALHE),
    })
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Editar Lista A' }))
    expect(await screen.findByDisplayValue('gato bola')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    const put = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(JSON.parse(String(put?.[1]?.body)).version).toBe(4)
  })
})
