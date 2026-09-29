import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAlunoBusca } from './useAlunoBusca'
import type { AlunoBuscaItem, Page } from '../../api/types'

const DEBOUNCE_MS = 300

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function pageOf(content: AlunoBuscaItem[]): Page<AlunoBuscaItem> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20 }
}

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: queryClient }, children)
}

async function advance(ms: number): Promise<void> {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

describe('useAlunoBusca', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('does not call the API when fewer than 2 characters are typed (spec.md P1 AC1)', async () => {
    renderHook(() => useAlunoBusca('j'), { wrapper: createWrapper() })

    await advance(1000)

    expect(fetch).not.toHaveBeenCalled()
  })

  it('only calls the API 300ms after the last keystroke (debounce)', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, pageOf([])))

    const wrapper = createWrapper()
    const { rerender } = renderHook(({ nome }: { nome: string }) => useAlunoBusca(nome), {
      wrapper,
      initialProps: { nome: 'j' },
    })

    rerender({ nome: 'jo' })
    await advance(200)
    expect(fetch).not.toHaveBeenCalled()

    rerender({ nome: 'joa' })
    await advance(299)
    expect(fetch).not.toHaveBeenCalled()

    await advance(1)
    expect(fetch).toHaveBeenCalledTimes(1)
    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos?nome=joa&page=0')
  })

  it('propagates the result typed as Page<AlunoBuscaItem>', async () => {
    const item: AlunoBuscaItem = {
      alunoId: 1,
      nome: 'João',
      turma: 'A',
      serie: 2,
      professor: 'Maria',
      anoLetivo: 2026,
      situacao: 'EM_ANDAMENTO',
    }
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, pageOf([item])))

    const { result } = renderHook(() => useAlunoBusca('jo'), { wrapper: createWrapper() })

    await advance(DEBOUNCE_MS)

    expect(result.current.data).toEqual(pageOf([item]))
  })
})
