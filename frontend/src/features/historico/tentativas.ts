import type { HistoricoAvaliacaoItem } from '../../api/types'

export const CICLOS_ORDEM = ['ENTRADA', 'ACOMPANHAMENTO', 'SAIDA'] as const

export interface Tentativa {
  avaliacaoId: number
  numero: number
  nomeAvaliacao: string | null
  dataAvaliacao: string
  quantidadeCorretas: number
  percentualAcerto: number
  fase: string | null
}

export interface TentativasCiclo {
  ciclo: string
  /** Cadeia mais recente (base dos gráficos). */
  tentativas: Tentativa[]
  /** Todas as cadeias do ciclo, cada uma com todas as suas tentativas. */
  cadeias: Tentativa[][]
}

/**
 * Cada vez que uma avaliação é refeita nasce uma tentativa; todas as
 * tentativas de uma avaliação compartilham a cadeia `refeitaDeId ?? avaliacaoId`.
 * Considera o ano letivo mais recente do histórico. Por ciclo, devolve todas as
 * cadeias (`cadeias`, para a tabela) e a mais recente (`tentativas`, para os
 * gráficos). Tentativas ordenadas por data e id.
 */
export function agruparTentativas(itens: HistoricoAvaliacaoItem[]): TentativasCiclo[] {
  const ano = Math.max(...itens.map((i) => i.anoLetivo))
  const doAno = itens.filter((i) => i.anoLetivo === ano)

  return CICLOS_ORDEM.map((ciclo) => {
    const cadeias = new Map<number, HistoricoAvaliacaoItem[]>()
    for (const item of doAno.filter((i) => i.ciclo === ciclo)) {
      const chave = item.refeitaDeId ?? item.avaliacaoId
      cadeias.set(chave, [...(cadeias.get(chave) ?? []), item])
    }
    const ordenar = (a: HistoricoAvaliacaoItem, b: HistoricoAvaliacaoItem) =>
      a.dataAvaliacao.localeCompare(b.dataAvaliacao) || a.avaliacaoId - b.avaliacaoId
    const ordenadas = [...cadeias.values()].map((c) => [...c].sort(ordenar))
    const aTentativa = (i: HistoricoAvaliacaoItem, idx: number): Tentativa => ({
      avaliacaoId: i.avaliacaoId,
      numero: i.numeroTentativa ?? idx + 1,
      nomeAvaliacao: i.nomeAvaliacao ?? null,
      dataAvaliacao: i.dataAvaliacao,
      quantidadeCorretas: i.quantidadeCorretas,
      percentualAcerto: i.percentualAcerto,
      fase: i.fase,
    })
    const mapeadas = ordenadas.map((c) => c.map(aTentativa))
    const maisRecente = [...ordenadas].sort((a, b) => ordenar(b[b.length - 1], a[a.length - 1]))[0] ?? []
    return {
      ciclo,
      tentativas: maisRecente.map(aTentativa),
      cadeias: mapeadas.sort((a, b) => a[0].avaliacaoId - b[0].avaliacaoId),
    }
  })
}
