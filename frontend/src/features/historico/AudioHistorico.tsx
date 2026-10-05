import { useEffect, useState } from 'react'
import { baixarAudio } from '../../api/client'

/**
 * Áudio gravado de uma avaliação do histórico (ativa ou inativa): mantém o
 * ícone ▶ e, ao clicar, busca o áudio sob demanda e mostra player + download.
 */
export function AudioHistorico({ avaliacaoId, dataAvaliacao }: { avaliacaoId: number; dataAvaliacao: string }) {
  const [pedido, setPedido] = useState(false)
  const [url, setUrl] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    if (!pedido) return
    let objectUrl: string | null = null
    let cancelado = false
    baixarAudio(avaliacaoId).then(
      (blob) => {
        if (cancelado) return
        objectUrl = URL.createObjectURL(blob)
        setUrl(objectUrl)
      },
      (e: Error) => {
        if (!cancelado) setErro(e.message)
      },
    )
    return () => {
      cancelado = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [pedido, avaliacaoId])

  if (url) {
    return (
      <span>
        <audio controls autoPlay src={url} />{' '}
        <a href={url} download={`avaliacao-${avaliacaoId}.audio`}>
          Baixar
        </a>
      </span>
    )
  }
  return (
    <>
      <button type="button" disabled={pedido && !erro} onClick={() => { setErro(null); setPedido(true) }}>
        <span role="img" aria-label={`Avaliação de ${dataAvaliacao} tem áudio`}>
          ▶
        </span>
      </button>
      {erro ? <span role="alert"> {erro}</span> : null}
    </>
  )
}
