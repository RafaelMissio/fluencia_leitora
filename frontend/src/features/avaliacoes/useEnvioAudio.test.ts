import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useEnvioAudio } from './useEnvioAudio'

function response(status: number): Response {
  return new Response(null, { status })
}

function blobDeAudio(): Blob {
  return new Blob(['audio'], { type: 'audio/webm' })
}

describe('useEnvioAudio', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('a successful send on the 1st attempt sets status enviado without waiting (spec.md AC2)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(response(201))

    const blob = blobDeAudio()
    const { result } = renderHook(() => useEnvioAudio(1, blob))

    await waitFor(() => expect(result.current.status).toBe('enviado'))
    expect(result.current.tentativas).toBe(1)
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('failure on the 1st attempt and success on the 2nd respects the 1s wait between attempts (fake timers)', async () => {
    vi.useFakeTimers()
    vi.mocked(fetch).mockResolvedValueOnce(response(500)).mockResolvedValueOnce(response(201))

    const blob = blobDeAudio()
    const { result } = renderHook(() => useEnvioAudio(1, blob))

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(fetch).toHaveBeenCalledTimes(1)
    expect(result.current.status).toBe('enviando')

    await act(async () => {
      await vi.advanceTimersByTimeAsync(999)
    })
    expect(fetch).toHaveBeenCalledTimes(1)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1)
    })
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(result.current.status).toBe('enviado')
    expect(result.current.tentativas).toBe(2)
  })

  it('failure in all 3 attempts sets status falhou, keeps the Blob in memory and registers beforeunload (AC3, FE-21)', async () => {
    vi.useFakeTimers()
    vi.mocked(fetch).mockResolvedValue(response(500))

    const blob = blobDeAudio()
    const { result } = renderHook(() => useEnvioAudio(1, blob))

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000)
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000)
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })

    expect(result.current.status).toBe('falhou')
    expect(fetch).toHaveBeenCalledTimes(3)

    const evento = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(evento)
    expect(evento.defaultPrevented).toBe(true)
  })

  it('reenviar() after a total failure retries with the retained Blob and can succeed (AC3)', async () => {
    vi.useFakeTimers()
    vi.mocked(fetch).mockResolvedValue(response(500))

    const blob = blobDeAudio()
    const { result } = renderHook(() => useEnvioAudio(1, blob))
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000)
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000)
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(result.current.status).toBe('falhou')

    vi.mocked(fetch).mockResolvedValue(response(201))
    await act(async () => {
      await result.current.reenviar()
    })

    expect(result.current.status).toBe('enviado')
  })

  it('beforeunload is no longer blocked once status is enviado (AC3, FE-21)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(response(201))

    const blob = blobDeAudio()
    const { result } = renderHook(() => useEnvioAudio(1, blob))
    await waitFor(() => expect(result.current.status).toBe('enviado'))

    const evento = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(evento)
    expect(evento.defaultPrevented).toBe(false)
  })
})
