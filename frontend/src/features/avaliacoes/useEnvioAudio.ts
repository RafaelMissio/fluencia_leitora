import { useEffect, useState } from 'react'
import { uploadAudio } from '../../api/client'

export type StatusEnvioAudio = 'idle' | 'enviando' | 'enviado' | 'falhou'

/** spec.md P1 "Resultado, áudio e envio", AC2/Assumptions: esperas de 1s, 2s e 4s entre as 3 tentativas. */
const ESPERAS_MS = [1000, 2000, 4000]
const MAX_TENTATIVAS = ESPERAS_MS.length

function esperar(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Envio automático do áudio após finalizar (design.md, Components), com até
 * 3 tentativas (1s/2s/4s - FE-20); em falha total, mantém o `Blob` em
 * memória, expõe `reenviar()` e bloqueia `beforeunload` enquanto o áudio não
 * foi enviado (FE-21).
 */
export function useEnvioAudio(
  avaliacaoId: number,
  blob: Blob | null,
): { status: StatusEnvioAudio; tentativas: number; reenviar: () => Promise<void> } {
  const [status, setStatus] = useState<StatusEnvioAudio>('idle')
  const [tentativas, setTentativas] = useState(0)

  async function enviar(blobParaEnviar: Blob): Promise<void> {
    setStatus('enviando')
    for (let tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
      setTentativas(tentativa)
      try {
        await uploadAudio(avaliacaoId, blobParaEnviar, blobParaEnviar.type || 'audio/webm')
        setStatus('enviado')
        return
      } catch {
        if (tentativa < MAX_TENTATIVAS) {
          await esperar(ESPERAS_MS[tentativa - 1])
        }
      }
    }
    setStatus('falhou')
  }

  useEffect(() => {
    if (!blob) return
    let cancelado = false
    // Dispara `enviar` como um callback assíncrono (não sincronamente no corpo
    // do efeito), que é o padrão que dispara side effects externos a partir
    // de um efeito (react.dev: "calling setState in a callback function when
    // external state changes").
    Promise.resolve().then(() => {
      if (!cancelado) void enviar(blob)
    })
    return () => {
      cancelado = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blob])

  // FE-21: enquanto o áudio ainda não foi enviado, pede confirmação ao sair da página.
  useEffect(() => {
    function aoSair(event: BeforeUnloadEvent): void {
      if (status !== 'enviado') {
        event.preventDefault()
        event.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', aoSair)
    return () => window.removeEventListener('beforeunload', aoSair)
  }, [status])

  async function reenviar(): Promise<void> {
    if (!blob) return
    await enviar(blob)
  }

  return { status, tentativas, reenviar }
}
