import { useMutation, useQuery, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type { ApiError, FaixaRequest, RegraClassificacaoResponse } from '../../../api/types'

/** `GET /regras-classificacao?serie=` (faixas ativas, PROFESSOR+COORDENADOR). */
export function useRegrasClassificacao(serie: number) {
  return useQuery({
    queryKey: ['regras-classificacao', serie],
    queryFn: () => request<RegraClassificacaoResponse[]>(`/regras-classificacao?serie=${serie}`),
  })
}

/** `PUT /regras-classificacao/series/{serie}` - substitui o conjunto inteiro de faixas. */
export function useSubstituirRegras(
  serie: number,
): UseMutationResult<RegraClassificacaoResponse[], ApiError, FaixaRequest[]> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (faixas) =>
      request<RegraClassificacaoResponse[]>(`/regras-classificacao/series/${serie}`, {
        method: 'PUT',
        body: JSON.stringify({ faixas }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['regras-classificacao', serie] }),
  })
}
