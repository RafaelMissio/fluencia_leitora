import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { EsqueciSenhaPage } from './EsqueciSenhaPage'

describe('EsqueciSenhaPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function renderPage() {
    return render(
      <MemoryRouter>
        <EsqueciSenhaPage />
      </MemoryRouter>,
    )
  }

  it('posts the e-mail and shows a neutral confirmation', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }))
    renderPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('E-mail'), 'prof@escola.com')
    await user.click(screen.getByRole('button', { name: 'Enviar link' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Se o e-mail estiver cadastrado')
    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(String(url)).toContain('/auth/esqueci-senha')
    expect(JSON.parse(String(init?.body))).toEqual({ email: 'prof@escola.com' })
  })

  it('shows an error when the request fails', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('network'))
    renderPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('E-mail'), 'prof@escola.com')
    await user.click(screen.getByRole('button', { name: 'Enviar link' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível enviar o pedido')
  })
})
