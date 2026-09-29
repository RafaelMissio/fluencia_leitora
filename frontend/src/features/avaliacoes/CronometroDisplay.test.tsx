import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CronometroDisplay } from './CronometroDisplay'

describe('CronometroDisplay', () => {
  it('renders 65000ms as "01:05" (spec.md P1 "Executar avaliação")', () => {
    render(<CronometroDisplay tempoRestanteMs={65000} gravando={false} />)

    expect(screen.getByRole('timer')).toHaveTextContent('01:05')
  })

  it('shows the "Gravando" indicator when gravando is true, and hides it when false (AC8)', () => {
    const { rerender } = render(<CronometroDisplay tempoRestanteMs={60000} gravando={true} />)
    expect(screen.getByText('Gravando')).toBeInTheDocument()

    rerender(<CronometroDisplay tempoRestanteMs={60000} gravando={false} />)
    expect(screen.queryByText('Gravando')).not.toBeInTheDocument()
  })

  it('exposes aria-label for screen readers on the timer and the recording indicator (SDD Edge Cases)', () => {
    render(<CronometroDisplay tempoRestanteMs={65000} gravando={true} />)

    expect(screen.getByRole('timer')).toHaveAttribute('aria-label', 'Tempo restante: 01:05')
    expect(screen.getByLabelText('Gravando')).toBeInTheDocument()
  })
})
