import type { MatriculaAno, StatusMatricula } from '../../api/types'

export const STATUS_MATRICULA_LABEL: Record<StatusMatricula, string> = {
  CURSANDO: 'Cursando',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado',
}

/** Status do aluno em cada ano letivo (o ativo e os inativos). */
export function StatusMatriculaLista({ matriculas }: { matriculas: MatriculaAno[] }) {
  if (matriculas.length === 0) return null
  return (
    <ul aria-label="Status por ano letivo">
      {matriculas.map((m) => (
        <li key={m.matriculaId}>
          {m.anoLetivo} ({m.situacaoAnoLetivo === 'ATIVO' ? 'ano ativo' : 'ano inativo'}) - {m.turma} -{' '}
          {m.situacaoAnoLetivo !== 'ATIVO' && m.status === 'CURSANDO' ? 'Resultado pendente' : STATUS_MATRICULA_LABEL[m.status]}
        </li>
      ))}
    </ul>
  )
}
