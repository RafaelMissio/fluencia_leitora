import { useMutation, useQuery, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type { ApiError, CriarUsuarioRequest, UsuarioResponse } from '../../../api/types'

/** `GET /usuarios` (COORDENADOR; nunca traz senha nem hash). */
export function useUsuarios() {
  return useQuery({ queryKey: ['usuarios'], queryFn: () => request<UsuarioResponse[]>('/usuarios') })
}

export function useCriarUsuario(): UseMutationResult<UsuarioResponse, ApiError, CriarUsuarioRequest> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload) =>
      request<UsuarioResponse>('/usuarios', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['usuarios'] }),
  })
}

/** `PUT /usuarios/{id}/senha` (204). A senha só trafega no corpo da requisição; nada é guardado em cache nem logado. */
export function useAlterarSenha(): UseMutationResult<void, ApiError, { usuarioId: number; senha: string }> {
  return useMutation({
    mutationFn: ({ usuarioId, senha }) =>
      request<void>(`/usuarios/${usuarioId}/senha`, { method: 'PUT', body: JSON.stringify({ senha }) }),
  })
}
