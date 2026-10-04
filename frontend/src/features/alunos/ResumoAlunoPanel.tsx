import { useNavigate } from 'react-router-dom'
import { useAlunoResumo } from './useAlunoResumo'

function formatarEvolucao(evolucao: number | null): string {
  if (evolucao === null) return '—'
  return evolucao > 0 ? `+${evolucao}` : String(evolucao)
}

/**
 * Painel de resumo do aluno (EVO-01, spec.md P1 "Buscar aluno", AC2): nome,
 * turma, professor, ano letivo, série, ciclo atual, última classificação, as
 * 5 avaliações mais recentes e a evolução no ano. Embutido em
 * `AlunoBuscaPage` quando há um aluno selecionado.
 */
export function ResumoAlunoPanel({ alunoId }: { alunoId: number }) {
  const navigate = useNavigate()
  const { aluno, cicloAtual, ultimasAvaliacoes, ultimaClassificacao, evolucao, isLoading } =
    useAlunoResumo(alunoId)

  if (isLoading || !aluno) {
    return <p>Carregando resumo…</p>
  }

  return (
    <section aria-label="Resumo do aluno">
      <h2>{aluno.nome}</h2>
      <dl>
        <dt>Turma</dt>
        <dd>{aluno.turma ?? '—'}</dd>
        <dt>Professor</dt>
        <dd>{aluno.professor ?? '—'}</dd>
        <dt>Ano letivo</dt>
        <dd>{aluno.anoLetivo ?? '—'}</dd>
        <dt>Série</dt>
        <dd>{aluno.serie}ª série</dd>
        <dt>Ciclo atual</dt>
        <dd>{cicloAtual}</dd>
        <dt>Última classificação</dt>
        <dd>
          {ultimaClassificacao ? `${ultimaClassificacao.fase ?? '—'} (nível ${ultimaClassificacao.nivel ?? '—'})` : '—'}
        </dd>
        <dt>Evolução no ano</dt>
        <dd>{formatarEvolucao(evolucao)}</dd>
      </dl>

      <h3>Últimas avaliações</h3>
      {ultimasAvaliacoes.length === 0 ? (
        <p>Nenhuma avaliação finalizada ainda</p>
      ) : (
        <ul>
          {ultimasAvaliacoes.map((item) => (
            <li key={item.avaliacaoId}>
              {item.dataAvaliacao} - {item.ciclo} - {item.tipoLeitura} - {item.quantidadeCorretas}/
              {item.quantidadeTotal} corretas
            </li>
          ))}
        </ul>
      )}

      <button type="button" onClick={() => navigate(`/avaliacoes/nova?alunoId=${alunoId}`)}>
        Configurar avaliação
      </button>
      <button type="button" onClick={() => navigate(`/alunos/${alunoId}/historico`)}>
        Ver histórico
      </button>
    </section>
  )
}
