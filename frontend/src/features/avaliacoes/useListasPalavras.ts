import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { TipoLeituraCodigo } from '../../api/types'

/**
 * `ListaPalavrasResumoResponse` (backend) não estava detalhada por completo
 * em design.md - definida aqui, junto do único hook que a consome (T12,
 * "Reuses").
 */
export interface ListaPalavrasResumo {
  id: number
  nome: string
  quantidadePalavras: number
}

/**
 * `GET /listas-palavras?serie=&tipoLeitura=` (spec.md P1 "Configurar
 * avaliação", AC2): lista as listas de palavras da série do aluno e do tipo
 * de leitura escolhido. Só dispara com os 2 parâmetros definidos.
 */
export function useListasPalavras(
  serie: number | undefined,
  tipoLeitura: TipoLeituraCodigo | undefined,
): UseQueryResult<ListaPalavrasResumo[]> {
  return useQuery({
    queryKey: ['listas-palavras', serie, tipoLeitura],
    queryFn: () => request<ListaPalavrasResumo[]>(`/listas-palavras?serie=${serie}&tipoLeitura=${tipoLeitura}`),
    enabled: serie !== undefined && tipoLeitura !== undefined,
  })
}
