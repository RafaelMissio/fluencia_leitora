import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { ApiError, AvaliacaoResponse } from '../../api/types'

/** `POST /avaliacoes/{id}/refazer`: cria uma nova avaliação com o mesmo conteúdo de uma finalizada. */
export function useRefazerAvaliacao(alunoId: number): UseMutationResult<AvaliacaoResponse, ApiError, number> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (avaliacaoId) => request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}/refazer`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['avaliacoes-pendentes', alunoId] }),
  })
}
