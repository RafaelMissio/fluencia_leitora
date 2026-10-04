import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type { ApiError, ListaPalavrasRequest, ListaPalavrasResponse } from '../../../api/types'

/** A leitura (lista filtrada por série + tipo) é `useListasPalavras` (T12); toda mutação invalida o prefixo `listas-palavras`. */
export function useCriarLista(): UseMutationResult<ListaPalavrasResponse, ApiError, ListaPalavrasRequest> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload) =>
      request<ListaPalavrasResponse>('/listas-palavras', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-palavras'] }),
  })
}

/** `PUT /listas-palavras/{id}` com `version` (lock otimista; 409 `CONFLITO_DE_VERSAO` se divergente). */
export function useAtualizarLista(): UseMutationResult<
  ListaPalavrasResponse,
  ApiError,
  { id: number; version: number; payload: ListaPalavrasRequest }
> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, version, payload }) =>
      request<ListaPalavrasResponse>(`/listas-palavras/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ ...payload, version }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-palavras'] }),
  })
}

export function useInativarLista(): UseMutationResult<void, ApiError, number> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id) => request<void>(`/listas-palavras/${id}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['listas-palavras'] }),
  })
}

/** `GET /listas-palavras/{id}` - detalhe com itens e `version`, para preencher a edição. */
export function buscarListaPorId(id: number): Promise<ListaPalavrasResponse> {
  return request<ListaPalavrasResponse>(`/listas-palavras/${id}`)
}
