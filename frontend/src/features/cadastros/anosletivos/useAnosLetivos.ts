import { useMutation, useQuery, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type {
  AnoLetivoResponse,
  ApiError,
  AtualizarConfiguracaoRequest,
  ConfiguracaoAvaliacaoResponse,
  CriarAnoLetivoRequest,
} from '../../../api/types'

/** `GET /anos-letivos` (COORDENADOR; só registros ativos). */
export function useAnosLetivos() {
  return useQuery({
    queryKey: ['anos-letivos'],
    queryFn: () => request<AnoLetivoResponse[]>('/anos-letivos'),
  })
}

/** `GET /anos-letivos/{id}/configuracoes` (séries 1-5 do ano). */
export function useConfiguracoesDoAno(anoLetivoId: number | undefined) {
  return useQuery({
    queryKey: ['anos-letivos', anoLetivoId, 'configuracoes'],
    queryFn: () => request<ConfiguracaoAvaliacaoResponse[]>(`/anos-letivos/${anoLetivoId}/configuracoes`),
    enabled: anoLetivoId !== undefined,
  })
}

/** `POST /anos-letivos`; invalida a lista no sucesso. */
export function useCriarAnoLetivo(): UseMutationResult<AnoLetivoResponse, ApiError, CriarAnoLetivoRequest> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload) =>
      request<AnoLetivoResponse>('/anos-letivos', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['anos-letivos'] }),
  })
}

/** `PUT /anos-letivos/{id}/configuracoes/{serie}`; invalida as configurações do ano no sucesso. */
export function useAtualizarConfiguracao(
  anoLetivoId: number,
): UseMutationResult<ConfiguracaoAvaliacaoResponse, ApiError, AtualizarConfiguracaoRequest & { serie: number }> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ serie, ...payload }) =>
      request<ConfiguracaoAvaliacaoResponse>(`/anos-letivos/${anoLetivoId}/configuracoes/${serie}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['anos-letivos', anoLetivoId, 'configuracoes'] }),
  })
}
