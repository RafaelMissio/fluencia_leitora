import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { useAuth } from '../../auth/AuthContext'
import { AppRouter } from '../../app/router'
import { HistoricoPage } from './HistoricoPage'

vi.mock('../../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

// Cada aba tem sua própria suíte (HistoricoTab.test.tsx, EvolucaoCiclosTab.test.tsx,
// ComparacaoAnualTab.test.tsx) - mockadas aqui para isolar o teste da
// composição/gate por perfil, sem duplicar a cobertura de cada uma (Check C).
vi.mock('./HistoricoTab', () => ({
  HistoricoTab: ({ alunoId }: { alunoId: number }) => <div>Histórico do aluno {alunoId}</div>,
}))
vi.mock('./EvolucaoCiclosTab', () => ({
  EvolucaoCiclosTab: ({ alunoId }: { alunoId: number }) => <div>Evolução do aluno {alunoId}</div>,
}))
vi.mock('./ComparacaoAnualTab', () => ({
  ComparacaoAnualTab: ({ alunoId }: { alunoId: number }) => <div>Comparação do aluno {alunoId}</div>,
}))

function mockAuth(perfil: 'PROFESSOR' | 'COORDENADOR') {
  vi.mocked(useAuth).mockReturnValue({
    isAuthenticated: true,
    token: 'abc',
    perfil,
    professorId: perfil === 'PROFESSOR' ? 1 : null,
    login: vi.fn(),
    logout: vi.fn(),
  })
}

describe('HistoricoPage', () => {
  it('shows only the "Histórico" tab for PROFESSOR (context.md, "Acesso a evolução")', () => {
    mockAuth('PROFESSOR')

    render(
      <MemoryRouter initialEntries={['/alunos/42/historico']}>
        <Routes>
          <Route path="/alunos/:id/historico" element={<HistoricoPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('button', { name: 'Histórico' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Evolução' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Comparação anual' })).not.toBeInTheDocument()
    expect(screen.getByText('Histórico do aluno 42')).toBeInTheDocument()
    expect(screen.queryByText(/Evolução do aluno/)).not.toBeInTheDocument()
    expect(screen.queryByText(/Comparação do aluno/)).not.toBeInTheDocument()
  })

  it('shows all 3 tabs for COORDENADOR and switches content when clicked', async () => {
    mockAuth('COORDENADOR')

    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/alunos/42/historico']}>
        <Routes>
          <Route path="/alunos/:id/historico" element={<HistoricoPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('button', { name: 'Histórico' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Evolução' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Comparação anual' })).toBeInTheDocument()
    expect(screen.getByText('Histórico do aluno 42')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Evolução' }))
    expect(screen.getByText('Evolução do aluno 42')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Comparação anual' }))
    expect(screen.getByText('Comparação do aluno 42')).toBeInTheDocument()
  })

  it('renders the page at the protected /alunos/:id/historico route', () => {
    mockAuth('PROFESSOR')

    render(
      <MemoryRouter initialEntries={['/alunos/42/historico']}>
        <AppRouter />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Histórico e evolução' })).toBeInTheDocument()
    expect(screen.getByText('Histórico do aluno 42')).toBeInTheDocument()
  })
})
