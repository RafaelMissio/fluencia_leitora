export type Tema = 'light' | 'dark'

const CHAVE = 'fluencia-tema'

function lerArmazenado(): Tema | null {
  try {
    const valor = localStorage.getItem(CHAVE)
    return valor === 'light' || valor === 'dark' ? valor : null
  } catch {
    return null
  }
}

/** Tema salvo pelo usuário; sem escolha, segue o do sistema operacional. */
export function temaInicial(): Tema {
  return lerArmazenado() ?? (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
}

/** Aplica no `<html data-theme>` (os tokens de cor em `index.css` reagem a isso) e persiste a escolha. */
export function aplicarTema(tema: Tema, persistir = true): void {
  document.documentElement.dataset.theme = tema
  if (!persistir) return
  try {
    localStorage.setItem(CHAVE, tema)
  } catch {
    // armazenamento indisponível (modo privado etc.): o tema vale só para esta sessão
  }
}
