import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { EvolucaoAnualResponse, TipoLeituraCodigo } from '../../api/types'

/**
 * `GET /alunos/{id}/evolucao-anos?tipoLeitura=` -
 * `HistoricoEvolucaoController.evolucaoAnos`, `hasRole('COORDENADOR')`.
 * PROFESSOR recebe 403 (context.md, "Acesso a evolução") - o hook não faz
 * gating de perfil sozinho, é só chamado dentro do `RoleGate` COORDENADOR de
 * `ComparacaoAnualTab`/`HistoricoPage` (T28), que impede o componente (e
 * este hook) de sequer montar para PROFESSOR.
 */
export function useEvolucaoAnual(alunoId: number, tipoLeitura: TipoLeituraCodigo): UseQueryResult<EvolucaoAnualResponse> {
  return useQuery({
    queryKey: ['evolucao-anual', alunoId, tipoLeitura],
    queryFn: () => request<EvolucaoAnualResponse>(`/alunos/${alunoId}/evolucao-anos?tipoLeitura=${tipoLeitura}`),
  })
}
