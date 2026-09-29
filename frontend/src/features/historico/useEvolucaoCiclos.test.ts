import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useEvolucaoCiclos } from './useEvolucaoCiclos'
import type { EvolucaoCiclosResponse } from '../../api/types'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const RESPOSTA: EvolucaoCiclosResponse = {
  alunoId: 42,
  anoLetivo: 2026,
  tipoLeitura: 'PALAVRA',
  entrada: null,
  acompanhamento: null,
  saida: null,
}

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: queryClient }, children)
}

describe('useEvolucaoCiclos', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requests only tipoLeitura when anoLetivoId is undefined', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, RESPOSTA))

    const { result } = renderHook(() => useEvolucaoCiclos(42, undefined, 'PALAVRA'), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.data).toEqual(RESPOSTA))

    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos/42/evolucao-ciclos?tipoLeitura=PALAVRA')
  })

  it('includes anoLetivoId in the URL when defined', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, RESPOSTA))

    const { result } = renderHook(() => useEvolucaoCiclos(42, 7, 'TEXTO_CURTO'), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos/42/evolucao-ciclos?anoLetivoId=7&tipoLeitura=TEXTO_CURTO')
  })
})
