import type { HistoricoAvaliacaoItem } from '../../api/types'

export type CicloAtualCodigo = 'ENTRADA' | 'ACOMPANHAMENTO' | 'SAIDA'

const ORDEM_CICLOS: CicloAtualCodigo[] = ['ENTRADA', 'ACOMPANHAMENTO', 'SAIDA']

/**
 * context.md, "Ciclo atual": o backend não tem o conceito de "ciclo em vigor
 * por data" - o frontend deriva o ciclo atual olhando as avaliações
 * FINALIZADA do aluno no ano ATIVO (`itens`, de `historico-avaliacoes`) e
 * escolhe o primeiro ciclo, na ordem ENTRADA -> ACOMPANHAMENTO -> SAIDA, que
 * ainda não aparece em nenhuma delas. Se os três já aparecem, usa SAIDA.
 */
export function calcularCicloAtual(itens: HistoricoAvaliacaoItem[]): CicloAtualCodigo {
  const ciclosPresentes = new Set(itens.map((item) => item.ciclo))

  for (const ciclo of ORDEM_CICLOS) {
    if (!ciclosPresentes.has(ciclo)) {
      return ciclo
    }
  }

  return 'SAIDA'
}
