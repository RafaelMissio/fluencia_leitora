import { useEffect, useState } from 'react'
import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { AlunoBuscaItem, Page } from '../../api/types'

const DEBOUNCE_MS = 300
const MIN_CHARS = 2

/** Debounce genérico: só propaga `value` 300ms após a última mudança (spec.md P1 "Buscar aluno", AC1). */
function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}

/**
 * `GET /alunos?nome=&page=`, com o termo debounced em 300ms e `enabled`
 * apenas com 2+ caracteres (spec.md P1 "Buscar aluno", AC1).
 */
export function useAlunoBusca(nome: string): UseQueryResult<Page<AlunoBuscaItem>> {
  const nomeDebounced = useDebouncedValue(nome.trim(), DEBOUNCE_MS)

  return useQuery({
    queryKey: ['aluno-busca', nomeDebounced],
    queryFn: () =>
      request<Page<AlunoBuscaItem>>(`/alunos?nome=${encodeURIComponent(nomeDebounced)}&page=0`),
    enabled: nomeDebounced.length >= MIN_CHARS,
  })
}
