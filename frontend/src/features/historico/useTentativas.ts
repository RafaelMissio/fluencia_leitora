import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { HistoricoAvaliacaoItem, Page, TipoLeituraCodigo } from '../../api/types'

/**
 * Todas as avaliações finalizadas (inclusive as refeitas, `ativa: false`) do
 * aluno para um tipo de leitura, percorrendo as páginas de
 * `GET /alunos/{id}/historico-avaliacoes`. Base da comparação de tentativas.
 */
export function useTentativas(
  alunoId: number,
  tipoLeitura: TipoLeituraCodigo,
): UseQueryResult<HistoricoAvaliacaoItem[]> {
  return useQuery({
    queryKey: ['tentativas', alunoId, tipoLeitura],
    queryFn: async () => {
      const itens: HistoricoAvaliacaoItem[] = []
      for (let page = 0; ; page++) {
        const resposta = await request<Page<HistoricoAvaliacaoItem>>(
          `/alunos/${alunoId}/historico-avaliacoes?tipoLeitura=${tipoLeitura}&page=${page}`,
        )
        itens.push(...resposta.content)
        if (page + 1 >= resposta.totalPages) break
      }
      return itens
    },
  })
}
