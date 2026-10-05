import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
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

    expect(screen.getAllByRole('listitem')).toHaveLength(4)
    expect(screen.getByText('Avaliar')).toBeInTheDocument()
    expect(screen.getByText('Alunos')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Avaliação Aluno' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Histórico' })).toHaveAttribute('href', '/historico')
    expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument()
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

    expect(screen.getAllByRole('listitem')).toHaveLength(21)
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
    expect(screen.getByRole('link', { name: 'Cadastrar aluno' })).toHaveAttribute('href', '/cadastros/alunos')
    expect(screen.getByRole('link', { name: 'Alterar aluno' })).toHaveAttribute('href', '/cadastros/alunos/alterar')
    expect(screen.getByRole('link', { name: 'Avaliação Aluno' })).toHaveAttribute('href', '/alunos')
    expect(screen.getByRole('link', { name: 'Histórico' })).toHaveAttribute('href', '/historico')
    expect(screen.getByRole('link', { name: 'Cadastrar turma' })).toHaveAttribute('href', '/cadastros/turmas/cadastrar')
    expect(screen.getByRole('link', { name: 'Listar turmas' })).toHaveAttribute('href', '/cadastros/turmas/listar')
    expect(screen.getByRole('link', { name: 'Alunos da turma' })).toHaveAttribute('href', '/cadastros/turmas/alunos')
    expect(screen.getByRole('link', { name: 'Cadastrar ano letivo' })).toHaveAttribute('href', '/cadastros/anos-letivos/cadastrar')
    expect(screen.getByRole('link', { name: 'Listar anos letivos' })).toHaveAttribute('href', '/cadastros/anos-letivos/listar')
    expect(screen.getByRole('link', { name: 'Cadastrar lista' })).toHaveAttribute('href', '/cadastros/listas-palavras/cadastrar')
    expect(screen.getByRole('link', { name: 'Buscar listas' })).toHaveAttribute('href', '/cadastros/listas-palavras/buscar')
    expect(screen.getByRole('link', { name: 'Cadastrar avaliação' })).toHaveAttribute('href', '/cadastros/avaliacoes/cadastrar')
    expect(screen.getByRole('link', { name: 'Buscar avaliações' })).toHaveAttribute('href', '/cadastros/avaliacoes/buscar')
    expect(screen.queryByText('Avaliar')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument()
  })

  it('clicking "Sair" calls logout() and navigates to /login (T38)', async () => {
    const logout = vi.fn()
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      token: 'abc',
      perfil: 'PROFESSOR',
      professorId: 1,
      login: vi.fn(),
      logout,
    })

    function LoginProbe() {
      return <div>Login Page</div>
    }

    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/alunos']}>
        <Routes>
          <Route path="/login" element={<LoginProbe />} />
          <Route path="/alunos" element={<AppLayout />} />
        </Routes>
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Sair' }))

    expect(logout).toHaveBeenCalledTimes(1)
    expect(screen.getByText('Login Page')).toBeInTheDocument()
  })
})
