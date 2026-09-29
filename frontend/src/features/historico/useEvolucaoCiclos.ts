import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { EvolucaoCiclosResponse, TipoLeituraCodigo } from '../../api/types'

/**
 * `GET /alunos/{id}/evolucao-ciclos?anoLetivoId=&tipoLeitura=` -
 * `HistoricoEvolucaoController.evolucaoCiclos`, `hasRole('COORDENADOR')`.
 * Correção crítica (context.md, "Acesso a evolução"): PROFESSOR recebe 403
 * deste endpoint - o hook não faz gating de perfil sozinho, é só chamado
 * dentro do `RoleGate` COORDENADOR de `EvolucaoCiclosTab`/`HistoricoPage`
 * (T28), que impede o componente (e este hook) de sequer montar para
 * PROFESSOR.
 */
export function useEvolucaoCiclos(
  alunoId: number,
  anoLetivoId: number | undefined,
  tipoLeitura: TipoLeituraCodigo,
): UseQueryResult<EvolucaoCiclosResponse> {
  return useQuery({
    queryKey: ['evolucao-ciclos', alunoId, anoLetivoId, tipoLeitura],
    queryFn: () => {
      const params = new URLSearchParams()
      if (anoLetivoId !== undefined) params.set('anoLetivoId', String(anoLetivoId))
      params.set('tipoLeitura', tipoLeitura)
      return request<EvolucaoCiclosResponse>(`/alunos/${alunoId}/evolucao-ciclos?${params.toString()}`)
    },
  })
}
