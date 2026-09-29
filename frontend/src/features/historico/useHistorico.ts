import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { HistoricoAvaliacaoItem, Page, TipoLeituraCodigo } from '../../api/types'

export interface HistoricoFiltros {
  anoLetivoId?: number
  tipoLeitura?: TipoLeituraCodigo
  cicloId?: number
}

function montarQueryString(filtros: HistoricoFiltros, page: number): string {
  const params = new URLSearchParams()
  if (filtros.anoLetivoId !== undefined) params.set('anoLetivoId', String(filtros.anoLetivoId))
  if (filtros.tipoLeitura !== undefined) params.set('tipoLeitura', filtros.tipoLeitura)
  if (filtros.cicloId !== undefined) params.set('cicloId', String(filtros.cicloId))
  params.set('page', String(page))
  return params.toString()
}

/**
 * `GET /alunos/{id}/historico-avaliacoes?anoLetivoId=&tipoLeitura=&cicloId=&page=`
 * (spec.md P1 "Histórico e evolução", AC1). Aberto a PROFESSOR e COORDENADOR
 * - `HistoricoEvolucaoController.historico`, `hasAnyRole('PROFESSOR','COORDENADOR')`.
 * Filtros só entram na URL quando definidos.
 */
export function useHistorico(
  alunoId: number,
  filtros: HistoricoFiltros = {},
  page = 0,
): UseQueryResult<Page<HistoricoAvaliacaoItem>> {
  const { anoLetivoId, tipoLeitura, cicloId } = filtros

  return useQuery({
    queryKey: ['historico', alunoId, anoLetivoId, tipoLeitura, cicloId, page],
    queryFn: () =>
      request<Page<HistoricoAvaliacaoItem>>(
        `/alunos/${alunoId}/historico-avaliacoes?${montarQueryString(filtros, page)}`,
      ),
  })
}
