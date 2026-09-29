import type { CSSProperties } from 'react'

export interface CronometroDisplayProps {
  tempoRestanteMs: number
  gravando: boolean
}

/** `mm:ss`, arredondando para cima (65000ms → "01:05", spec.md P1 "Executar avaliação"). */
function formatarTempo(tempoRestanteMs: number): string {
  const totalSegundos = Math.max(0, Math.ceil(tempoRestanteMs / 1000))
  const minutos = Math.floor(totalSegundos / 60)
  const segundos = totalSegundos % 60
  return `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`
}

const pontoStyle: CSSProperties = {
  display: 'inline-block',
  width: 10,
  height: 10,
  marginRight: 6,
  borderRadius: '50%',
  backgroundColor: '#c62828',
  animation: 'cronometro-display-pulso 1.2s ease-in-out infinite',
}

/**
 * Componente puramente visual (design.md, Components): mostra o tempo
 * restante em `mm:ss` e, enquanto `gravando`, o indicador "Gravando" com um
 * ponto vermelho pulsante (spec.md P1 "Executar avaliação", AC8).
 * Rótulos `aria-label` para leitores de tela (SDD Edge Cases).
 */
export function CronometroDisplay({ tempoRestanteMs, gravando }: CronometroDisplayProps) {
  const tempoFormatado = formatarTempo(tempoRestanteMs)

  return (
    <div>
      <style>{'@keyframes cronometro-display-pulso { 0%, 100% { opacity: 1 } 50% { opacity: 0.25 } }'}</style>
      <span role="timer" aria-label={`Tempo restante: ${tempoFormatado}`}>
        {tempoFormatado}
      </span>
      {gravando && (
        <span aria-label="Gravando">
          <span style={pontoStyle} aria-hidden="true" />
          Gravando
        </span>
      )}
    </div>
  )
}
