import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

async function advance(ms: number): Promise<void> {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

describe('App (composition root)', () => {
  beforeEach(() => {
    sessionStorage.clear()
    window.history.pushState({}, '', '/alunos')
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the LoginPage when there is no session', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('shows the protected layout when a valid session exists in sessionStorage', () => {
    sessionStorage.setItem(
      'fluencia.session',
      JSON.stringify({ token: 'tok', perfil: 'PROFESSOR', professorId: 1 }),
    )

    render(<App />)

    expect(screen.queryByRole('heading', { name: 'Entrar' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Meus alunos' })).toBeInTheDocument()
  })

  it('a 401 from the real apiClient logs out the real session and shows the LoginPage', async () => {
    vi.useFakeTimers()
    sessionStorage.setItem(
      'fluencia.session',
      JSON.stringify({ token: 'tok', perfil: 'PROFESSOR', professorId: 1 }),
    )
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, { code: 'NAO_AUTENTICADO' }))

    render(<App />)
    expect(screen.getByRole('link', { name: 'Meus alunos' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Nome do aluno'), { target: { value: 'jo' } })
    await advance(300)
    vi.useRealTimers()

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument())
  })
})
