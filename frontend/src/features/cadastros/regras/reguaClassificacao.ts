import type { Fase } from '../../../api/types'

/** Limite superior da régua: a avaliação mede de 0 a 60 acertos. */
export const LIMITE_REGUA = 60

export interface FaixaEdicao {
  min: number
  max: number | null
  fase: Fase
  nivel: number | null
}

export type SegmentoRegua =
  | { tipo: 'faixa'; inicio: number; fim: number; fase: Fase; nivel: number | null }
  | { tipo: 'lacuna'; inicio: number; fim: number }
  | { tipo: 'sobreposicao'; inicio: number; fim: number }

export interface Regua {
  segmentos: SegmentoRegua[]
  temLacuna: boolean
  temSobreposicao: boolean
}

/**
 * Pré-visualização client-side da régua (spec.md P2, AC3): ordena as faixas
 * por `min` e pinta `[min, max ?? 60]`. Um intervalo de 0..60 sem cobertura é
 * lacuna; um intervalo coberto por mais de uma faixa é sobreposição. Só
 * destaca - a validação definitiva (ex. `FAIXA_COM_LACUNA`) é do backend.
 * Faixas com `max < min` são ignoradas (erro de digitação, o backend recusa).
 */
export function calcularRegua(faixas: FaixaEdicao[]): Regua {
  const ordenadas = faixas
    .map((faixa) => ({ ...faixa, fim: faixa.max ?? LIMITE_REGUA }))
    .filter((faixa) => faixa.fim >= faixa.min)
    .sort((a, b) => a.min - b.min)

  const segmentos: SegmentoRegua[] = []
  let proximo = 0

  for (const faixa of ordenadas) {
    if (faixa.min > proximo) {
      segmentos.push({ tipo: 'lacuna', inicio: proximo, fim: faixa.min - 1 })
    } else if (faixa.min < proximo) {
      segmentos.push({ tipo: 'sobreposicao', inicio: faixa.min, fim: Math.min(proximo - 1, faixa.fim) })
    }
    segmentos.push({ tipo: 'faixa', inicio: faixa.min, fim: faixa.fim, fase: faixa.fase, nivel: faixa.nivel })
    proximo = Math.max(proximo, faixa.fim + 1)
  }

  if (proximo <= LIMITE_REGUA) {
    segmentos.push({ tipo: 'lacuna', inicio: proximo, fim: LIMITE_REGUA })
  }

  return {
    segmentos,
    temLacuna: segmentos.some((segmento) => segmento.tipo === 'lacuna'),
    temSobreposicao: segmentos.some((segmento) => segmento.tipo === 'sobreposicao'),
  }
}
