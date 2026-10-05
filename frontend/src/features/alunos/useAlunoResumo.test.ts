import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AlunoBuscaItem, EvolucaoCiclosResponse, HistoricoAvaliacaoItem, Page } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAlunoResumo } from './useAlunoResumo'

vi.mock('../../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function pageOf(content: HistoricoAvaliacaoItem[]): Page<HistoricoAvaliacaoItem> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20 }
}

function historicoItem(overrides: Partial<HistoricoAvaliacaoItem>): HistoricoAvaliacaoItem {
  return {
    avaliacaoId: 1,
    anoLetivo: 2026,
    serie: 2,
    turma: 'A',
    professor: 'Maria',
    ciclo: 'ENTRADA',
    tipoLeitura: 'PALAVRA',
    dataAvaliacao: '2026-03-01',
    quantidadeTotal: 20,
    quantidadeCorretas: 15,
    quantidadeIncorretas: 3,
    quantidadeNaoLidas: 2,
    percentualAcerto: 75,
    fase: 'ALFABETICA',
    nivel: 2,
    tempoUtilizadoSegundos: 55,
    temAudio: true,
    ativa: true,
    ...overrides,
  }
}

const ALUNO: AlunoBuscaItem = {
  alunoId: 42,
  nome: 'João',
  turma: 'A',
  serie: 2,
  professor: 'Maria',
  anoLetivo: 2026,
  situacao: 'EM_ANDAMENTO',
}

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

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: queryClient }, children)
}

describe('useAlunoResumo', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('PROFESSOR composition: computes evolucao client-side and never calls evolucao-ciclos', async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout: vi.fn(),
    })
    const itens = [
      historicoItem({ avaliacaoId: 2, ciclo: 'ENTRADA', tipoLeitura: 'PALAVRA', quantidadeCorretas: 18 }),
      historicoItem({ avaliacaoId: 1, ciclo: 'ENTRADA', tipoLeitura: 'PALAVRA', quantidadeCorretas: 15 }),
    ]
    mockFetchRoutes({
      '/api/v1/alunos/42/historico-avaliacoes': pageOf(itens),
      '/api/v1/alunos/42': ALUNO,
    })

    const { result } = renderHook(() => useAlunoResumo(42), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    expect(result.current.evolucao).toBe(3)
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes('evolucao-ciclos'))).toBe(false)
  })

  it('COORDENADOR composition: uses evolucao-ciclos directly', async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'COORDENADOR',
      professorId: null,
      login: vi.fn(),
      logout: vi.fn(),
    })
    const itens = [historicoItem({ avaliacaoId: 1, ciclo: 'ACOMPANHAMENTO', tipoLeitura: 'PALAVRA' })]
    const evolucaoCiclos: EvolucaoCiclosResponse = {
      alunoId: 42,
      anoLetivo: 2026,
      tipoLeitura: 'PALAVRA',
      entrada: {
        ciclo: 'ENTRADA',
        dataAvaliacao: '2026-02-01',
        quantidadeCorretas: 10,
        percentualAcerto: 50,
        fase: 'PRE_ALFABETICA',
        nivel: 1,
      },
      acompanhamento: {
        ciclo: 'ACOMPANHAMENTO',
        dataAvaliacao: '2026-06-01',
        quantidadeCorretas: 16,
        percentualAcerto: 80,
        fase: 'ALFABETICA',
        nivel: 2,
      },
      saida: null,
    }
    mockFetchRoutes({
      '/api/v1/alunos/42/historico-avaliacoes': pageOf(itens),
      '/api/v1/alunos/42/evolucao-ciclos': evolucaoCiclos,
      '/api/v1/alunos/42': ALUNO,
    })

    const { result } = renderHook(() => useAlunoResumo(42), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    expect(result.current.evolucao).toBe(6)
    const evolucaoCall = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes('evolucao-ciclos'))
    expect(evolucaoCall?.[0]).toBe('/api/v1/alunos/42/evolucao-ciclos?tipoLeitura=PALAVRA')
  })

  it('PROFESSOR evolucao without a comparable pair returns null (panel shows "—")', async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout: vi.fn(),
    })
    const itens = [historicoItem({ avaliacaoId: 1, ciclo: 'ENTRADA', tipoLeitura: 'PALAVRA' })]
    mockFetchRoutes({
      '/api/v1/alunos/42/historico-avaliacoes': pageOf(itens),
      '/api/v1/alunos/42': ALUNO,
    })

    const { result } = renderHook(() => useAlunoResumo(42), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    expect(result.current.evolucao).toBeNull()
  })

  it('cuts ultimasAvaliacoes to the 5 most recent items', async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout: vi.fn(),
    })
    const itens = Array.from({ length: 7 }, (_, i) => historicoItem({ avaliacaoId: 7 - i }))
    mockFetchRoutes({
      '/api/v1/alunos/42/historico-avaliacoes': pageOf(itens),
      '/api/v1/alunos/42': ALUNO,
    })

    const { result } = renderHook(() => useAlunoResumo(42), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    expect(result.current.ultimasAvaliacoes).toHaveLength(5)
    expect(result.current.ultimasAvaliacoes.map((item) => item.avaliacaoId)).toEqual([7, 6, 5, 4, 3])
  })

  it('exposes aluno, cicloAtual and ultimaClassificacao from the composed data', async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout: vi.fn(),
    })
    const itens = [historicoItem({ avaliacaoId: 1, ciclo: 'ENTRADA', fase: 'ALFABETICA', nivel: 3 })]
    mockFetchRoutes({
      '/api/v1/alunos/42/historico-avaliacoes': pageOf(itens),
      '/api/v1/alunos/42': ALUNO,
    })

    const { result } = renderHook(() => useAlunoResumo(42), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    expect(result.current.aluno).toEqual(ALUNO)
    expect(result.current.cicloAtual).toBe('ACOMPANHAMENTO')
    expect(result.current.ultimaClassificacao).toEqual({ fase: 'ALFABETICA', nivel: 3 })
  })
})
