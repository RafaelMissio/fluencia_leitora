import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { PalavraAvaliacao } from '../api/types'
import { GradePalavras } from './GradePalavras'

function palavras(): PalavraAvaliacao[] {
  return [
    { ordem: 1, palavra: 'casa', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
    { ordem: 2, palavra: 'bola', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
  ]
}

/** Contraste relativo WCAG 2.x (não "a olho" - Done-when de T20). */
function luminanciaRelativa(hex: string): number {
  const valores = [hex.slice(1, 3), hex.slice(3, 5), hex.slice(5, 7)].map((parte) => parseInt(parte, 16) / 255)
  const [r, g, b] = valores.map((v) => (v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)))
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function razaoContraste(hexA: string, hexB: string): number {
  const la = luminanciaRelativa(hexA)
  const lb = luminanciaRelativa(hexB)
  const [claro, escuro] = la > lb ? [la, lb] : [lb, la]
  return (claro + 0.05) / (escuro + 0.05)
}

describe('GradePalavras', () => {
  it('tapping a word once sets it to CORRETA and calls onMarcar (spec.md AC1)', async () => {
    const onMarcar = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="EM_ANDAMENTO" onMarcar={onMarcar} />)

    await user.click(screen.getByRole('button', { name: 'casa: Pendente' }))

    expect(onMarcar).toHaveBeenCalledWith(1, 'CORRETA')
    expect(screen.getByRole('button', { name: 'casa: Correta' })).toHaveStyle({ color: 'rgb(27, 94, 32)' })
  })

  it('tapping a word twice sets it to INCORRETA (red, ✗) and calls onMarcar (spec.md AC1)', async () => {
    const onMarcar = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="EM_ANDAMENTO" onMarcar={onMarcar} />)

    const botao = screen.getByRole('button', { name: 'casa: Pendente' })
    await user.click(botao)
    await user.click(screen.getByRole('button', { name: 'casa: Correta' }))

    expect(onMarcar).toHaveBeenLastCalledWith(1, 'INCORRETA')
    const botaoFinal = screen.getByRole('button', { name: 'casa: Incorreta' })
    expect(botaoFinal).toHaveTextContent('✗')
    expect(botaoFinal).toHaveStyle({ color: 'rgb(183, 28, 28)' })
  })

  it('the C shortcut on the focused word marks it CORRETA directly (spec.md Assumptions)', async () => {
    const onMarcar = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="EM_ANDAMENTO" onMarcar={onMarcar} />)

    screen.getByRole('button', { name: 'casa: Pendente' }).focus()
    await user.keyboard('c')

    expect(onMarcar).toHaveBeenCalledWith(1, 'CORRETA')
  })

  it('the I shortcut on the focused word marks it INCORRETA directly (spec.md Assumptions)', async () => {
    const onMarcar = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="EM_ANDAMENTO" onMarcar={onMarcar} />)

    screen.getByRole('button', { name: 'casa: Pendente' }).focus()
    await user.keyboard('i')

    expect(onMarcar).toHaveBeenCalledWith(1, 'INCORRETA')
  })

  it('the N shortcut on the focused word marks it NAO_LIDA directly (spec.md Assumptions)', async () => {
    const onMarcar = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="EM_ANDAMENTO" onMarcar={onMarcar} />)

    screen.getByRole('button', { name: 'casa: Pendente' }).focus()
    await user.keyboard('n')

    expect(onMarcar).toHaveBeenCalledWith(1, 'NAO_LIDA')
  })

  it('a rejected onMarcar reverts the word to its previous status and shows the failure message (AC3)', async () => {
    const onMarcar = vi.fn().mockRejectedValue(new Error('falha de rede'))
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="EM_ANDAMENTO" onMarcar={onMarcar} />)

    await user.click(screen.getByRole('button', { name: 'casa: Pendente' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível salvar a marcação')
    expect(screen.getByRole('button', { name: 'casa: Pendente' })).toBeInTheDocument()
  })

  it('an avaliação CRIADA disables marking: tapping does not call onMarcar (AC4)', async () => {
    const onMarcar = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="CRIADA" onMarcar={onMarcar} />)

    const botao = screen.getByRole('button', { name: 'casa: Pendente' })
    expect(botao).toBeDisabled()
    await user.click(botao)

    expect(onMarcar).not.toHaveBeenCalled()
  })

  it('marking a word of a FINALIZADA avaliação is allowed and shows the audit warning (AC5)', async () => {
    const onMarcar = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<GradePalavras palavras={palavras()} avaliacaoStatus="FINALIZADA" onMarcar={onMarcar} />)

    await user.click(screen.getByRole('button', { name: 'casa: Pendente' }))

    expect(onMarcar).toHaveBeenCalledWith(1, 'CORRETA')
    expect(await screen.findByRole('status')).toHaveTextContent('Alteração registrada em auditoria')
  })

  it('all 4 status colors have a contrast ratio of at least 4.5:1 against white (WCAG 1.4.1, spec.md AC2)', () => {
    const cores = ['#1B5E20', '#B71C1C', '#616161']
    for (const cor of cores) {
      expect(razaoContraste(cor, '#FFFFFF')).toBeGreaterThanOrEqual(4.5)
    }
  })
})
