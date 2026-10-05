import { useState } from 'react'
import type { CicloAnual, EvolucaoAnualLinha, TipoLeituraCodigo } from '../../api/types'
import { useEvolucaoAnual } from './useEvolucaoAnual'

const TIPOS_LEITURA: TipoLeituraCodigo[] = ['PALAVRA', 'PSEUDOPALAVRA', 'TEXTO_CURTO']

function formatarPercentual(percentual: number): string {
  return `${percentual.toFixed(2).replace('.', ',')}%`
}

function formatarAbsoluta(absoluta: number): string {
  return absoluta > 0 ? `+${absoluta}` : String(absoluta)
}

/**
 * spec.md P1 "Histórico e evolução", AC3: mostra "sem base" quando `semBase`.
 * `CicloAnualResponse` (backend) não tem um campo `semBase` explícito - a
 * task T27 define a derivação: `semBase = evolucao.absoluta === null &&
 * evolucao.percentual === null` (só acontece quando não há ano anterior com
 * esse ciclo para comparar - `HistoricoEvolucaoService.evolucao`). Quando só
 * o `percentual` é `null` (ano anterior com 0 corretas, divisão por zero
 * evitada - HIST-19), isso NÃO é "sem base": o `absoluta` continua válido.
 */
function CelulaCiclo({ cicloAnual }: { cicloAnual: CicloAnual | null }) {
  if (!cicloAnual) {
    return <>—</>
  }

  const { evolucao } = cicloAnual
  const semBase = evolucao.absoluta === null && evolucao.percentual === null
  if (semBase) {
    return <>{cicloAnual.quantidadeCorretas} (sem base)</>
  }

  return (
    <>
      {cicloAnual.quantidadeCorretas} ({formatarAbsoluta(evolucao.absoluta as number)} /{' '}
      {evolucao.percentual !== null ? formatarPercentual(evolucao.percentual) : '—'})
    </>
  )
}

/**
 * Aba "Comparação anual" (spec.md P1 "Histórico e evolução", AC3; tabela do
 * SDD §15) - COORDENADOR only (`evolucao-anos` é `hasRole('COORDENADOR')`);
 * a página `HistoricoPage` (T28) só monta este componente dentro de um
 * `RoleGate` COORDENADOR (context.md, "Acesso a evolução"). Uma linha por
 * ano letivo, na ordem já ascendente devolvida pelo backend (HIST-13, a
 * query ordena por `anoLetivo.ano`).
 */
export function ComparacaoAnualTab({ alunoId }: { alunoId: number }) {
  const [tipoLeitura, setTipoLeitura] = useState<TipoLeituraCodigo>('PALAVRA')
  const { data, isLoading } = useEvolucaoAnual(alunoId, tipoLeitura)

  const anos: EvolucaoAnualLinha[] = data?.anos ?? []

  return (
    <section aria-label="Comparação anual">
      <label htmlFor="comparacao-anual-tipo-leitura">Tipo de leitura</label>
      <select
        id="comparacao-anual-tipo-leitura"
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
        <p>Carregando comparação anual…</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Ano letivo</th>
              <th>Ano/Série</th>
              <th>Turma</th>
              <th>Entrada</th>
              <th>Acompanhamento</th>
              <th>Saída</th>
            </tr>
          </thead>
          <tbody>
            {anos.map((linha) => (
              <tr key={linha.anoLetivo}>
                <td>{linha.anoLetivo}</td>
                <td>{linha.serie}º Ano</td>
                <td>{linha.turma ?? '—'}</td>
                <td>
                  <CelulaCiclo cicloAnual={linha.entrada} />
                </td>
                <td>
                  <CelulaCiclo cicloAnual={linha.acompanhamento} />
                </td>
                <td>
                  <CelulaCiclo cicloAnual={linha.saida} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
