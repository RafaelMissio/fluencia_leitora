import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { EvolucaoCiclosResponse, ResultadoCiclo } from '../../api/types'
import { EvolucaoCiclosTab } from './EvolucaoCiclosTab'
import { useEvolucaoCiclos } from './useEvolucaoCiclos'

vi.mock('./useEvolucaoCiclos', () => ({
  useEvolucaoCiclos: vi.fn(),
}))

function ciclo(overrides: Partial<ResultadoCiclo>): ResultadoCiclo {
  return {
    ciclo: 'ENTRADA',
    dataAvaliacao: '2026-03-01',
    quantidadeCorretas: 6,
    percentualAcerto: 30,
    fase: 'Pré-Leitor Nível 3',
    nivel: 3,
    ...overrides,
  }
}

describe('EvolucaoCiclosTab', () => {
  it('shows the 3 cycles with corretas/classificação/evolução for the chosen tipoLeitura', async () => {
    const resposta: EvolucaoCiclosResponse = {
      alunoId: 42,
      anoLetivo: 2026,
      tipoLeitura: 'PALAVRA',
      entrada: ciclo({ quantidadeCorretas: 6, fase: 'Pré-Leitor Nível 3' }),
      acompanhamento: ciclo({ ciclo: 'ACOMPANHAMENTO', quantidadeCorretas: 10, fase: 'Leitor Iniciante' }),
      saida: ciclo({ ciclo: 'SAIDA', quantidadeCorretas: 15, fase: 'Leitor Fluente' }),
    }
    vi.mocked(useEvolucaoCiclos).mockReturnValue({
      data: resposta,
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    const user = userEvent.setup()
    render(<EvolucaoCiclosTab alunoId={42} />)

    expect(screen.getByText('Entrada')).toBeInTheDocument()
    expect(screen.getByText('Acompanhamento')).toBeInTheDocument()
    expect(screen.getByText('Saída')).toBeInTheDocument()
    expect(screen.getByText('6')).toBeInTheDocument()
    expect(screen.getByText('10')).toBeInTheDocument()
    expect(screen.getByText('15')).toBeInTheDocument()
    expect(screen.getByText('Pré-Leitor Nível 3')).toBeInTheDocument()
    expect(screen.getByText('Leitor Iniciante')).toBeInTheDocument()
    expect(screen.getByText('Leitor Fluente')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Tipo de leitura'), 'TEXTO_CURTO')

    expect(useEvolucaoCiclos).toHaveBeenLastCalledWith(42, undefined, 'TEXTO_CURTO')
  })

  it('shows ▲ green for positive evolution, ▼ red for negative, and — for null', () => {
    const resposta: EvolucaoCiclosResponse = {
      alunoId: 42,
      anoLetivo: 2026,
      tipoLeitura: 'PALAVRA',
      entrada: ciclo({ quantidadeCorretas: 10 }),
      acompanhamento: ciclo({ ciclo: 'ACOMPANHAMENTO', quantidadeCorretas: 16 }),
      saida: ciclo({ ciclo: 'SAIDA', quantidadeCorretas: 12 }),
    }
    vi.mocked(useEvolucaoCiclos).mockReturnValue({
      data: resposta,
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    render(<EvolucaoCiclosTab alunoId={42} />)

    // Entrada não tem ciclo anterior -> "—"
    const linhaEntrada = screen.getByText('Entrada').closest('tr')!
    expect(linhaEntrada).toHaveTextContent('—')

    // Acompanhamento (16) vs Entrada (10) -> positivo, ▲ verde
    const linhaAcompanhamento = screen.getByText('Acompanhamento').closest('tr')!
    expect(linhaAcompanhamento).toHaveTextContent('▲ +6')

    // Saída (12) vs Acompanhamento (16) -> negativo, ▼ vermelho
    const linhaSaida = screen.getByText('Saída').closest('tr')!
    expect(linhaSaida).toHaveTextContent('▼ -4')
  })

  it('handles a cycle with no avaliação (null from backend) without breaking the table', () => {
    const resposta: EvolucaoCiclosResponse = {
      alunoId: 42,
      anoLetivo: 2026,
      tipoLeitura: 'PALAVRA',
      entrada: ciclo({ quantidadeCorretas: 6 }),
      acompanhamento: null,
      saida: null,
    }
    vi.mocked(useEvolucaoCiclos).mockReturnValue({
      data: resposta,
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    render(<EvolucaoCiclosTab alunoId={42} />)

    const linhaAcompanhamento = screen.getByText('Acompanhamento').closest('tr')!
    const celulasAcompanhamento = within(linhaAcompanhamento).getAllByRole('cell')
    expect(celulasAcompanhamento.map((celula) => celula.textContent)).toEqual([
      'Acompanhamento',
      '—',
      '—',
      '—',
    ])

    const linhaSaida = screen.getByText('Saída').closest('tr')!
    const celulasSaida = within(linhaSaida).getAllByRole('cell')
    expect(celulasSaida.map((celula) => celula.textContent)).toEqual(['Saída', '—', '—', '—'])
  })
})
