import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useEvolucaoAnual } from './useEvolucaoAnual'
import type { EvolucaoAnualResponse } from '../../api/types'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const RESPOSTA: EvolucaoAnualResponse = { alunoId: 42, tipoLeitura: 'PALAVRA', anos: [] }

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: queryClient }, children)
}

describe('useEvolucaoAnual', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requests evolucao-anos with the given tipoLeitura', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, RESPOSTA))

    const { result } = renderHook(() => useEvolucaoAnual(42, 'PSEUDOPALAVRA'), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.data).toEqual(RESPOSTA))

    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos/42/evolucao-anos?tipoLeitura=PSEUDOPALAVRA')
  })
})
