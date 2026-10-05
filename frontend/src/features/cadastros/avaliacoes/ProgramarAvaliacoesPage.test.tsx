import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AvaliacaoProgramada } from '../../../api/types'
import { ProgramarAvaliacoesPage } from './ProgramarAvaliacoesPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const EXISTENTE: AvaliacaoProgramada = {
  id: 5, anoLetivoId: 1, nome: 'Diagnóstica', serie: 2, cicloId: 1, tipoLeitura: 'PALAVRA', tempoSegundos: 60, listaPalavrasId: null, palavras: 'a b', texto: null, maxRefazeres: 3,
}

function mockApi(estado: { programadas: AvaliacaoProgramada[]; posts: unknown[] }): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url === '/api/v1/avaliacoes-programadas' && method === 'GET') return jsonResponse(200, estado.programadas)
    if (url === '/api/v1/avaliacoes-programadas' && method === 'POST') {
      estado.posts.push(JSON.parse(String(init?.body)))
      return jsonResponse(201, EXISTENTE)
    }
    if (url === '/api/v1/avaliacoes-programadas/5' && method === 'DELETE') {
      estado.programadas = []
      return new Response(null, { status: 204 })
    }
    if (url === '/api/v1/ciclos') return jsonResponse(200, [{ id: 1, codigo: 'ENTRADA' }])
    if (url.startsWith('/api/v1/anos-letivos/ativo/configuracoes/')) {
      return jsonResponse(200, { quantidadeMinima: 20, quantidadeMaxima: 60 })
    }
    if (url.startsWith('/api/v1/listas-palavras')) return jsonResponse(200, [])
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage(modo: 'cadastrar' | 'buscar') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ProgramarAvaliacoesPage modo={modo} />
    </QueryClientProvider>,
  )
}

describe('ProgramarAvaliacoesPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists the configured evaluations grouped by série and removes one', async () => {
    mockApi({ programadas: [EXISTENTE], posts: [] })
    const user = userEvent.setup()
    renderPage('buscar')

    expect(await screen.findByRole('heading', { name: '2º ano' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Remover Diagnóstica do 2º ano' }))
    expect(await screen.findByText('Nenhuma avaliação configurada.')).toBeInTheDocument()
  })

  it('filters the evaluations by nome and série', async () => {
    mockApi({ programadas: [EXISTENTE], posts: [] })
    const user = userEvent.setup()
    renderPage('buscar')

    expect(await screen.findByRole('heading', { name: '2º ano' })).toBeInTheDocument()
    await user.type(screen.getByLabelText('Nome'), 'inexistente')
    expect(screen.getByText('Nenhuma avaliação configurada.')).toBeInTheDocument()
  })

  it('creates an evaluation for the chosen série', async () => {
    const estado = { programadas: [], posts: [] as unknown[] }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage('cadastrar')

    await user.type(screen.getByLabelText('Nome da avaliação'), 'Prova 1')
    await user.selectOptions(screen.getByLabelText('Série'), '2')
    await user.selectOptions(screen.getByLabelText('Tipo de leitura'), 'PALAVRA')
    await waitFor(() => expect(screen.getByRole('option', { name: 'ENTRADA' })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('Ciclo'), '1')
    await user.type(screen.getByLabelText('Palavras (separadas por espaço)'), 'casa bola gato')
    await user.click(screen.getByRole('button', { name: 'Configurar avaliação' }))

    await waitFor(() => expect(estado.posts).toHaveLength(1))
    expect(estado.posts[0]).toEqual({ nome: 'Prova 1', serie: 2, tipoLeitura: 'PALAVRA', cicloId: 1, tempoSegundos: 60, maxRefazeres: 3, palavras: 'casa bola gato' })
  })
})
