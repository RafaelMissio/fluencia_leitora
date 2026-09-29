import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AlunoBuscaItem, Page } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { AppRouter } from '../../app/router'
import { AlunoBuscaPage } from './AlunoBuscaPage'
import { useAlunoBusca } from './useAlunoBusca'

vi.mock('./useAlunoBusca', () => ({
  useAlunoBusca: vi.fn(),
}))

vi.mock('../../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

function pageOf(content: AlunoBuscaItem[]): Page<AlunoBuscaItem> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20 }
}

const JOAO: AlunoBuscaItem = {
  alunoId: 1,
  nome: 'João',
  turma: 'A',
  serie: 2,
  professor: 'Maria',
  anoLetivo: 2026,
  situacao: 'EM_ANDAMENTO',
}

describe('AlunoBuscaPage', () => {
  it('lists the students returned by the search hook with name, turma and série', async () => {
    vi.mocked(useAlunoBusca).mockReturnValue({
      data: pageOf([JOAO]),
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <AlunoBuscaPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Nome do aluno'), 'jo')

    expect(screen.getByText('João - A - 2ª série')).toBeInTheDocument()
  })

  it('selecting a result item marks that student as selected', async () => {
    vi.mocked(useAlunoBusca).mockReturnValue({
      data: pageOf([JOAO]),
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <AlunoBuscaPage />
      </MemoryRouter>,
    )

    const item = screen.getByText('João - A - 2ª série')
    expect(item).toHaveAttribute('aria-pressed', 'false')

    await user.click(item)

    expect(item).toHaveAttribute('aria-pressed', 'true')
  })

  it('shows "Nenhum aluno encontrado" when the search returns no results', async () => {
    vi.mocked(useAlunoBusca).mockReturnValue({
      data: pageOf([]),
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <AlunoBuscaPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Nome do aluno'), 'xy')

    expect(screen.getByText('Nenhum aluno encontrado')).toBeInTheDocument()
  })

  it('renders the page at the protected /alunos route', () => {
    vi.mocked(useAlunoBusca).mockReturnValue({
      data: pageOf([]),
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)
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
        <AppRouter />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Buscar aluno' })).toBeInTheDocument()
  })
})
