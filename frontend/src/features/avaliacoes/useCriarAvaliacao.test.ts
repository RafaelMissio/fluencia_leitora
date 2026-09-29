import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ApiError, AvaliacaoResponse, NovaAvaliacaoRequest } from '../../api/types'
import { useCriarAvaliacao } from './useCriarAvaliacao'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: queryClient }, children)
}

const PAYLOAD: NovaAvaliacaoRequest = {
  alunoId: 42,
  tipoLeitura: 'PALAVRA',
  cicloId: 1,
  dataAvaliacao: '2026-09-29',
  tempoSegundos: 60,
  listaPalavrasId: 7,
}

describe('useCriarAvaliacao', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('success returns AvaliacaoResponse with status CRIADA', async () => {
    const response: Partial<AvaliacaoResponse> = { id: 100, alunoId: 42, status: 'CRIADA' }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(201, response))

    const { result } = renderHook(() => useCriarAvaliacao(), { wrapper: createWrapper() })
    result.current.mutate(PAYLOAD)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data?.status).toBe('CRIADA')
    expect(result.current.data?.id).toBe(100)
  })

  it('sends a POST to /avaliacoes with the given payload as JSON', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(201, { id: 1, status: 'CRIADA' }))

    const { result } = renderHook(() => useCriarAvaliacao(), { wrapper: createWrapper() })
    result.current.mutate(PAYLOAD)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/avaliacoes')
    expect(init?.method).toBe('POST')
    expect(JSON.parse(init?.body as string)).toEqual(PAYLOAD)
  })

  it('422 error propagates errors[] without dropping any item', async () => {
    const errors = [
      { field: 'dataAvaliacao', message: 'obrigatório' },
      { field: 'palavras', message: 'quantidade fora do limite' },
    ]
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(422, { code: 'VALIDACAO', errors }))

    const { result } = renderHook(() => useCriarAvaliacao(), { wrapper: createWrapper() })
    result.current.mutate(PAYLOAD)

    await waitFor(() => expect(result.current.isError).toBe(true))

    const apiError = result.current.error as ApiError
    expect(apiError.errors).toHaveLength(2)
    expect(apiError.errors).toEqual(errors)
  })
})
