import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AlunoBuscaItem, AvaliacaoResponse, HistoricoAvaliacaoItem, Page } from '../../api/types'
import { ConfigurarAvaliacaoPage } from './ConfigurarAvaliacaoPage'
import { useCriarAvaliacao } from './useCriarAvaliacao'
import { useListasPalavras } from './useListasPalavras'

vi.mock('./useListasPalavras', () => ({
  useListasPalavras: vi.fn(),
}))

vi.mock('./useCriarAvaliacao', () => ({
  useCriarAvaliacao: vi.fn(),
}))

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function pageOf(content: HistoricoAvaliacaoItem[]): Page<HistoricoAvaliacaoItem> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20 }
}

const ALUNO: AlunoBuscaItem = {
  alunoId: 42,
  nome: 'João',
  turma: 'A',
  serie: 1,
  professor: 'Maria',
  anoLetivo: 2026,
  situacao: 'EM_ANDAMENTO',
}

const CICLOS = [
  { id: 1, codigo: 'ENTRADA', descricao: 'Entrada' },
  { id: 2, codigo: 'ACOMPANHAMENTO', descricao: 'Acompanhamento' },
  { id: 3, codigo: 'SAIDA', descricao: 'Saída' },
]

const LIMITES = { quantidadeMinima: 15, quantidadeMaxima: 20 }

function mockFetchRoutes(routes: Record<string, unknown>): void {
  vi.mocked(fetch).mockImplementation(async (input) => {
    const url = String(input)
    for (const [path, body] of Object.entries(routes)) {
      if (url.startsWith(path)) {
        return jsonResponse(200, body)
      }
    }
    throw new Error(`unexpected fetch call: ${url}`)
  })
}

function renderPage(alunoId = 42) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/avaliacoes/nova?alunoId=${alunoId}`]}>
        <Routes>
          <Route path="/avaliacoes/nova" element={<ConfigurarAvaliacaoPage />} />
          <Route path="/avaliacoes/:id/executar" element={<div>Tela de execução</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ConfigurarAvaliacaoPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    vi.mocked(useListasPalavras).mockReturnValue({
      data: [],
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)
    vi.mocked(useCriarAvaliacao).mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
      error: null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)
    mockFetchRoutes({
      '/api/v1/alunos/42/historico-avaliacoes': pageOf([]),
      '/api/v1/alunos/42': ALUNO,
      '/api/v1/ciclos': CICLOS,
      '/api/v1/anos-letivos/ativo/configuracoes/1': LIMITES,
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('opens with ciclo/data/tempo prefilled per AC1 (ciclo atual = ENTRADA, hoje, 60s)', async () => {
    renderPage()

    await waitFor(() =>
      expect((screen.getByLabelText('Ciclo') as HTMLSelectElement).value).toBe('1'),
    )

    const hoje = new Date()
    const isoHoje = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`
    expect((screen.getByLabelText('Data') as HTMLInputElement).value).toBe(isoHoje)
    expect((screen.getByLabelText('Tempo (segundos)') as HTMLInputElement).value).toBe('60')
  })

  it('changing tipo de leitura updates the listas de palavras and labels "Digitar texto" only for TEXTO_CURTO', async () => {
    vi.mocked(useListasPalavras).mockReturnValue({
      data: [{ id: 9, nome: 'Lista A', quantidadePalavras: 15 }],
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(screen.getByLabelText('Tipo de leitura'), 'PALAVRA')
    expect(screen.getByText('Digitar palavras')).toBeInTheDocument()
    expect(screen.getByText('Lista A (15 palavras)')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Tipo de leitura'), 'TEXTO_CURTO')
    expect(screen.getByText('Digitar texto')).toBeInTheDocument()
    expect(screen.queryByText('Digitar palavras')).not.toBeInTheDocument()
  })

  it('disables "Criar avaliação" with 14 words (min 15) and enables it with 15', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(screen.getByLabelText('Tipo de leitura'), 'PALAVRA')
    await waitFor(() => expect(screen.getByText(/mín\. 15, máx\. 20/)).toBeInTheDocument())

    const palavras14 = Array.from({ length: 14 }, (_, i) => `p${i}`).join(' ')
    await user.type(screen.getByLabelText('Palavras (separadas por espaço)'), palavras14)
    expect(screen.getByText('Criar avaliação')).toBeDisabled()

    await user.type(screen.getByLabelText('Palavras (separadas por espaço)'), ' p14')
    expect(screen.getByText('Criar avaliação')).toBeEnabled()
  })

  it('shows the 422 field message next to the corresponding field', async () => {
    vi.mocked(useCriarAvaliacao).mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
      error: { status: 422, code: 'VALIDACAO', errors: [{ field: 'dataAvaliacao', message: 'data inválida' }] },
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    renderPage()

    await waitFor(() => expect(screen.getByText('data inválida')).toBeInTheDocument())
  })

  it('navigates to /avaliacoes/{id}/executar when creation succeeds', async () => {
    const mutateAsync = vi.fn().mockResolvedValue({ id: 555, status: 'CRIADA' } as AvaliacaoResponse)
    vi.mocked(useCriarAvaliacao).mockReturnValue({
      mutateAsync,
      isPending: false,
      error: null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(screen.getByLabelText('Tipo de leitura'), 'PALAVRA')
    await waitFor(() => expect(screen.getByText(/mín\. 15, máx\. 20/)).toBeInTheDocument())
    const palavras15 = Array.from({ length: 15 }, (_, i) => `p${i}`).join(' ')
    await user.type(screen.getByLabelText('Palavras (separadas por espaço)'), palavras15)

    await user.click(screen.getByText('Criar avaliação'))

    await waitFor(() => expect(screen.getByText('Tela de execução')).toBeInTheDocument())
    expect(mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({ alunoId: 42, tipoLeitura: 'PALAVRA', dataAvaliacao: expect.any(String) }),
    )
  })
})
