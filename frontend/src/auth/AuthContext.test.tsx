import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../api/client'
import { AuthProvider, useAuth } from './AuthContext'

const SESSION_KEY = 'fluencia.session'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function TestConsumer() {
  const { isAuthenticated, perfil, professorId, token, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="isAuthenticated">{String(isAuthenticated)}</span>
      <span data-testid="perfil">{perfil ?? ''}</span>
      <span data-testid="professorId">{professorId ?? ''}</span>
      <span data-testid="token">{token ?? ''}</span>
      <button onClick={() => void login('prof@escola.com', 'senha123')}>login</button>
      <button onClick={() => logout()}>logout</button>
      <button
        onClick={() => {
          request('/alunos').catch(() => {
            // 401 tratado pelo interceptor do apiClient (setUnauthorizedHandler) - erro ignorado aqui de propósito.
          })
        }}
      >
        chamarProtegida
      </button>
    </div>
  )
}

describe('AuthContext / useAuth', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('a successful login writes token/perfil/professorId to context state and sessionStorage', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(200, { accessToken: 'abc123', expiresIn: 3600, perfil: 'PROFESSOR', professorId: 9 }),
    )
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    )

    await user.click(screen.getByText('login'))

    await waitFor(() => expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true'))
    expect(screen.getByTestId('perfil')).toHaveTextContent('PROFESSOR')
    expect(screen.getByTestId('professorId')).toHaveTextContent('9')
    expect(screen.getByTestId('token')).toHaveTextContent('abc123')

    const stored: unknown = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? '{}')
    expect(stored).toMatchObject({ token: 'abc123', perfil: 'PROFESSOR', professorId: 9 })
  })

  it('rehydrates from sessionStorage on mount so isAuthenticated starts true without any call', () => {
    sessionStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ token: 'existing-token', perfil: 'COORDENADOR', professorId: null }),
    )

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    )

    expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true')
    expect(screen.getByTestId('perfil')).toHaveTextContent('COORDENADOR')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('logout clears sessionStorage and resets context state', async () => {
    sessionStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ token: 'existing-token', perfil: 'PROFESSOR', professorId: 1 }),
    )
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    )
    expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true')

    await user.click(screen.getByText('logout'))

    expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false')
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('a 401 from any authenticated request triggers logout via the apiClient interceptor (FE-03)', async () => {
    sessionStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ token: 'existing-token', perfil: 'PROFESSOR', professorId: 1 }),
    )
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, { code: 'TOKEN_EXPIRADO' }))
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    )
    expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true')

    await user.click(screen.getByText('chamarProtegida'))

    await waitFor(() => expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false'))
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })
})
