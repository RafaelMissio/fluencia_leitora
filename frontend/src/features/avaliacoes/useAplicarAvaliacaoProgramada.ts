import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { ApiError, AvaliacaoResponse } from '../../api/types'

/** `POST /avaliacoes-programadas/{id}/aplicar`: cria para o aluno a avaliação configurada para a série dele. */
export function useAplicarAvaliacaoProgramada(
  alunoId: number,
): UseMutationResult<AvaliacaoResponse, ApiError, number> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (programadaId) =>
      request<AvaliacaoResponse>(`/avaliacoes-programadas/${programadaId}/aplicar`, {
        method: 'POST',
        body: JSON.stringify({ alunoId }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['avaliacoes-pendentes', alunoId] }),
  })
}
