import { useState, type CSSProperties, type KeyboardEvent } from 'react'
import type { PalavraAvaliacao, StatusAvaliacao, StatusPalavra } from '../api/types'

export interface GradePalavrasProps {
  palavras: PalavraAvaliacao[]
  avaliacaoStatus: StatusAvaliacao
  onMarcar: (ordem: number, novoStatus: StatusPalavra) => Promise<void>
  /** T23 (composição) usa isto para re-buscar o resultado exibido após marcar numa avaliação FINALIZADA (AC5). */
  onAposMarcarFinalizada?: () => void
}

/** spec.md, Assumptions: ciclo de toque PENDENTE → CORRETA → INCORRETA → NAO_LIDA → CORRETA. */
const PROXIMO_STATUS: Record<StatusPalavra, StatusPalavra> = {
  PENDENTE: 'CORRETA',
  CORRETA: 'INCORRETA',
  INCORRETA: 'NAO_LIDA',
  NAO_LIDA: 'CORRETA',
}

/**
 * spec.md P1 "Marcar palavras", AC2: cor + ícone + texto (nunca só cor,
 * WCAG 1.4.1) com contraste mínimo de 4.5:1 sobre fundo branco. As cores
 * escuras abaixo (`#1B5E20`, `#B71C1C`, `#616161`) foram escolhidas para
 * ultrapassar 4.5:1 (checado em `GradePalavras.test.tsx`, não "a olho").
 */
const CONFIG_STATUS: Record<StatusPalavra, { cor: string; icone: string; rotulo: string; borda: string }> = {
  PENDENTE: { cor: '#616161', icone: '', rotulo: 'Pendente', borda: '1px dashed #616161' },
  CORRETA: { cor: '#1B5E20', icone: '✓', rotulo: 'Correta', borda: '1px solid #1B5E20' },
  INCORRETA: { cor: '#B71C1C', icone: '✗', rotulo: 'Incorreta', borda: '1px solid #B71C1C' },
  NAO_LIDA: { cor: '#616161', icone: '–', rotulo: 'Não lida', borda: '1px solid #616161' },
}

/** spec.md P1 "Marcar palavras", AC1/AC4: só permite marcar nesses 3 status. */
const STATUS_QUE_PERMITEM_MARCACAO: StatusAvaliacao[] = ['EM_ANDAMENTO', 'PAUSADA', 'FINALIZADA']

const MENSAGEM_FALHA = 'Não foi possível salvar a marcação'
const MENSAGEM_AUDITORIA = 'Alteração registrada em auditoria'

/**
 * Grade das palavras da avaliação (design.md, Components): toque/atalho de
 * teclado avança o status em ciclo, atualiza a cor otimisticamente e chama
 * `onMarcar`; reverte e avisa em falha (AC3); mostra o aviso de auditoria ao
 * marcar numa avaliação FINALIZADA (AC5).
 */
export function GradePalavras({ palavras, avaliacaoStatus, onMarcar, onAposMarcarFinalizada }: GradePalavrasProps) {
  const [local, setLocal] = useState<PalavraAvaliacao[]>(palavras)
  const [erro, setErro] = useState<string | null>(null)
  const [avisoAuditoria, setAvisoAuditoria] = useState<string | null>(null)
  // "Adjusting state when a prop changes" (react.dev): sincroniza `local` com
  // uma atualização externa de `palavras` (ex.: resetar, resync de 409) sem
  // usar useEffect - feito durante a renderização, não depois dela.
  const [ultimasPalavrasProp, setUltimasPalavrasProp] = useState(palavras)
  if (palavras !== ultimasPalavrasProp) {
    setUltimasPalavrasProp(palavras)
    setLocal(palavras)
  }

  const desabilitado = !STATUS_QUE_PERMITEM_MARCACAO.includes(avaliacaoStatus)

  async function marcar(ordem: number, novoStatusForcado?: StatusPalavra): Promise<void> {
    if (desabilitado) return
    const atual = local.find((palavra) => palavra.ordem === ordem)
    if (!atual) return

    const statusAnterior = atual.status
    const novoStatus = novoStatusForcado ?? PROXIMO_STATUS[statusAnterior]

    setErro(null)
    setLocal((prev) => prev.map((palavra) => (palavra.ordem === ordem ? { ...palavra, status: novoStatus } : palavra)))

    try {
      await onMarcar(ordem, novoStatus)
      if (avaliacaoStatus === 'FINALIZADA') {
        setAvisoAuditoria(MENSAGEM_AUDITORIA)
        onAposMarcarFinalizada?.()
      }
    } catch {
      setLocal((prev) => prev.map((palavra) => (palavra.ordem === ordem ? { ...palavra, status: statusAnterior } : palavra)))
      setErro(MENSAGEM_FALHA)
    }
  }

  function aoTeclar(ordem: number) {
    return (event: KeyboardEvent<HTMLButtonElement>) => {
      const tecla = event.key.toLowerCase()
      if (tecla === 'c') {
        event.preventDefault()
        void marcar(ordem, 'CORRETA')
      } else if (tecla === 'i') {
        event.preventDefault()
        void marcar(ordem, 'INCORRETA')
      } else if (tecla === 'n') {
        event.preventDefault()
        void marcar(ordem, 'NAO_LIDA')
      }
    }
  }

  return (
    <div>
      {erro && <span role="alert">{erro}</span>}
      {avisoAuditoria && <span role="status">{avisoAuditoria}</span>}
      <ul>
        {local.map((palavra) => {
          const config = CONFIG_STATUS[palavra.status]
          const estilo: CSSProperties = { color: config.cor, border: config.borda }
          return (
            <li key={palavra.ordem}>
              <button
                type="button"
                disabled={desabilitado}
                onClick={() => void marcar(palavra.ordem)}
                onKeyDown={aoTeclar(palavra.ordem)}
                aria-label={`${palavra.palavra}: ${config.rotulo}`}
                style={estilo}
              >
                <span aria-hidden="true">{config.icone}</span> {palavra.palavra}
              </button>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
