import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { ThemeToggle } from './ThemeToggle'

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear()
    delete document.documentElement.dataset.theme
  })

  afterEach(() => {
    delete document.documentElement.dataset.theme
  })

  it('switches between light and dark, applying data-theme and remembering the choice', async () => {
    document.documentElement.dataset.theme = 'light'
    const user = userEvent.setup()
    render(<ThemeToggle />)

    await user.click(screen.getByRole('button', { name: /Modo escuro/ }))
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(localStorage.getItem('fluencia-tema')).toBe('dark')

    await user.click(screen.getByRole('button', { name: /Modo claro/ }))
    expect(document.documentElement.dataset.theme).toBe('light')
    expect(localStorage.getItem('fluencia-tema')).toBe('light')
  })
})
