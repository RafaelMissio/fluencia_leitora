import { describe, expect, it } from 'vitest'
import { calcularRegua, type FaixaEdicao } from './reguaClassificacao'

const completa: FaixaEdicao[] = [
  { min: 0, max: 14, fase: 'PRE_LEITOR', nivel: 1 },
  { min: 15, max: 29, fase: 'LEITOR_INICIANTE', nivel: null },
  { min: 30, max: null, fase: 'LEITOR_FLUENTE', nivel: null },
]

describe('calcularRegua', () => {
  it('a full 0-60 ruler has no gap nor overlap and paints each faixa', () => {
    const regua = calcularRegua(completa)
    expect(regua.temLacuna).toBe(false)
    expect(regua.temSobreposicao).toBe(false)
    expect(regua.segmentos).toEqual([
      { tipo: 'faixa', inicio: 0, fim: 14, fase: 'PRE_LEITOR', nivel: 1 },
      { tipo: 'faixa', inicio: 15, fim: 29, fase: 'LEITOR_INICIANTE', nivel: null },
      { tipo: 'faixa', inicio: 30, fim: 60, fase: 'LEITOR_FLUENTE', nivel: null },
    ])
  })

  it('flags a gap between two faixas', () => {
    const regua = calcularRegua([completa[0], { ...completa[1], min: 20 }, completa[2]].map((f, i) => (i === 1 ? { ...f, max: 29 } : f)))
    expect(regua.temLacuna).toBe(true)
    expect(regua.segmentos).toContainEqual({ tipo: 'lacuna', inicio: 15, fim: 19 })
  })

  it('flags an overlap between two faixas', () => {
    const regua = calcularRegua([completa[0], { ...completa[1], min: 10 }, completa[2]])
    expect(regua.temSobreposicao).toBe(true)
    expect(regua.segmentos).toContainEqual({ tipo: 'sobreposicao', inicio: 10, fim: 14 })
  })

  it('flags a gap at the start when the first faixa does not begin at 0 and at the end when the last is limited', () => {
    const regua = calcularRegua([{ min: 5, max: 40, fase: 'PRE_LEITOR', nivel: 1 }])
    expect(regua.segmentos[0]).toEqual({ tipo: 'lacuna', inicio: 0, fim: 4 })
    expect(regua.segmentos.at(-1)).toEqual({ tipo: 'lacuna', inicio: 41, fim: 60 })
  })

  it('sorts faixas by minimum regardless of the order given', () => {
    const regua = calcularRegua([completa[2], completa[0], completa[1]])
    expect(regua.temLacuna).toBe(false)
    expect(regua.segmentos.map((s) => s.inicio)).toEqual([0, 15, 30])
  })

  it('treats everything as a gap when there are no faixas', () => {
    expect(calcularRegua([]).segmentos).toEqual([{ tipo: 'lacuna', inicio: 0, fim: 60 }])
  })
})
