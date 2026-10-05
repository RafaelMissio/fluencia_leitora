import { describe, expect, it } from 'vitest'
import { calcularCicloAtual } from './cicloAtual'
import type { HistoricoAvaliacaoItem } from '../../api/types'

function item(ciclo: string): HistoricoAvaliacaoItem {
  return {
    avaliacaoId: 1,
    anoLetivo: 2026,
    serie: 2,
    turma: 'A',
    professor: 'Maria',
    ciclo,
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
}

describe('calcularCicloAtual', () => {
  it('returns ENTRADA when the list is empty', () => {
    expect(calcularCicloAtual([])).toBe('ENTRADA')
  })

  it('returns ACOMPANHAMENTO when only ENTRADA is present', () => {
    expect(calcularCicloAtual([item('ENTRADA')])).toBe('ACOMPANHAMENTO')
  })

  it('returns SAIDA when ENTRADA and ACOMPANHAMENTO are present', () => {
    expect(calcularCicloAtual([item('ENTRADA'), item('ACOMPANHAMENTO')])).toBe('SAIDA')
  })

  it('returns SAIDA when all three cycles are present', () => {
    expect(calcularCicloAtual([item('ENTRADA'), item('ACOMPANHAMENTO'), item('SAIDA')])).toBe('SAIDA')
  })
})
