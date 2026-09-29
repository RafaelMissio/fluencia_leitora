import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { RoleGate } from '../../app/router'
import { ComparacaoAnualTab } from './ComparacaoAnualTab'
import { EvolucaoCiclosTab } from './EvolucaoCiclosTab'
import { HistoricoTab } from './HistoricoTab'

type Aba = 'historico' | 'evolucao' | 'comparacao'

/**
 * 3 abas (spec.md P1 "Histórico e evolução", FE-23; design.md, Components):
 * "Histórico" (T25) sempre visível para PROFESSOR e COORDENADOR; "Evolução"
 * (T26) e "Comparação anual" (T27) só renderizam para COORDENADOR, via
 * `RoleGate` (context.md, "Acesso a evolução" - `evolucao-ciclos`/
 * `evolucao-anos` são `hasRole('COORDENADOR')`, PROFESSOR recebe 403).
 *
 * Ponto crítico de correção: o gate acontece no RENDER, não como
 * tratamento de erro pós-chamada - os botões das 2 abas COORDENADOR-only e
 * o conteúdo delas ficam dentro de `RoleGate`, então `EvolucaoCiclosTab`/
 * `ComparacaoAnualTab` (e os hooks que eles chamam) nunca montam para
 * PROFESSOR. Não é um 403 na tela - a aba simplesmente não existe para ele.
 */
export function HistoricoPage() {
  const { id } = useParams<{ id: string }>()
  const alunoId = Number(id)
  const [aba, setAba] = useState<Aba>('historico')

  return (
    <div>
      <h1>Histórico e evolução</h1>
      <nav aria-label="Abas de histórico">
        <button type="button" aria-pressed={aba === 'historico'} onClick={() => setAba('historico')}>
          Histórico
        </button>
        <RoleGate allow={['COORDENADOR']}>
          <button type="button" aria-pressed={aba === 'evolucao'} onClick={() => setAba('evolucao')}>
            Evolução
          </button>
          <button type="button" aria-pressed={aba === 'comparacao'} onClick={() => setAba('comparacao')}>
            Comparação anual
          </button>
        </RoleGate>
      </nav>

      {aba === 'historico' ? <HistoricoTab alunoId={alunoId} /> : null}
      <RoleGate allow={['COORDENADOR']}>
        {aba === 'evolucao' ? <EvolucaoCiclosTab alunoId={alunoId} /> : null}
        {aba === 'comparacao' ? <ComparacaoAnualTab alunoId={alunoId} /> : null}
      </RoleGate>
    </div>
  )
}
