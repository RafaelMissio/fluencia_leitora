import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useListasPalavras, type ListaPalavrasResumo } from './useListasPalavras'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: queryClient }, children)
}

describe('useListasPalavras', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('calls GET /listas-palavras with serie and tipoLeitura when both are defined', async () => {
    const listas: ListaPalavrasResumo[] = [{ id: 1, nome: 'Lista A', quantidadePalavras: 20 }]
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, listas))

    const { result } = renderHook(() => useListasPalavras(2, 'PALAVRA'), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.data).toBeDefined())

    expect(result.current.data).toEqual(listas)
    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/listas-palavras?serie=2&tipoLeitura=PALAVRA')
  })

  it('does not call the API when neither serie nor tipoLeitura is defined', () => {
    renderHook(() => useListasPalavras(undefined, undefined), { wrapper: createWrapper() })

    expect(fetch).not.toHaveBeenCalled()
  })

  it('does not call the API when only serie is defined (tipoLeitura missing)', () => {
    renderHook(() => useListasPalavras(2, undefined), { wrapper: createWrapper() })

    expect(fetch).not.toHaveBeenCalled()
  })
})
