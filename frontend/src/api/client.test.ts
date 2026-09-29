import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { request, setAuthToken, setUnauthorizedHandler, uploadAudio } from './client'

function jsonResponse(status: number, body: unknown, headers?: Record<string, string>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  })
}

describe('apiClient request', () => {
  beforeEach(() => {
    setAuthToken(null)
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('does not inject the Authorization header when no token is registered', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    await request('/auth/login', { method: 'POST' })

    const [, init] = vi.mocked(fetch).mock.calls[0]
    const headers = new Headers(init?.headers)
    expect(headers.has('Authorization')).toBe(false)
  })

  it('injects the Authorization header with the registered token', async () => {
    setAuthToken('token-123')
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    await request('/alunos')

    const [, init] = vi.mocked(fetch).mock.calls[0]
    const headers = new Headers(init?.headers)
    expect(headers.get('Authorization')).toBe('Bearer token-123')
  })

  it('builds the request URL as /api/v1<path> so the Vite proxy applies', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    await request('/alunos')

    const [url] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/alunos')
  })

  it('returns the typed object for a 2xx response with a JSON body', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { id: 42, nome: 'João' }))

    const result = await request<{ id: number; nome: string }>('/alunos/42')

    expect(result).toEqual({ id: 42, nome: 'João' })
  })

  it('returns undefined for a 204 response with no body', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))

    const result = await request('/avaliacoes/1/pausar', { method: 'POST' })

    expect(result).toBeUndefined()
  })

  it('throws an ApiError with code and errors[] for a 422 response, without losing any item', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(422, {
        code: 'VALIDACAO_INVALIDA',
        errors: [
          { field: 'tempoSegundos', message: 'deve ser positivo' },
          { field: 'cicloId', message: 'não pode ser nulo' },
        ],
      }),
    )

    await expect(request('/avaliacoes', { method: 'POST' })).rejects.toMatchObject({
      status: 422,
      code: 'VALIDACAO_INVALIDA',
      errors: [
        { field: 'tempoSegundos', message: 'deve ser positivo' },
        { field: 'cicloId', message: 'não pode ser nulo' },
      ],
    })
  })

  it('throws an ApiError with code/detail for a 409 response', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(409, { code: 'TRANSICAO_INVALIDA', detail: 'Avaliação não está EM_ANDAMENTO' }),
    )

    await expect(request('/avaliacoes/1/pausar', { method: 'POST' })).rejects.toMatchObject({
      status: 409,
      code: 'TRANSICAO_INVALIDA',
      detail: 'Avaliação não está EM_ANDAMENTO',
    })
  })

  it('throws an ApiError carrying retryAfterSeconds parsed from the Retry-After header on 429', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(
        429,
        { code: 'CONTA_BLOQUEADA', detail: 'Conta bloqueada temporariamente por excesso de tentativas' },
        { 'Retry-After': '120' },
      ),
    )

    await expect(request('/auth/login', { method: 'POST' })).rejects.toMatchObject({
      status: 429,
      code: 'CONTA_BLOQUEADA',
      retryAfterSeconds: 120,
    })
  })

  it('calls the registered unauthorized handler before rejecting on a 401 response', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, { code: 'CREDENCIAIS_INVALIDAS' }))

    await expect(request('/alunos')).rejects.toMatchObject({ status: 401 })
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('does not call the unauthorized handler for a non-401 error', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(409, { code: 'CONFLITO' }))

    await expect(request('/turmas', { method: 'POST' })).rejects.toMatchObject({ status: 409 })
    expect(handler).not.toHaveBeenCalled()
  })

  it('converts a network failure (fetch rejects) into an ApiError with status: 0', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('Failed to fetch'))

    await expect(request('/alunos')).rejects.toMatchObject({ status: 0 })
  })
})

describe('apiClient uploadAudio', () => {
  beforeEach(() => {
    setAuthToken(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends multipart/form-data with an "audio" field to the audio endpoint and resolves on 201', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 201 }))
    const blob = new Blob(['fake-audio-bytes'], { type: 'audio/webm' })

    await expect(uploadAudio(7, blob, 'audio/webm;codecs=opus')).resolves.toBeUndefined()

    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/avaliacoes/7/audio')
    expect(init?.method).toBe('POST')
    const formData = init?.body as FormData
    expect(formData).toBeInstanceOf(FormData)
    const audioEntry = formData.get('audio')
    expect(audioEntry).toBeInstanceOf(Blob)
  })

  it('throws an ApiError when the audio upload fails', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(500, { code: 'ERRO_INTERNO' }))
    const blob = new Blob(['x'], { type: 'audio/webm' })

    await expect(uploadAudio(7, blob, 'audio/webm')).rejects.toMatchObject({ status: 500 })
  })
})
