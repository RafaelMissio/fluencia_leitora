import { useMutation, useQuery, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type { ApiError, ProfessorResponse } from '../../../api/types'

/** `GET /professores` (COORDENADOR; só ativos, cada um com suas turmas ativas). */
export function useProfessores() {
  return useQuery({ queryKey: ['professores'], queryFn: () => request<ProfessorResponse[]>('/professores') })
}

/** `POST /professores`; invalida a lista no sucesso. */
export function useCriarProfessor(): UseMutationResult<ProfessorResponse, ApiError, { nome: string }> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload) =>
      request<ProfessorResponse>('/professores', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['professores'] }),
  })
}
