import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type { ApiError, AvaliacaoProgramada, NovaAvaliacaoProgramadaRequest } from '../../../api/types'

const CHAVE = ['avaliacoes-programadas']

/** `GET /avaliacoes-programadas`: avaliações configuradas por série no ano letivo ativo. */
export function useAvaliacoesProgramadas(anoLetivoId?: number) {
  return useQuery({
    queryKey: [...CHAVE, anoLetivoId ?? 'ativo'],
    queryFn: () =>
      request<AvaliacaoProgramada[]>(
        anoLetivoId === undefined ? '/avaliacoes-programadas' : `/avaliacoes-programadas?anoLetivoId=${anoLetivoId}`,
      ),
  })
}

export function useCriarAvaliacaoProgramada() {
  const queryClient = useQueryClient()
  return useMutation<AvaliacaoProgramada, ApiError, NovaAvaliacaoProgramadaRequest>({
    mutationFn: (payload) =>
      request<AvaliacaoProgramada>('/avaliacoes-programadas', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}

export function useRemoverAvaliacaoProgramada() {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, number>({
    mutationFn: (id) => request<void>(`/avaliacoes-programadas/${id}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}
