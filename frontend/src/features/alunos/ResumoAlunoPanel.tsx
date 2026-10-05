import { useNavigate } from 'react-router-dom'
import type { AvaliacaoPendente } from '../../api/types'
import { useAplicarAvaliacaoProgramada } from '../avaliacoes/useAplicarAvaliacaoProgramada'
import { useRefazerAvaliacao } from '../avaliacoes/useRefazerAvaliacao'
import { useAvaliacoesPendentes } from '../avaliacoes/useAvaliacoesPendentes'
import { useAlunoResumo } from './useAlunoResumo'

const TIPO_LEITURA_LABEL = { PALAVRA: 'Palavra', PSEUDOPALAVRA: 'Pseudopalavra', TEXTO_CURTO: 'Texto curto' }

/**
 * Painel de resumo do aluno (EVO-01, spec.md P1 "Buscar aluno", AC2): nome,
 * turma, professor, ano letivo, série, ciclo atual, última classificação, as
 * 5 avaliações mais recentes e a evolução no ano. Embutido em
 * `AlunoBuscaPage` quando há um aluno selecionado.
 */
export function ResumoAlunoPanel({ alunoId }: { alunoId: number }) {
  const navigate = useNavigate()
  const { aluno, ultimasAvaliacoes, isLoading } = useAlunoResumo(alunoId)
  const { data: pendentes = [] } = useAvaliacoesPendentes(alunoId)
  const aplicar = useAplicarAvaliacaoProgramada(alunoId)
  const refazer = useRefazerAvaliacao(alunoId)

  /** Avaliação da série ainda não criada para o aluno: cria a partir da configuração e abre; as demais abrem direto. */
  async function abrir(pendente: AvaliacaoPendente): Promise<void> {
    if (pendente.avaliacaoId !== null) {
      navigate(`/avaliacoes/${pendente.avaliacaoId}/executar`)
      return
    }
    try {
      const avaliacao = await aplicar.mutateAsync(pendente.programadaId as number)
      navigate(`/avaliacoes/${avaliacao.id}/executar`)
    } catch {
      // o motivo aparece em `aplicar.error`
    }
  }

  if (isLoading || !aluno) {
    return <p>Carregando resumo…</p>
  }

  return (
    <section aria-label="Resumo do aluno">
      <h2>{aluno.nome}</h2>
      <h3>Últimas avaliações</h3>
      {ultimasAvaliacoes.length === 0 ? (
        <p>Nenhuma avaliação finalizada ainda</p>
      ) : (
        <ul>
          {ultimasAvaliacoes.map((item) => (
            <li key={item.avaliacaoId}>
              {item.dataAvaliacao} - {item.ciclo} - {item.tipoLeitura} - {item.quantidadeCorretas}/
              {item.quantidadeTotal} corretas{' '}
              {item.ativa ? (
                <button type="button" disabled={refazer.isPending} onClick={() => void refazer.mutateAsync(item.avaliacaoId).then((a) => navigate(`/avaliacoes/${a.id}/executar?modo=refazer`), () => {})}>
                  Refazer avaliação
                </button>
              ) : (
                <em>(inativa - refeita)</em>
              )}
            </li>
          ))}
        </ul>
      )}
      {refazer.error ? <p role="alert">{refazer.error.detail ?? 'Não foi possível refazer a avaliação'}</p> : null}

      {pendentes.length > 0 ? (
        <>
          <h3>Avaliações para fazer</h3>
          <ul>
            {pendentes.map((pendente) => (
              <li key={pendente.avaliacaoId ?? `programada-${pendente.programadaId}`}>
                {pendente.nome ? `${pendente.nome} - ` : ''}
                {TIPO_LEITURA_LABEL[pendente.tipoLeitura]} - {pendente.tempoSegundos}s{' '}
                <button
                  type="button"
                  className="primary"
                  disabled={aplicar.isPending}
                  onClick={() => void abrir(pendente)}
                >
                  {pendente.status === null || pendente.status === 'CRIADA' ? 'Iniciar avaliação' : 'Continuar avaliação'}
                </button>
              </li>
            ))}
          </ul>
          {aplicar.error ? <p role="alert">{aplicar.error.detail ?? 'Não foi possível abrir a avaliação'}</p> : null}
        </>
      ) : null}

      <button type="button" className="primary" onClick={() => navigate(`/avaliacoes/nova?alunoId=${alunoId}`)}>
        Configurar avaliação
      </button>
    </section>
  )
}
