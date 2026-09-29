import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { HistoricoAvaliacaoItem, Page } from '../../api/types'
import { HistoricoTab } from './HistoricoTab'
import { useHistorico } from './useHistorico'

vi.mock('./useHistorico', () => ({
  useHistorico: vi.fn(),
}))

function pageOf(content: HistoricoAvaliacaoItem[], totalPages = 1, number = 0): Page<HistoricoAvaliacaoItem> {
  return { content, totalElements: content.length, totalPages, number, size: 20 }
}

const COM_AUDIO: HistoricoAvaliacaoItem = {
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

const SEM_AUDIO: HistoricoAvaliacaoItem = {
  ...COM_AUDIO,
  avaliacaoId: 2,
  dataAvaliacao: '2026-04-01',
  temAudio: false,
}

describe('HistoricoTab', () => {
  it('renders every SDD §16 column for each row', () => {
    vi.mocked(useHistorico).mockReturnValue({
      data: pageOf([COM_AUDIO]),
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    render(<HistoricoTab alunoId={42} />)

    for (const header of [
      'Ano letivo',
      'Série',
      'Turma',
      'Professor',
      'Ciclo',
      'Tipo de leitura',
      'Data da avaliação',
      'Quantidade de palavras',
      'Palavras corretas',
      'Palavras incorretas',
      'Palavras não lidas',
      'Percentual',
      'Classificação',
      'Nível',
      'Tempo',
      'Áudio',
    ]) {
      expect(screen.getByRole('columnheader', { name: header })).toBeInTheDocument()
    }

    expect(screen.getByText('2026')).toBeInTheDocument()
    expect(screen.getByText('2ª série')).toBeInTheDocument()
    expect(screen.getByText('A')).toBeInTheDocument()
    expect(screen.getByText('Maria')).toBeInTheDocument()
    expect(screen.getByText('ENTRADA')).toBeInTheDocument()
    expect(screen.getByText('PALAVRA')).toBeInTheDocument()
    expect(screen.getByText('2026-03-01')).toBeInTheDocument()
    expect(screen.getByText('20')).toBeInTheDocument()
    expect(screen.getByText('15')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('75%')).toBeInTheDocument()
    expect(screen.getByText('ALFABETICA')).toBeInTheDocument()
    expect(screen.getByText('55')).toBeInTheDocument()
  })

  it('shows the play icon only for the row with temAudio: true', () => {
    vi.mocked(useHistorico).mockReturnValue({
      data: pageOf([COM_AUDIO, SEM_AUDIO]),
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    render(<HistoricoTab alunoId={42} />)

    expect(screen.getByRole('img', { name: 'Avaliação de 2026-03-01 tem áudio' })).toBeInTheDocument()
    expect(screen.queryByRole('img', { name: 'Avaliação de 2026-04-01 tem áudio' })).not.toBeInTheDocument()
  })

  it('paginates by requesting the next page from useHistorico when "Próxima" is clicked', async () => {
    vi.mocked(useHistorico).mockReturnValue({
      data: pageOf([COM_AUDIO], 2, 0),
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    const user = userEvent.setup()
    render(<HistoricoTab alunoId={42} />)

    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Próxima' }))

    expect(useHistorico).toHaveBeenLastCalledWith(42, {}, 1)
  })
})
