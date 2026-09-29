import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { CicloAnual, EvolucaoAnualResponse } from '../../api/types'
import { ComparacaoAnualTab } from './ComparacaoAnualTab'
import { useEvolucaoAnual } from './useEvolucaoAnual'

vi.mock('./useEvolucaoAnual', () => ({
  useEvolucaoAnual: vi.fn(),
}))

function cicloAnual(overrides: Partial<CicloAnual>): CicloAnual {
  return {
    ciclo: 'ENTRADA',
    quantidadeCorretas: 12,
    percentualAcerto: 60,
    fase: 'Pré-Leitor Nível 3',
    nivel: 3,
    evolucao: { absoluta: null, percentual: null },
    ...overrides,
  }
}

// spec.md, Independent Test da história "Histórico e evolução": "Um aluno
// com dados de 2026 e 2027 mostra a comparação com +13 / 108,33% na
// Entrada" (SDD §15: Entrada 2026 = 12, 2027 = 25).
const RESPOSTA: EvolucaoAnualResponse = {
  alunoId: 42,
  tipoLeitura: 'PALAVRA',
  anos: [
    {
      anoLetivo: 2026,
      serie: 2,
      entrada: cicloAnual({ quantidadeCorretas: 12 }),
      acompanhamento: cicloAnual({ ciclo: 'ACOMPANHAMENTO', quantidadeCorretas: 22 }),
      saida: cicloAnual({ ciclo: 'SAIDA', quantidadeCorretas: 32 }),
    },
    {
      anoLetivo: 2027,
      serie: 3,
      entrada: cicloAnual({ quantidadeCorretas: 25, evolucao: { absoluta: 13, percentual: 108.33 } }),
      acompanhamento: cicloAnual({
        ciclo: 'ACOMPANHAMENTO',
        quantidadeCorretas: 36,
        evolucao: { absoluta: 14, percentual: 63.64 },
      }),
      saida: cicloAnual({ ciclo: 'SAIDA', quantidadeCorretas: 48, evolucao: { absoluta: 16, percentual: 50 } }),
    },
  ],
}

describe('ComparacaoAnualTab', () => {
  it('renders one row per ano letivo, ordered ascending, with the 3 ciclos', () => {
    vi.mocked(useEvolucaoAnual).mockReturnValue({
      data: RESPOSTA,
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    render(<ComparacaoAnualTab alunoId={42} />)

    const linhas = screen.getAllByRole('row').slice(1) // pula o cabeçalho
    expect(linhas).toHaveLength(2)
    expect(within(linhas[0]).getByText('2026')).toBeInTheDocument()
    expect(within(linhas[1]).getByText('2027')).toBeInTheDocument()
    expect(within(linhas[0]).getByText('2º Ano')).toBeInTheDocument()
    expect(within(linhas[1]).getByText('3º Ano')).toBeInTheDocument()
  })

  it('shows the correct absoluta/percentual for 2 consecutive years in the same ciclo (+13 / 108,33% na Entrada)', () => {
    vi.mocked(useEvolucaoAnual).mockReturnValue({
      data: RESPOSTA,
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    render(<ComparacaoAnualTab alunoId={42} />)

    const linhas = screen.getAllByRole('row').slice(1)
    const linha2027 = linhas[1]
    const celulaEntrada = within(linha2027).getAllByRole('cell')[2]
    expect(celulaEntrada).toHaveTextContent('25 (+13 / 108,33%)')
  })

  it('shows "sem base" (not "0%" nor an error) for a ciclo with no previous year to compare', () => {
    vi.mocked(useEvolucaoAnual).mockReturnValue({
      data: RESPOSTA,
      isLoading: false,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any)

    render(<ComparacaoAnualTab alunoId={42} />)

    const linhas = screen.getAllByRole('row').slice(1)
    const linha2026 = linhas[0]
    const celulaEntrada = within(linha2026).getAllByRole('cell')[2]
    expect(celulaEntrada).toHaveTextContent('12 (sem base)')
    expect(celulaEntrada).not.toHaveTextContent('0%')
  })
})
