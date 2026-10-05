import { useEffect, useRef, useState } from 'react'
import type { PalavraAvaliacao, StatusAvaliacao, StatusPalavra } from '../api/types'

export interface PalavraPorVezProps {
  palavras: PalavraAvaliacao[]
  avaliacaoStatus: StatusAvaliacao
  onMarcar: (ordem: number, novoStatus: StatusPalavra) => Promise<void>
}

const ATRASO_PROXIMA_MS = 700
const TEMPO_SILENCIO_MS = 10_000

interface ReconhecimentoFala {
  lang: string
  continuous: boolean
  interimResults: boolean
  maxAlternatives: number
  onresult: ((e: { results: ArrayLike<ArrayLike<{ transcript: string }> & { isFinal: boolean }>; resultIndex: number }) => void) | null
  onend: (() => void) | null
  onerror: (() => void) | null
  start(): void
  abort(): void
}

function criarReconhecimento(): ReconhecimentoFala | null {
  const w = window as unknown as { SpeechRecognition?: new () => ReconhecimentoFala; webkitSpeechRecognition?: new () => ReconhecimentoFala }
  const Ctor = w.SpeechRecognition ?? w.webkitSpeechRecognition
  return Ctor ? new Ctor() : null
}

/** Minúsculas, sem acentos nem pontuação, para comparar a fala reconhecida com a palavra esperada. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9\s]/g, '').trim()
}

/** Correta se alguma palavra falada coincide com a esperada. */
function leuCorretamente(esperada: string, falado: string): boolean {
  const alvo = normalizar(esperada)
  return normalizar(falado).split(/\s+/).includes(alvo) || normalizar(falado) === alvo
}

/** Mostra uma palavra por vez (modo "refazer"): verde se lida corretamente, vermelha se errada, e avança sozinha. */
export function PalavraPorVez({ palavras, avaliacaoStatus, onMarcar }: PalavraPorVezProps) {
  const [indice, setIndice] = useState(() => Math.max(0, palavras.findIndex((p) => p.status === 'PENDENTE')))
  const [marcada, setMarcada] = useState<StatusPalavra | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    if (marcada === null) return
    const timer = setTimeout(() => {
      setMarcada(null)
      setIndice((i) => i + 1)
    }, ATRASO_PROXIMA_MS)
    return () => clearTimeout(timer)
  }, [marcada])

  const ativa = avaliacaoStatus === 'EM_ANDAMENTO'
  const atual = palavras[indice]
  const suportaFala = typeof window !== 'undefined' && criarReconhecimento() !== null
  const marcarRef = useRef<(status: StatusPalavra) => Promise<void>>(async () => {})

  useEffect(() => {
    marcarRef.current = marcar
  })

  // Silêncio: sem resposta em 10s, a palavra é marcada como não lida e a tela avança.
  useEffect(() => {
    if (!ativa || marcada !== null || !atual) return
    const timer = setTimeout(() => void marcarRef.current('NAO_LIDA'), TEMPO_SILENCIO_MS)
    return () => clearTimeout(timer)
  }, [ativa, marcada, atual])

  // Correção automática: reconhece a fala do aluno e compara com a palavra atual.
  useEffect(() => {
    if (!ativa || marcada !== null || !atual) return
    const rec = criarReconhecimento()
    if (!rec) return
    let encerrado = false
    rec.lang = 'pt-BR'
    rec.continuous = false
    rec.interimResults = false
    rec.maxAlternatives = 5
    rec.onresult = (e) => {
      const resultado = e.results[e.resultIndex]
      const alternativas = Array.from(resultado, (a) => a.transcript)
      encerrado = true
      void marcarRef.current(alternativas.some((t) => leuCorretamente(atual.palavra, t)) ? 'CORRETA' : 'INCORRETA')
    }
    const reiniciar = () => {
      if (encerrado) return
      try { rec.start() } catch { /* já iniciado */ }
    }
    rec.onend = reiniciar
    rec.onerror = () => {}
    reiniciar()
    return () => {
      encerrado = true
      rec.onend = null
      rec.abort()
    }
  }, [ativa, marcada, atual])

  if (palavras.length === 0) return null
  if (!atual) return <p role="status">Todas as palavras foram lidas.</p>

  async function marcar(status: StatusPalavra): Promise<void> {
    if (!ativa || marcada !== null) return
    setErro(null)
    try {
      await onMarcar(atual.ordem, status)
      setMarcada(status)
    } catch {
      setErro('Não foi possível salvar a marcação')
    }
  }

  const classe = marcada === 'CORRETA' ? 'correta' : marcada === 'INCORRETA' ? 'incorreta' : marcada === 'NAO_LIDA' ? 'nao-lida' : ''

  return (
    <div className="palavra-por-vez">
      {erro && <span role="alert">{erro}</span>}
      <p className="contador">
        Palavra {indice + 1} de {palavras.length}
      </p>
      <p className={`palavra-atual ${classe}`.trim()} aria-live="polite">
        {marcada === 'CORRETA' && <span aria-hidden="true">✓ </span>}
        {marcada === 'INCORRETA' && <span aria-hidden="true">✗ </span>}
        {marcada === 'NAO_LIDA' && <span aria-hidden="true">– </span>}
        {atual.palavra}
      </p>
      {ativa && marcada === null && suportaFala && <p>Ouvindo… sem resposta em 10 s, passa para a próxima palavra.</p>}
      <button type="button" disabled={!ativa || marcada !== null} onClick={() => void marcar('NAO_LIDA')}>
        Próxima palavra
      </button>
      {!suportaFala && (
        <div>
          <p role="note">Este navegador não reconhece fala (use o Chrome ou Edge); marque manualmente.</p>
          <button type="button" disabled={!ativa || marcada !== null} onClick={() => void marcar('CORRETA')}>
            Leu corretamente
          </button>
          <button type="button" disabled={!ativa || marcada !== null} onClick={() => void marcar('INCORRETA')}>
            Leu errado
          </button>
        </div>
      )}
    </div>
  )
}
