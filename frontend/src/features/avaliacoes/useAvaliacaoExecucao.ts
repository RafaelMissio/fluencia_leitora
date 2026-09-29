import { useEffect, useReducer, useRef } from 'react'
import { request } from '../../api/client'
import type { AvaliacaoResponse, PalavraAvaliacao, StatusAvaliacao } from '../../api/types'
import { createRecorder, isMediaRecorderSupported, pickSupportedMimeType, type RecorderHandle } from '../../media/recorder'

/** Edge case do spec.md: navegador sem `MediaRecorder` (FE-25). */
const MENSAGEM_SEM_SUPORTE = 'Navegador sem suporte à gravação. Use Chrome, Edge, Firefox ou Safari 17+'
/** spec.md P1 "Executar avaliação", AC2 (FE-12). */
const MENSAGEM_MICROFONE_NEGADO = 'Permita o acesso ao microfone para iniciar a avaliação'

/** Intervalo do "tick" local do cronômetro (design.md: `performance.now()`, ressincronizado a cada resposta do servidor). */
const INTERVALO_TICK_MS = 250

interface State {
  carregado: boolean
  status: StatusAvaliacao | null
  tempoConfiguradoSegundos: number
  tempoRestanteMs: number
  gravando: boolean
  erroMicrofone: string | null
  /** FE-24: a avaliação já chegou `EM_ANDAMENTO` do servidor no mount (página recarregada). */
  interrompida: boolean
  palavras: PalavraAvaliacao[]
}

const initialState: State = {
  carregado: false,
  status: null,
  tempoConfiguradoSegundos: 0,
  tempoRestanteMs: 0,
  gravando: false,
  erroMicrofone: null,
  interrompida: false,
  palavras: [],
}

type Action =
  | { type: 'AVALIACAO_CARREGADA'; avaliacao: AvaliacaoResponse }
  | { type: 'ERRO_MICROFONE'; mensagem: string }
  | { type: 'TRANSICAO_OK'; avaliacao: AvaliacaoResponse; gravando: boolean; tempoRestanteMs?: number }
  | { type: 'TICK'; tempoRestanteMs: number }

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'AVALIACAO_CARREGADA': {
      const avaliacao = action.avaliacao
      return {
        ...state,
        carregado: true,
        status: avaliacao.status,
        tempoConfiguradoSegundos: avaliacao.tempoConfiguradoSegundos,
        tempoRestanteMs: avaliacao.tempoConfiguradoSegundos * 1000,
        gravando: false,
        palavras: avaliacao.palavras,
        interrompida: avaliacao.status === 'EM_ANDAMENTO',
      }
    }
    case 'ERRO_MICROFONE':
      return { ...state, erroMicrofone: action.mensagem }
    case 'TRANSICAO_OK': {
      const avaliacao = action.avaliacao
      return {
        ...state,
        status: avaliacao.status,
        gravando: action.gravando,
        erroMicrofone: null,
        interrompida: false,
        tempoConfiguradoSegundos: avaliacao.tempoConfiguradoSegundos,
        tempoRestanteMs: action.tempoRestanteMs ?? state.tempoRestanteMs,
        palavras: avaliacao.palavras,
      }
    }
    case 'TICK':
      return { ...state, tempoRestanteMs: action.tempoRestanteMs }
    default:
      return state
  }
}

/**
 * Núcleo do fluxo do professor (design.md, Components): orquestra
 * `getUserMedia`, `MediaRecorder` (via `media/recorder.ts`) e o cronômetro
 * local, ressincronizados com as respostas do servidor. T16 cobre o
 * carregamento inicial e `iniciar()` (FE-11, FE-12, FE-13, FE-24, FE-25);
 * T17 adiciona `pausar`/`continuar`/`resetar`; `finalizar` e o resync de 409
 * chegam em T18.
 */
export function useAvaliacaoExecucao(avaliacaoId: number) {
  const [state, dispatch] = useReducer(reducer, initialState)

  const recorderRef = useRef<RecorderHandle | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const inicioContagemRef = useRef<number>(0)
  const tempoRestanteNoInicioRef = useRef<number>(0)

  useEffect(() => {
    let cancelado = false
    request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}`).then((avaliacao) => {
      if (!cancelado) {
        dispatch({ type: 'AVALIACAO_CARREGADA', avaliacao })
      }
    })
    return () => {
      cancelado = true
    }
  }, [avaliacaoId])

  function pararContagem(): void {
    if (intervalRef.current !== null) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
  }

  function iniciarContagem(tempoRestanteMsAtual: number): void {
    pararContagem()
    inicioContagemRef.current = performance.now()
    tempoRestanteNoInicioRef.current = tempoRestanteMsAtual
    intervalRef.current = setInterval(() => {
      const decorrido = performance.now() - inicioContagemRef.current
      const restante = Math.max(0, tempoRestanteNoInicioRef.current - decorrido)
      dispatch({ type: 'TICK', tempoRestanteMs: restante })
    }, INTERVALO_TICK_MS)
  }

  // Para o intervalo local e libera o stream do microfone ao desmontar a tela.
  useEffect(() => {
    return () => {
      pararContagem()
      streamRef.current?.getTracks().forEach((track) => track.stop())
    }
  }, [])

  /**
   * FE-11/FE-12: pede o microfone ANTES de chamar a API `iniciar` - se
   * negado/indisponível, `iniciar` nunca é chamado e a avaliação continua
   * `CRIADA`. FE-13: gravação e cronômetro começam juntos ao suceder.
   */
  async function iniciar(): Promise<void> {
    if (!isMediaRecorderSupported()) {
      dispatch({ type: 'ERRO_MICROFONE', mensagem: MENSAGEM_SEM_SUPORTE })
      return
    }

    let stream: MediaStream
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    } catch {
      dispatch({ type: 'ERRO_MICROFONE', mensagem: MENSAGEM_MICROFONE_NEGADO })
      return
    }

    const avaliacao = await request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}/iniciar`, { method: 'POST' })

    streamRef.current = stream
    const mimeType = pickSupportedMimeType() ?? 'audio/webm'
    const recorder = createRecorder(stream, mimeType)
    recorderRef.current = recorder
    recorder.start()

    dispatch({ type: 'TRANSICAO_OK', avaliacao, gravando: true })
    iniciarContagem(avaliacao.tempoConfiguradoSegundos * 1000)
  }

  /** spec.md P1 "Executar avaliação", AC4: pausa o cronômetro e a gravação junto com a API. */
  async function pausar(): Promise<void> {
    const avaliacao = await request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}/pausar`, { method: 'POST' })
    pararContagem()
    recorderRef.current?.pause()
    dispatch({ type: 'TRANSICAO_OK', avaliacao, gravando: false })
  }

  /** spec.md P1 "Executar avaliação", AC4: caminho inverso de `pausar`, retomando de onde parou. */
  async function continuar(): Promise<void> {
    const avaliacao = await request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}/continuar`, { method: 'POST' })
    recorderRef.current?.resume()
    dispatch({ type: 'TRANSICAO_OK', avaliacao, gravando: true })
    iniciarContagem(state.tempoRestanteMs)
  }

  /**
   * spec.md P1 "Executar avaliação", AC5: só age com `confirmado === true` (a
   * confirmação em si é responsabilidade da UI que chama o hook). Descarta a
   * gravação em andamento (sem usar o `Blob`) e zera o cronômetro para o
   * tempo configurado; as palavras voltam a `PENDENTE` porque o próprio
   * servidor já as devolve assim (`AvaliacaoService.resetar`).
   */
  async function resetar(confirmado: boolean): Promise<void> {
    if (!confirmado) return

    const avaliacao = await request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}/resetar`, { method: 'POST' })
    pararContagem()
    if (recorderRef.current) {
      recorderRef.current.stop().catch(() => undefined)
      recorderRef.current = null
    }
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null

    dispatch({
      type: 'TRANSICAO_OK',
      avaliacao,
      gravando: false,
      tempoRestanteMs: avaliacao.tempoConfiguradoSegundos * 1000,
    })
  }

  return {
    status: state.status,
    tempoRestanteMs: state.tempoRestanteMs,
    gravando: state.gravando,
    erroMicrofone: state.erroMicrofone,
    interrompida: state.interrompida,
    palavras: state.palavras,
    iniciar,
    pausar,
    continuar,
    resetar,
  }
}
