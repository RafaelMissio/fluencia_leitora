import type { ApiError } from './types'

const API_BASE = '/api/v1'

/**
 * `ApiError` (T2) mais o `Retry-After` (em segundos), quando a resposta o traz
 * (429 de login - spec.md P1 "Login", AC3). Extensão local a este arquivo:
 * não altera o tipo `ApiError` compartilhado em `types.ts`.
 */
export interface ApiErrorComRetry extends ApiError {
  retryAfterSeconds?: number
}

let currentToken: string | null = null
let unauthorizedHandler: (() => void) | null = null

/** Registrado pelo `AuthContext` (T4): token atual injetado no header Authorization. */
export function setAuthToken(token: string | null): void {
  currentToken = token
}

/** Registrado pelo `AuthContext` (T4) como o `logout()` chamado em qualquer 401 (FE-03). */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

interface ProblemDetailBody {
  code?: string
  detail?: string
  errors?: { field: string; message: string }[]
}

async function parseProblemDetail(response: Response): Promise<ProblemDetailBody> {
  try {
    const body: unknown = await response.json()
    if (typeof body !== 'object' || body === null) return {}
    const record = body as Record<string, unknown>
    return {
      code: typeof record.code === 'string' ? record.code : undefined,
      detail: typeof record.detail === 'string' ? record.detail : undefined,
      errors: Array.isArray(record.errors)
        ? (record.errors as { field: string; message: string }[])
        : undefined,
    }
  } catch {
    return {}
  }
}

function parseRetryAfterSeconds(response: Response): number | undefined {
  const header = response.headers.get('Retry-After')
  if (!header) return undefined
  const seconds = Number(header)
  return Number.isFinite(seconds) ? seconds : undefined
}

async function toApiError(response: Response): Promise<ApiErrorComRetry> {
  const body = await parseProblemDetail(response)
  return {
    status: response.status,
    code: body.code,
    detail: body.detail,
    errors: body.errors,
    retryAfterSeconds: response.status === 429 ? parseRetryAfterSeconds(response) : undefined,
  }
}

function networkError(): ApiErrorComRetry {
  return { status: 0, detail: 'Falha de rede' }
}

function buildHeaders(init: RequestInit): Headers {
  const headers = new Headers(init.headers)
  if (currentToken) {
    headers.set('Authorization', `Bearer ${currentToken}`)
  }
  if (init.body !== undefined && !headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  return headers
}

/**
 * Único ponto de `fetch` da app: injeta Authorization, monta a URL relativa
 * `/api/v1${path}` (proxy do Vite), faz parse do erro RFC 7807 em respostas
 * não-2xx e dispara o interceptor de 401 antes de rejeitar.
 */
export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = buildHeaders(init)

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  } catch {
    throw networkError()
  }

  if (!response.ok) {
    const apiError = await toApiError(response)
    if (response.status === 401 && unauthorizedHandler) {
      unauthorizedHandler()
    }
    throw apiError
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text) {
    return undefined as T
  }
  return JSON.parse(text) as T
}

/** `POST /api/v1/avaliacoes/{id}/audio` via `multipart/form-data` (não passa por `request` - precisa de `FormData`, não JSON). */
export async function uploadAudio(avaliacaoId: number, blob: Blob, mimeType: string): Promise<void> {
  // O MediaRecorder do Chrome grava `audio/webm;codecs=opus`, mas o backend só aceita o mimeType puro
  // (AUDIO_FORMATO_INVALIDO com parâmetros): remove os parâmetros do Content-Type da parte.
  const baseType = mimeType.split(';')[0]
  const extension = baseType.split('/')[1] ?? 'webm'
  const formData = new FormData()
  formData.append('audio', new Blob([blob], { type: baseType }), `avaliacao-${avaliacaoId}.${extension}`)

  const headers = new Headers()
  if (currentToken) {
    headers.set('Authorization', `Bearer ${currentToken}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}/avaliacoes/${avaliacaoId}/audio`, {
      method: 'POST',
      headers,
      body: formData,
    })
  } catch {
    throw networkError()
  }

  if (!response.ok) {
    const apiError = await toApiError(response)
    if (response.status === 401 && unauthorizedHandler) {
      unauthorizedHandler()
    }
    throw apiError
  }
}

/** `GET /api/v1/avaliacoes/{id}/audio` com o token (um `<audio src>` nativo não envia `Authorization`); devolve os bytes. */
export async function baixarAudio(avaliacaoId: number): Promise<Blob> {
  const headers = new Headers()
  if (currentToken) {
    headers.set('Authorization', `Bearer ${currentToken}`)
  }
  let response: Response
  try {
    response = await fetch(`${API_BASE}/avaliacoes/${avaliacaoId}/audio`, { headers })
  } catch {
    throw networkError()
  }
  if (!response.ok) {
    throw new Error('Não foi possível carregar o áudio')
  }
  return response.blob()
}
