import type { TipoLeituraCodigo } from '../../api/types'
import { TentativasChart } from './TentativasChart'
import { agruparTentativas } from './tentativas'
import { useTentativas } from './useTentativas'

const ROTULOS: Record<string, string> = { ENTRADA: 'Entrada', ACOMPANHAMENTO: 'Acompanhamento', SAIDA: 'Saída' }

/** Comparação das tentativas (avaliações refeitas) de cada ciclo, em tabela e gráfico. */
export function TentativasSection({ alunoId, tipoLeitura }: { alunoId: number; tipoLeitura: TipoLeituraCodigo }) {
  const { data, isLoading } = useTentativas(alunoId, tipoLeitura)

  if (isLoading) return <p>Carregando tentativas…</p>
  if (!data || data.length === 0) return <p>Nenhuma tentativa registrada.</p>

  const ciclos = agruparTentativas(data)

  return (
    <section aria-label="Comparação de tentativas">
      <h3>Evolução por tentativa</h3>
      <table>
        <thead>
          <tr>
            <th>Ciclo</th>
            <th>Avaliação</th>
            <th>Tentativa</th>
            <th>Data</th>
            <th>Corretas</th>
            <th>% acerto</th>
            <th>Variação</th>
          </tr>
        </thead>
        <tbody>
          {ciclos.flatMap(({ ciclo, cadeias }) =>
            cadeias.flatMap((tentativas) => tentativas.map((t, i) => {
              const delta = i === 0 ? null : t.quantidadeCorretas - tentativas[i - 1].quantidadeCorretas
              return (
                <tr key={t.avaliacaoId}>
                  <td>{ROTULOS[ciclo]}</td>
                  <td>{t.nomeAvaliacao ?? '—'}</td>
                  <td>{t.numero}ª</td>
                  <td>{t.dataAvaliacao}</td>
                  <td>{t.quantidadeCorretas}</td>
                  <td>{t.percentualAcerto}%</td>
                  <td>{delta === null ? '—' : delta > 0 ? `▲ +${delta}` : delta < 0 ? `▼ ${delta}` : '0'}</td>
                </tr>
              )
            })),
          )}
        </tbody>
      </table>
      
      <div style={{ display: 'grid', gap: 16, gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))' }}>
        {ciclos.map((c) => (
          <TentativasChart key={c.ciclo} ciclos={[c]} />
        ))}
      </div>
    </section>
  )
}
