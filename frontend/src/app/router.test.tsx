import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { useAuth } from '../auth/AuthContext'
import { ProtectedRoute, RoleGate } from './router'

vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

function LoginProbe() {
  const location = useLocation()
  const from = (location.state as { from?: { pathname: string } } | null)?.from
  return <div>Login Page{from ? ` (voltar para ${from.pathname})` : ''}</div>
}

describe('ProtectedRoute', () => {
  it('redirects an unauthenticated user to /login, preserving the origin route (FE-03 AC4)', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      token: null,
      perfil: null,
      professorId: null,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter initialEntries={['/alunos']}>
        <Routes>
          <Route path="/login" element={<LoginProbe />} />
          <Route
            path="/alunos"
            element={
              <ProtectedRoute>
                <div>Conteúdo protegido</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Login Page (voltar para /alunos)')).toBeInTheDocument()
    expect(screen.queryByText('Conteúdo protegido')).not.toBeInTheDocument()
  })

  it('renders the protected content when the user is authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter initialEntries={['/alunos']}>
        <Routes>
          <Route path="/login" element={<LoginProbe />} />
          <Route
            path="/alunos"
            element={
              <ProtectedRoute>
                <div>Conteúdo protegido</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Conteúdo protegido')).toBeInTheDocument()
    expect(screen.queryByText(/Login Page/)).not.toBeInTheDocument()
  })
})

describe('RoleGate', () => {
  it('hides content when the profile is not in the allowed list (blocks PROFESSOR from COORDENADOR content)', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <RoleGate allow={['COORDENADOR']}>
        <div>Conteúdo do coordenador</div>
      </RoleGate>,
    )

    expect(screen.queryByText('Conteúdo do coordenador')).not.toBeInTheDocument()
  })

  it('renders content when the profile is in the allowed list', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'COORDENADOR',
      professorId: null,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <RoleGate allow={['COORDENADOR']}>
        <div>Conteúdo do coordenador</div>
      </RoleGate>,
    )

    expect(screen.getByText('Conteúdo do coordenador')).toBeInTheDocument()
  })
})
