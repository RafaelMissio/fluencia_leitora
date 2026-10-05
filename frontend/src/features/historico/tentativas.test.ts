import { describe, expect, it } from 'vitest'
import type { HistoricoAvaliacaoItem } from '../../api/types'
import { agruparTentativas } from './tentativas'

function item(o: Partial<HistoricoAvaliacaoItem>): HistoricoAvaliacaoItem {
  return {
    avaliacaoId: 1, anoLetivo: 2026, serie: 2, turma: 'A', professor: 'P', ciclo: 'ENTRADA',
    tipoLeitura: 'PALAVRA', dataAvaliacao: '2026-03-01', quantidadeTotal: 20, quantidadeCorretas: 5,
    quantidadeIncorretas: 15, quantidadeNaoLidas: 0, percentualAcerto: 25, fase: null, nivel: null,
    tempoUtilizadoSegundos: 60, temAudio: false, ativa: true, refeitaDeId: null, ...o,
  }
}

describe('agruparTentativas', () => {
  it('numbers attempts of a refeita chain per cycle by date', () => {
    const r = agruparTentativas([
      item({ avaliacaoId: 3, refeitaDeId: 1, dataAvaliacao: '2026-03-20', quantidadeCorretas: 12 }),
      item({ avaliacaoId: 1, quantidadeCorretas: 5, ativa: false }),
      item({ avaliacaoId: 2, refeitaDeId: 1, dataAvaliacao: '2026-03-10', quantidadeCorretas: 8, ativa: false }),
      item({ avaliacaoId: 9, ciclo: 'SAIDA', quantidadeCorretas: 15 }),
    ])
    expect(r[0].tentativas.map((t) => [t.numero, t.quantidadeCorretas])).toEqual([[1, 5], [2, 8], [3, 12]])
    expect(r[1].tentativas).toEqual([])
    expect(r[2].tentativas).toHaveLength(1)
  })

  it('keeps every chain of a cycle in `cadeias`, not only the latest', () => {
    const r = agruparTentativas([
      item({ avaliacaoId: 1, ativa: false }),
      item({ avaliacaoId: 2, refeitaDeId: 1, dataAvaliacao: '2026-03-02' }),
      item({ avaliacaoId: 5, dataAvaliacao: '2026-04-01' }),
    ])
    expect(r[0].cadeias.map((c) => c.map((t) => t.avaliacaoId))).toEqual([[1, 2], [5]])
    expect(r[0].tentativas.map((t) => t.avaliacaoId)).toEqual([5])
  })
})
