import { useState } from 'react'
import type { ResultadoCiclo, TipoLeituraCodigo } from '../../api/types'
import { useEvolucaoCiclos } from './useEvolucaoCiclos'

const TIPOS_LEITURA: TipoLeituraCodigo[] = ['PALAVRA', 'PSEUDOPALAVRA', 'TEXTO_CURTO']

interface EvolucaoValorLocal {
  absoluta: number | null
  percentual: number | null
}

/**
 * `EvolucaoCiclosResponse` (backend) não traz uma evolução pronta por ciclo -
 * só corretas/classificação por ciclo (HIST-07..11). Calculada aqui no
 * cliente comparando cada ciclo com o ciclo anterior da mesma resposta
 * (Entrada não tem anterior -> sempre nula), com a mesma fórmula do backend
 * (`HistoricoEvolucaoService.evolucao`, HIST-14/17-19): null/null quando
 * falta um dos dois lados; 0/0 quando os dois são zero; percentual null
 * quando o anterior é zero e o atual é maior que zero.
 */
function calcularEvolucao(atual: ResultadoCiclo | null, anterior: ResultadoCiclo | null): EvolucaoValorLocal {
  if (!atual || !anterior) return { absoluta: null, percentual: null }
  const absoluta = atual.quantidadeCorretas - anterior.quantidadeCorretas
  let percentual: number | null
  if (anterior.quantidadeCorretas === 0) {
    percentual = atual.quantidadeCorretas === 0 ? 0 : null
  } else {
    percentual = Number(((absoluta * 100) / anterior.quantidadeCorretas).toFixed(2))
  }
  return { absoluta, percentual }
}

/**
 * spec.md P1 "Histórico e evolução", AC2: ▲ verde para positivo, ▼ vermelho
 * para negativo, "—" para nulo. AC2 não define o caso `absoluta === 0`
 * (spec-precision gap) - tratado como neutro ("0", sem seta).
 */
function IndicadorEvolucao({ evolucao }: { evolucao: EvolucaoValorLocal }) {
  if (evolucao.absoluta === null) {
    return <span>—</span>
  }
  if (evolucao.absoluta > 0) {
    return <span style={{ color: '#1B5E20' }}>▲ +{evolucao.absoluta}</span>
  }
  if (evolucao.absoluta < 0) {
    return <span style={{ color: '#B71C1C' }}>▼ {evolucao.absoluta}</span>
  }
  return <span>0</span>
}

/**
 * Aba "Evolução por ciclo" (spec.md P1 "Histórico e evolução", AC2) -
 * COORDENADOR only (`evolucao-ciclos` é `hasRole('COORDENADOR')`); a página
 * `HistoricoPage` (T28) só monta este componente dentro de um `RoleGate`
 * COORDENADOR (context.md, "Acesso a evolução").
 */
export function EvolucaoCiclosTab({ alunoId }: { alunoId: number }) {
  const [tipoLeitura, setTipoLeitura] = useState<TipoLeituraCodigo>('PALAVRA')
  const { data, isLoading } = useEvolucaoCiclos(alunoId, undefined, tipoLeitura)

  const ciclos: { rotulo: string; atual: ResultadoCiclo | null; anterior: ResultadoCiclo | null }[] = data
    ? [
        { rotulo: 'Entrada', atual: data.entrada, anterior: null },
        { rotulo: 'Acompanhamento', atual: data.acompanhamento, anterior: data.entrada },
        { rotulo: 'Saída', atual: data.saida, anterior: data.acompanhamento },
      ]
    : []

  return (
    <section aria-label="Evolução por ciclo">
      <label htmlFor="evolucao-ciclos-tipo-leitura">Tipo de leitura</label>
      <select
        id="evolucao-ciclos-tipo-leitura"
        value={tipoLeitura}
        onChange={(event) => setTipoLeitura(event.target.value as TipoLeituraCodigo)}
      >
        {TIPOS_LEITURA.map((tipo) => (
          <option key={tipo} value={tipo}>
            {tipo}
          </option>
        ))}
      </select>

      {isLoading ? (
        <p>Carregando evolução…</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Ciclo</th>
              <th>Corretas</th>
              <th>Classificação</th>
              <th>Evolução</th>
            </tr>
          </thead>
          <tbody>
            {ciclos.map(({ rotulo, atual, anterior }) => (
              <tr key={rotulo}>
                <td>{rotulo}</td>
                <td>{atual ? atual.quantidadeCorretas : '—'}</td>
                <td>{atual ? (atual.fase ?? '—') : '—'}</td>
                <td>
                  <IndicadorEvolucao evolucao={calcularEvolucao(atual, anterior)} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
