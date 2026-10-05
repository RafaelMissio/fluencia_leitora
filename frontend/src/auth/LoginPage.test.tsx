import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from './AuthContext'
import { LoginPage } from './LoginPage'

function jsonResponse(status: number, body: unknown, headers?: Record<string, string>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  })
}

function renderLoginPage(initialEntries: (string | { pathname: string; state?: unknown })[]) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={initialEntries}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/avaliar" element={<div>Tela Avaliar</div>} />
          <Route path="/alunos" element={<div>Tela Alunos</div>} />
          <Route path="/destino-original" element={<div>Destino original</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

async function submitLogin(email = 'prof@escola.com', senha = 'segredo123'): Promise<void> {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('E-mail'), email)
  await user.type(screen.getByLabelText('Senha'), senha)
  await user.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('toggles password visibility', async () => {
    renderLoginPage(['/login'])
    const user = userEvent.setup()
    const senha = screen.getByLabelText('Senha')
    expect(senha).toHaveAttribute('type', 'password')

    await user.click(screen.getByRole('button', { name: /mostrar senha/i }))
    expect(senha).toHaveAttribute('type', 'text')

    await user.click(screen.getByRole('button', { name: /ocultar senha/i }))
    expect(senha).toHaveAttribute('type', 'password')
  })

  it('links to the forgot-password page', () => {
    renderLoginPage(['/login'])

    expect(screen.getByRole('link', { name: 'Esqueci a senha' })).toHaveAttribute('href', '/esqueci-senha')
  })

  it('a valid PROFESSOR login opens /avaliar (spec.md P1 "Login", AC1)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(200, { accessToken: 'tok', expiresIn: 3600, perfil: 'PROFESSOR', professorId: 1 }),
    )
    renderLoginPage(['/login'])

    await submitLogin()

    await waitFor(() => expect(screen.getByText('Tela Avaliar')).toBeInTheDocument())
  })

  it('a valid COORDENADOR login opens /alunos (spec.md P1 "Login", AC1)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(200, { accessToken: 'tok', expiresIn: 3600, perfil: 'COORDENADOR', professorId: null }),
    )
    renderLoginPage(['/login'])

    await submitLogin()

    await waitFor(() => expect(screen.getByText('Tela Alunos')).toBeInTheDocument())
  })

  it('a valid login with a preserved return route opens that route instead of the default', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(200, { accessToken: 'tok', expiresIn: 3600, perfil: 'PROFESSOR', professorId: 1 }),
    )
    renderLoginPage([{ pathname: '/login', state: { from: { pathname: '/destino-original', search: '' } } }])

    await submitLogin()

    await waitFor(() => expect(screen.getByText('Destino original')).toBeInTheDocument())
    expect(screen.queryByText('Tela Avaliar')).not.toBeInTheDocument()
  })

  it('a 401 shows "E-mail ou senha inválidos" and keeps the typed email (AC2)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(401, { code: 'CREDENCIAIS_INVALIDAS' }))
    renderLoginPage(['/login'])

    await submitLogin('prof@escola.com', 'senha-errada')

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('E-mail ou senha inválidos'))
    expect(screen.getByLabelText('E-mail')).toHaveValue('prof@escola.com')
  })

  it('a 429 shows "Conta bloqueada. Tente novamente em N minutos" rounding Retry-After up (AC3)', async () => {
    // 125s / 60 = 2.08... -> arredondado para cima = 3 minutos (prova o "para cima", não só a divisão).
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(429, { code: 'CONTA_BLOQUEADA' }, { 'Retry-After': '125' }))
    renderLoginPage(['/login'])

    await submitLogin()

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Conta bloqueada. Tente novamente em 3 minutos'),
    )
  })
})
