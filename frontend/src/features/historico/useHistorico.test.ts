import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useHistorico } from './useHistorico'
import type { HistoricoAvaliacaoItem, Page } from '../../api/types'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const ITEM: HistoricoAvaliacaoItem = {
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
}

function pageOf(content: HistoricoAvaliacaoItem[]): Page<HistoricoAvaliacaoItem> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20 }
}

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: queryClient }, children)
}

describe('useHistorico', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requests only page when no optional filter is given', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, pageOf([ITEM])))

    const { result } = renderHook(() => useHistorico(42), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.data).toEqual(pageOf([ITEM])))

    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos/42/historico-avaliacoes?page=0')
  })

  it('sends each optional filter only when defined', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, pageOf([])))

    const { result } = renderHook(
      () => useHistorico(42, { anoLetivoId: 7, tipoLeitura: 'TEXTO_CURTO', cicloId: 3 }),
      { wrapper: createWrapper() },
    )

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos/42/historico-avaliacoes?anoLetivoId=7&tipoLeitura=TEXTO_CURTO&cicloId=3&page=0')
  })

  it('propagates the page parameter correctly', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, pageOf([])))

    const { result } = renderHook(() => useHistorico(42, {}, 2), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos/42/historico-avaliacoes?page=2')
  })
})
