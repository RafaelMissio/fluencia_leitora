import { useMutation, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { ApiError, AvaliacaoResponse, NovaAvaliacaoRequest } from '../../api/types'

/**
 * `POST /avaliacoes`, tipado com `NovaAvaliacaoRequest`/`AvaliacaoResponse`
 * (design.md, Components). O `ApiError` (incluindo `errors[]` de 422) fica
 * disponível em `mutation.error` para o formulário mapear por campo (FE-10).
 */
export function useCriarAvaliacao(): UseMutationResult<AvaliacaoResponse, ApiError, NovaAvaliacaoRequest> {
  return useMutation({
    mutationFn: (payload) =>
      request<AvaliacaoResponse>('/avaliacoes', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
  })
}
