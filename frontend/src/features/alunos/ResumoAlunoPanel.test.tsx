import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AlunoBuscaItem, HistoricoAvaliacaoItem } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ResumoAlunoPanel } from './ResumoAlunoPanel'
import { useAlunoResumo } from './useAlunoResumo'

vi.mock('./useAlunoResumo', () => ({
  useAlunoResumo: vi.fn(),
}))

vi.mock('../../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}))

function comPerfil(perfil: 'PROFESSOR' | 'COORDENADOR'): void {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  vi.mocked(useAuth).mockReturnValue({ perfil } as any)
}

beforeEach(() => comPerfil('PROFESSOR'))

const ALUNO: AlunoBuscaItem = {
  alunoId: 42,
  nome: 'João',
  turma: 'A',
  serie: 2,
  professor: 'Maria',
  anoLetivo: 2026,
  situacao: 'EM_ANDAMENTO',
}

const AVALIACAO: HistoricoAvaliacaoItem = {
  avaliacaoId: 1,
  anoLetivo: 2026,
  serie: 2,
  turma: 'A',
  professor: 'Maria',
  ciclo: 'ENTRADA',
  tipoLeitura: 'PALAVRA',
  dataAvaliacao: '2026-03-01',
  quantidadeTotal: 20,
  quantidadeCorretas: 15,
  quantidadeIncorretas: 3,
  quantidadeNaoLidas: 2,
  percentualAcerto: 75,
  fase: 'ALFABETICA',
  nivel: 2,
  tempoUtilizadoSegundos: 55,
  temAudio: true,
}

describe('ResumoAlunoPanel', () => {
  it('renders every AC2 field, including the current cycle', () => {
    vi.mocked(useAlunoResumo).mockReturnValue({
      aluno: ALUNO,
      cicloAtual: 'ACOMPANHAMENTO',
      ultimasAvaliacoes: [AVALIACAO],
      ultimaClassificacao: { fase: 'ALFABETICA', nivel: 2 },
      evolucao: 3,
      isLoading: false,
      error: null,
    })

    render(
      <MemoryRouter>
        <ResumoAlunoPanel alunoId={42} />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'João' })).toBeInTheDocument()
    expect(screen.getByText('A')).toBeInTheDocument()
    expect(screen.getByText('Maria')).toBeInTheDocument()
    expect(screen.getByText('2026')).toBeInTheDocument()
    expect(screen.getByText('2ª série')).toBeInTheDocument()
    expect(screen.getByText('ACOMPANHAMENTO')).toBeInTheDocument()
    expect(screen.getByText('ALFABETICA (nível 2)')).toBeInTheDocument()
    expect(screen.getByText('+3')).toBeInTheDocument()
    expect(screen.getByText(/2026-03-01.*ENTRADA.*PALAVRA.*15\/20 corretas/)).toBeInTheDocument()
  })

  it('shows "—" when evolucao has no comparison pair', () => {
    vi.mocked(useAlunoResumo).mockReturnValue({
      aluno: ALUNO,
      cicloAtual: 'ENTRADA',
      ultimasAvaliacoes: [],
      ultimaClassificacao: { fase: 'ALFABETICA', nivel: 1 },
      evolucao: null,
      isLoading: false,
      error: null,
    })

    render(
      <MemoryRouter>
        <ResumoAlunoPanel alunoId={42} />
      </MemoryRouter>,
    )

    const evolucaoDt = screen.getByText('Evolução no ano')
    expect(evolucaoDt.nextElementSibling).toHaveTextContent('—')
  })

  it('navigates to /avaliacoes/nova?alunoId= when "Configurar avaliação" is clicked', async () => {
    vi.mocked(useAlunoResumo).mockReturnValue({
      aluno: ALUNO,
      cicloAtual: 'ENTRADA',
      ultimasAvaliacoes: [],
      ultimaClassificacao: null,
      evolucao: null,
      isLoading: false,
      error: null,
    })

    function DestinoProbe() {
      return <div>Tela de configurar avaliação</div>
    }

    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/alunos']}>
        <Routes>
          <Route path="/alunos" element={<ResumoAlunoPanel alunoId={42} />} />
          <Route path="/avaliacoes/nova" element={<DestinoProbe />} />
        </Routes>
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Configurar avaliação' }))

    expect(screen.getByText('Tela de configurar avaliação')).toBeInTheDocument()
  })

  it('navigates to /alunos/:id/historico when "Ver histórico" is clicked', async () => {
    vi.mocked(useAlunoResumo).mockReturnValue({
      aluno: ALUNO,
      cicloAtual: 'ENTRADA',
      ultimasAvaliacoes: [],
      ultimaClassificacao: null,
      evolucao: null,
      isLoading: false,
      error: null,
    })

    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/alunos']}>
        <Routes>
          <Route path="/alunos" element={<ResumoAlunoPanel alunoId={42} />} />
          <Route path="/alunos/:id/historico" element={<div>Tela de histórico</div>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Ver histórico' }))

    expect(screen.getByText('Tela de histórico')).toBeInTheDocument()
  })

  it('hides "Configurar avaliação" for the COORDENADOR (only the professor applies evaluations)', () => {
    comPerfil('COORDENADOR')
    vi.mocked(useAlunoResumo).mockReturnValue({
      aluno: ALUNO,
      cicloAtual: 'ENTRADA',
      ultimasAvaliacoes: [],
      ultimaClassificacao: null,
      evolucao: null,
      isLoading: false,
      error: null,
    })

    render(
      <MemoryRouter>
        <ResumoAlunoPanel alunoId={42} />
      </MemoryRouter>,
    )

    expect(screen.queryByRole('button', { name: 'Configurar avaliação' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver histórico' })).toBeInTheDocument()
  })
})
