import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AlunoBuscaItem, HistoricoAvaliacaoItem } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ResumoAlunoPanel } from './ResumoAlunoPanel'
import { useAlunoResumo } from './useAlunoResumo'
import { useAplicarAvaliacaoProgramada } from '../avaliacoes/useAplicarAvaliacaoProgramada'
import { useAvaliacoesPendentes } from '../avaliacoes/useAvaliacoesPendentes'

vi.mock('../avaliacoes/useAplicarAvaliacaoProgramada', () => ({
  useAplicarAvaliacaoProgramada: vi.fn(),
}))

vi.mock('../avaliacoes/useRefazerAvaliacao', () => ({
  useRefazerAvaliacao: () => ({ mutateAsync: vi.fn(), isPending: false, error: null }),
}))
vi.mock('../avaliacoes/useAvaliacoesPendentes', () => ({
  useAvaliacoesPendentes: vi.fn(),
}))

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

beforeEach(() => {
  comPerfil('PROFESSOR')
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  vi.mocked(useAvaliacoesPendentes).mockReturnValue({ data: [] } as any)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  vi.mocked(useAplicarAvaliacaoProgramada).mockReturnValue({ mutateAsync: vi.fn(), isPending: false, error: null } as any)
})

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
  ativa: true,
}

describe('ResumoAlunoPanel', () => {
  it('renders only the student evaluations and the configure button', () => {
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
    expect(screen.queryByText('Turma')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ver histórico' })).not.toBeInTheDocument()
    expect(screen.getByText(/2026-03-01.*ENTRADA.*PALAVRA.*15\/20 corretas/)).toBeInTheDocument()
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

  it('shows "Configurar avaliação" for the COORDENADOR too', () => {
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

    expect(screen.getByRole('button', { name: 'Configurar avaliação' })).toBeInTheDocument()
  })

  it('lists pending evaluations; started ones open directly, series ones are created first', async () => {
    vi.mocked(useAlunoResumo).mockReturnValue({
      aluno: ALUNO,
      cicloAtual: 'ENTRADA',
      ultimasAvaliacoes: [],
      ultimaClassificacao: null,
      evolucao: null,
      isLoading: false,
      error: null,
    })
    vi.mocked(useAvaliacoesPendentes).mockReturnValue({
      data: [
        { programadaId: null, avaliacaoId: 8, nome: null, status: 'PAUSADA', tipoLeitura: 'TEXTO_CURTO', cicloId: 1, tempoSegundos: 90 },
        { programadaId: 3, avaliacaoId: null, nome: 'Diagnóstica', status: null, tipoLeitura: 'PALAVRA', cicloId: 1, tempoSegundos: 60 },
      ],
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)
    const mutateAsync = vi.fn().mockResolvedValue({ id: 99 })
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.mocked(useAplicarAvaliacaoProgramada).mockReturnValue({ mutateAsync, isPending: false, error: null } as any)
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/alunos']}>
        <Routes>
          <Route path="/alunos" element={<ResumoAlunoPanel alunoId={42} />} />
          <Route path="/avaliacoes/:id/executar" element={<ExecutarFake />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('button', { name: 'Continuar avaliação' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Iniciar avaliação' }))
    expect(mutateAsync).toHaveBeenCalledWith(3)
    expect(await screen.findByText('executando 99')).toBeInTheDocument()
  })
})

function ExecutarFake() {
  const { id } = useParams()
  return <p>executando {id}</p>
}
