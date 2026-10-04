import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query'
import { request } from '../../../api/client'
import type { ApiError, CriarAlunoRequest, CriarAlunoResponse, MatriculaResponse } from '../../../api/types'

/** Os resultados da busca por nome (`useAlunoBusca`) são a "lista" desta tela: toda mutação os invalida. */
function useInvalidarBusca(): () => Promise<void> {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ['aluno-busca'] })
}

/** `POST /alunos` (aluno + primeira matrícula). */
export function useCriarAluno(): UseMutationResult<CriarAlunoResponse, ApiError, CriarAlunoRequest> {
  const invalidar = useInvalidarBusca()
  return useMutation({
    mutationFn: (payload) => request<CriarAlunoResponse>('/alunos', { method: 'POST', body: JSON.stringify(payload) }),
    onSuccess: invalidar,
  })
}

/** `PUT /alunos/{id}` - atualiza o nome. */
export function useAtualizarNomeAluno(): UseMutationResult<unknown, ApiError, { alunoId: number; nome: string }> {
  const invalidar = useInvalidarBusca()
  return useMutation({
    mutationFn: ({ alunoId, nome }) =>
      request(`/alunos/${alunoId}`, { method: 'PUT', body: JSON.stringify({ nome }) }),
    onSuccess: invalidar,
  })
}

/** `DELETE /alunos/{id}` - soft-delete. */
export function useInativarAluno(): UseMutationResult<void, ApiError, number> {
  const invalidar = useInvalidarBusca()
  return useMutation({
    mutationFn: (alunoId) => request<void>(`/alunos/${alunoId}`, { method: 'DELETE' }),
    onSuccess: invalidar,
  })
}

/** `POST /alunos/{id}/matriculas` - nova matrícula em outra turma/ano letivo. */
export function useNovaMatricula(): UseMutationResult<MatriculaResponse, ApiError, { alunoId: number; turmaId: number }> {
  const invalidar = useInvalidarBusca()
  return useMutation({
    mutationFn: ({ alunoId, turmaId }) =>
      request<MatriculaResponse>(`/alunos/${alunoId}/matriculas`, {
        method: 'POST',
        body: JSON.stringify({ turmaId }),
      }),
    onSuccess: invalidar,
  })
}
