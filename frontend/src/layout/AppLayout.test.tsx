import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { useAuth } from '../auth/AuthContext'
import { AppLayout } from './AppLayout'

vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

describe('AppLayout', () => {
  it('shows exactly the 3 PROFESSOR menu items (spec.md P1 "Login", AC5)', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter>
        <AppLayout />
      </MemoryRouter>,
    )

    expect(screen.getAllByRole('listitem')).toHaveLength(3)
    expect(screen.getByText('Avaliar')).toBeInTheDocument()
    expect(screen.getByText('Meus alunos')).toBeInTheDocument()
    expect(screen.getByText('Histórico')).toBeInTheDocument()
  })

  it('shows exactly the 8 COORDENADOR menu items and never "Avaliar" (spec.md P1 "Login", AC6)', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'COORDENADOR',
      professorId: null,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <MemoryRouter>
        <AppLayout />
      </MemoryRouter>,
    )

    expect(screen.getAllByRole('listitem')).toHaveLength(8)
    for (const label of [
      'Alunos',
      'Turmas',
      'Professores',
      'Anos letivos',
      'Regras de classificação',
      'Listas de palavras',
      'Usuários',
      'Avaliações',
    ]) {
      expect(screen.getByText(label)).toBeInTheDocument()
    }
    expect(screen.queryByText('Avaliar')).not.toBeInTheDocument()
  })
})
