import { useMutation, useQuery, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type { ApiError, CriarTurmaRequest, TurmaResponse } from '../../../api/types'

/** `GET /turmas` (COORDENADOR; só turmas ativas). */
export function useTurmas() {
  return useQuery({ queryKey: ['turmas'], queryFn: () => request<TurmaResponse[]>('/turmas') })
}

export function useCriarTurma(): UseMutationResult<TurmaResponse, ApiError, CriarTurmaRequest> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload) => request<TurmaResponse>('/turmas', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['turmas'] }),
  })
}

/** `PUT /turmas/{id}` - troca o professor responsável. */
export function useTrocarProfessorDaTurma(): UseMutationResult<
  TurmaResponse,
  ApiError,
  { turmaId: number; professorId: number }
> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ turmaId, professorId }) =>
      request<TurmaResponse>(`/turmas/${turmaId}`, { method: 'PUT', body: JSON.stringify({ professorId }) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['turmas'] }),
  })
}
