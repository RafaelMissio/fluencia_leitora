import { useState } from 'react'
import { AudioHistorico } from './AudioHistorico'
import { useHistorico } from './useHistorico'

/**
 * Tabela paginada do histórico do aluno (spec.md P1 "Histórico e evolução",
 * AC1; colunas do SDD §16) com um ícone de play nas avaliações com áudio.
 */
export function HistoricoTab({ alunoId }: { alunoId: number }) {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useHistorico(alunoId, {}, page)

  const itens = data?.content ?? []
  const totalPages = data?.totalPages ?? 0

  return (
    <section aria-label="Histórico de avaliações">
      {isLoading ? (
        <p>Carregando histórico…</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Avaliação</th>
              <th>Ano letivo</th>
              <th>Série</th>
              <th>Turma</th>
              <th>Professor</th>
              <th>Ciclo</th>
              <th>Tipo de leitura</th>
              <th>Data da avaliação</th>
              <th>Quantidade de palavras</th>
              <th>Palavras corretas</th>
              <th>Palavras incorretas</th>
              <th>Palavras não lidas</th>
              <th>Percentual</th>
              <th>Classificação</th>
              <th>Nível</th>
              <th>Tempo</th>
              <th>Áudio</th>
              <th>Situação</th>
            </tr>
          </thead>
          <tbody>
            {itens.map((item) => (
              <tr key={item.avaliacaoId} className={item.ativa ? undefined : 'inativa'}>
                <td>{item.nomeAvaliacao ?? '—'}</td>
                <td>{item.anoLetivo}</td>
                <td>{item.serie}ª série</td>
                <td>{item.turma}</td>
                <td>{item.professor}</td>
                <td>{item.ciclo}</td>
                <td>{item.tipoLeitura}</td>
                <td>{item.dataAvaliacao}</td>
                <td>{item.quantidadeTotal}</td>
                <td>{item.quantidadeCorretas}</td>
                <td>{item.quantidadeIncorretas}</td>
                <td>{item.quantidadeNaoLidas}</td>
                <td>{item.percentualAcerto}%</td>
                <td>{item.fase ?? '—'}</td>
                <td>{item.nivel ?? '—'}</td>
                <td>{item.tempoUtilizadoSegundos ?? '—'}</td>
                <td>
                  {item.temAudio ? <AudioHistorico avaliacaoId={item.avaliacaoId} dataAvaliacao={item.dataAvaliacao} /> : null}
                </td>
                <td>{item.ativa ? 'Ativa' : 'Inativa'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div>
        <button type="button" disabled={page === 0} onClick={() => setPage((atual) => atual - 1)}>
          Anterior
        </button>
        <span>
          Página {page + 1} de {Math.max(totalPages, 1)}
        </span>
        <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((atual) => atual + 1)}>
          Próxima
        </button>
      </div>
    </section>
  )
}
