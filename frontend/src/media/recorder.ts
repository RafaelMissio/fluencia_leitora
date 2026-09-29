/**
 * Wrapper fino sobre `MediaRecorder` (design.md, Components: `media/recorder.ts`).
 * Isola `window.MediaRecorder` do resto da app para que `useAvaliacaoExecucao`
 * seja testável com um fake, e resolve a ordem de MIME do spec.md (Edge Cases).
 */

/** Ordem de preferência do spec.md (Edge Cases): o primeiro suportado por `MediaRecorder.isTypeSupported` vence. */
const MIME_TYPES_EM_ORDEM = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/mp4'] as const

/** FE-25: `false` quando o navegador não expõe `MediaRecorder`. */
export function isMediaRecorderSupported(): boolean {
  return typeof window !== 'undefined' && typeof window.MediaRecorder !== 'undefined'
}

/** Primeiro MIME suportado na ordem do spec, ou `null` se nenhum for (FE-25). */
export function pickSupportedMimeType(): string | null {
  if (!isMediaRecorderSupported()) return null
  for (const mimeType of MIME_TYPES_EM_ORDEM) {
    if (MediaRecorder.isTypeSupported(mimeType)) return mimeType
  }
  return null
}

export interface RecorderHandle {
  start(): void
  pause(): void
  resume(): void
  /** Para a gravação e resolve com o `Blob` acumulado, no `mimeType` escolhido. */
  stop(): Promise<Blob>
}

/** Cria o wrapper sobre um `MediaStream` já liberado pelo usuário (FE-11). */
export function createRecorder(stream: MediaStream, mimeType: string): RecorderHandle {
  const mediaRecorder = new MediaRecorder(stream, { mimeType })
  const chunks: Blob[] = []

  mediaRecorder.ondataavailable = (event: BlobEvent) => {
    if (event.data.size > 0) {
      chunks.push(event.data)
    }
  }

  return {
    start() {
      mediaRecorder.start()
    },
    pause() {
      mediaRecorder.pause()
    },
    resume() {
      mediaRecorder.resume()
    },
    stop() {
      return new Promise<Blob>((resolve) => {
        mediaRecorder.onstop = () => {
          resolve(new Blob(chunks, { type: mimeType }))
        }
        mediaRecorder.stop()
      })
    },
  }
}
