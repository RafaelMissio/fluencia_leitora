import { useEffect, useReducer, useRef } from 'react'
import { request } from '../../api/client'
import type { ApiError, AvaliacaoResponse, PalavraAvaliacao, StatusAvaliacao } from '../../api/types'
import { createRecorder, isMediaRecorderSupported, pickSupportedMimeType, type RecorderHandle } from '../../media/recorder'

/** spec.md P1 "Executar avaliação", AC7: botões habilitados por status. */
export type BotaoExecucao = 'iniciar' | 'pausar' | 'continuar' | 'resetar' | 'finalizar'

function botoesParaStatus(status: StatusAvaliacao | null): BotaoExecucao[] {
  switch (status) {
    case 'CRIADA':
      return ['iniciar']
    case 'EM_ANDAMENTO':
      return ['pausar', 'resetar', 'finalizar']
    case 'PAUSADA':
      return ['continuar', 'resetar', 'finalizar']
    case 'FINALIZADA':
    case 'CANCELADA':
    case null:
      return []
    default:
      return []
  }
}

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
 * T17 adiciona `pausar`/`continuar`/`resetar`; T18 adiciona `finalizar`, os
 * `botoesHabilitados` por status (FE-14) e o resync silencioso em qualquer
 * 409 `TRANSICAO_INVALIDA` (FE-15).
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
   * FE-15: chama uma transição e, se o servidor responder 409
   * `TRANSICAO_INVALIDA`, recarrega a avaliação e resincroniza o estado
   * silenciosamente (sem lançar erro visível) em vez de propagar o erro.
   * Retorna `null` quando resincronizou - o chamador deve pular seus efeitos
   * locais de sucesso (recorder/cronômetro) nesse caso.
   */
  async function chamarTransicao(path: string): Promise<AvaliacaoResponse | null> {
    try {
      return await request<AvaliacaoResponse>(path, { method: 'POST' })
    } catch (erro) {
      const apiError = erro as ApiError
      if (apiError.status === 409 && apiError.code === 'TRANSICAO_INVALIDA') {
        const avaliacao = await request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}`)
        dispatch({ type: 'AVALIACAO_CARREGADA', avaliacao })
        return null
      }
      throw erro
    }
  }

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

    const avaliacao = await chamarTransicao(`/avaliacoes/${avaliacaoId}/iniciar`)
    if (!avaliacao) {
      stream.getTracks().forEach((track) => track.stop())
      return
    }

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
    const avaliacao = await chamarTransicao(`/avaliacoes/${avaliacaoId}/pausar`)
    if (!avaliacao) return
    pararContagem()
    recorderRef.current?.pause()
    dispatch({ type: 'TRANSICAO_OK', avaliacao, gravando: false })
  }

  /** spec.md P1 "Executar avaliação", AC4: caminho inverso de `pausar`, retomando de onde parou. */
  async function continuar(): Promise<void> {
    const avaliacao = await chamarTransicao(`/avaliacoes/${avaliacaoId}/continuar`)
    if (!avaliacao) return
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

    const avaliacao = await chamarTransicao(`/avaliacoes/${avaliacaoId}/resetar`)
    if (!avaliacao) return
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

  /**
   * spec.md P1 "Executar avaliação", AC6: para a gravação (`stop()`
   * consumindo o `Blob` desta vez - ao contrário de `resetar`), chama
   * `POST .../finalizar` e retorna o `Blob` gravado para quem chamou (a
   * composição da tela, T23) encaminhar ao envio de áudio.
   *
   * SPEC_DEVIATION: o `AvaliacaoController` real (`POST
   * /avaliacoes/{id}/finalizar`) não tem `@RequestBody` - o servidor decide
   * sozinho se o motivo foi tempo esgotado (`AvaliacaoService.
   * finalizarSeTempoEsgotado`, chamado antes de qualquer transição). `motivo`
   * fica só como intenção do chamador (dispara automaticamente ao chegar a
   * 00:00 vs. clique manual); não é enviado no corpo da requisição.
   */
  async function finalizar(motivo?: 'TEMPO_ESGOTADO'): Promise<Blob | null> {
    void motivo
    pararContagem()
    let blob: Blob | null = null
    if (recorderRef.current) {
      blob = await recorderRef.current.stop()
      recorderRef.current = null
    }
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null

    const avaliacao = await chamarTransicao(`/avaliacoes/${avaliacaoId}/finalizar`)
    if (avaliacao) {
      dispatch({ type: 'TRANSICAO_OK', avaliacao, gravando: false })
    }
    return blob
  }

  // spec.md P1 "Executar avaliação", AC6: o cronômetro chegando a 00:00 finaliza automaticamente.
  useEffect(() => {
    if (state.status === 'EM_ANDAMENTO' && state.tempoRestanteMs <= 0) {
      finalizar('TEMPO_ESGOTADO')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.status, state.tempoRestanteMs])

  return {
    status: state.status,
    tempoRestanteMs: state.tempoRestanteMs,
    gravando: state.gravando,
    erroMicrofone: state.erroMicrofone,
    interrompida: state.interrompida,
    palavras: state.palavras,
    botoesHabilitados: botoesParaStatus(state.status),
    iniciar,
    pausar,
    continuar,
    resetar,
    finalizar,
  }
}
