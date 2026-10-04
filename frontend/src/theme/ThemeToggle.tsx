import { useState } from 'react'
import { aplicarTema, temaInicial, type Tema } from './theme'

/** Alterna entre modo claro e escuro (lembrado entre visitas). */
export function ThemeToggle({ className }: { className?: string }) {
  const [tema, setTema] = useState<Tema>(() => document.documentElement.dataset.theme === 'dark' ? 'dark' : document.documentElement.dataset.theme === 'light' ? 'light' : temaInicial())

  function alternar(): void {
    const proximo: Tema = tema === 'dark' ? 'light' : 'dark'
    aplicarTema(proximo)
    setTema(proximo)
  }

  return (
    <button type="button" className={className ?? 'theme-toggle'} onClick={alternar} aria-pressed={tema === 'dark'}>
      <span aria-hidden="true">{tema === 'dark' ? '☀' : '☾'}</span>{' '}
      {tema === 'dark' ? 'Modo claro' : 'Modo escuro'}
    </button>
  )
}
