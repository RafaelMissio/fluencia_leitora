import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RedefinirSenhaPage } from './RedefinirSenhaPage'

describe('RedefinirSenhaPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function renderPage(url = '/redefinir-senha?token=abc123') {
    return render(
      <MemoryRouter initialEntries={[url]}>
        <RedefinirSenhaPage />
      </MemoryRouter>,
    )
  }

  async function preencher(senha: string, confirmacao: string) {
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Nova senha'), senha)
    await user.type(screen.getByLabelText('Confirmar senha'), confirmacao)
    await user.click(screen.getByRole('button', { name: 'Redefinir senha' }))
  }

  it('sends the token and new password, then confirms', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))
    renderPage()

    await preencher('NovaSenha#123', 'NovaSenha#123')

    expect(await screen.findByRole('status')).toHaveTextContent('Senha redefinida com sucesso')
    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(String(url)).toContain('/auth/redefinir-senha')
    expect(JSON.parse(String(init?.body))).toEqual({ token: 'abc123', novaSenha: 'NovaSenha#123' })
  })

  it('rejects mismatched or short passwords without calling the API', async () => {
    renderPage()

    await preencher('NovaSenha#123', 'outra')
    expect(await screen.findByRole('alert')).toHaveTextContent('As senhas não conferem')

    const user = userEvent.setup()
    await user.clear(screen.getByLabelText('Nova senha'))
    await user.clear(screen.getByLabelText('Confirmar senha'))
    await user.type(screen.getByLabelText('Nova senha'), 'curta')
    await user.type(screen.getByLabelText('Confirmar senha'), 'curta')
    await user.click(screen.getByRole('button', { name: 'Redefinir senha' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('pelo menos 8 caracteres')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('explains an invalid or expired link (400)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ status: 400, code: 'TOKEN_INVALIDO', detail: 'x' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    renderPage()

    await preencher('NovaSenha#123', 'NovaSenha#123')

    expect(await screen.findByRole('alert')).toHaveTextContent('Link inválido ou expirado')
  })

  it('shows an invalid-link message when there is no token', () => {
    renderPage('/redefinir-senha')

    expect(screen.getByRole('alert')).toHaveTextContent('Link inválido')
    expect(screen.queryByLabelText('Nova senha')).not.toBeInTheDocument()
  })
})
