import { useQuery } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { AvaliacaoPendente } from '../../api/types'

/** `GET /avaliacoes/pendentes?alunoId=`: avaliações em aberto do aluno e as configuradas pelo coordenador para a série dele ainda não iniciadas (pode haver várias). O PROFESSOR as executa; o COORDENADOR só consulta. */
export function useAvaliacoesPendentes(alunoId: number) {
  return useQuery({
    queryKey: ['avaliacoes-pendentes', alunoId],
    queryFn: () => request<AvaliacaoPendente[]>(`/avaliacoes/pendentes?alunoId=${alunoId}`),
  })
}
