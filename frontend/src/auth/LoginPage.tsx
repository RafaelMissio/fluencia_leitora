import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, type Location } from 'react-router-dom'
import type { ApiErrorComRetry } from '../api/client'
import { useAuth } from './AuthContext'
import { ThemeToggle } from '../theme/ThemeToggle'
import './LoginPage.css'

function defaultPathFor(perfil: 'PROFESSOR' | 'COORDENADOR'): string {
  return perfil === 'COORDENADOR' ? '/alunos' : '/avaliar'
}

/** Formulário de e-mail/senha. Trata 401/429 do login separadamente do interceptor 401 genérico do apiClient (não vem de uma sessão). */
export function LoginPage() {
  const { login, isAuthenticated, perfil } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // login() só devolve void (T4) - o `perfil` fresco só fica disponível depois
  // que o AuthProvider atualiza o estado e este componente re-renderiza, daí a
  // navegação reagir à mudança de `isAuthenticated`/`perfil`, não ao retorno
  // do `await login(...)` (que ainda veria os valores antigos, `null`).
  useEffect(() => {
    if (!isAuthenticated || !perfil) return
    const from = (location.state as { from?: Location } | null)?.from
    if (from) {
      navigate({ pathname: from.pathname, search: from.search }, { replace: true })
    } else {
      navigate(defaultPathFor(perfil), { replace: true })
    }
  }, [isAuthenticated, perfil, location.state, navigate])

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, senha)
    } catch (err) {
      const apiError = err as ApiErrorComRetry
      if (apiError.status === 401) {
        setError('E-mail ou senha inválidos')
      } else if (apiError.status === 429) {
        const minutos = apiError.retryAfterSeconds ? Math.ceil(apiError.retryAfterSeconds / 60) : 0
        setError(`Conta bloqueada. Tente novamente em ${minutos} minutos`)
      } else {
        setError('Não foi possível entrar. Tente novamente.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login">
      <ThemeToggle className="theme-toggle login__theme" />
      <form className="login__card" onSubmit={(event) => void handleSubmit(event)}>
        <div className="login__brand">
          <span className="login__logo" aria-hidden="true">
            F
          </span>
          <p className="login__brand-name">
            <strong>Fluência Leitora</strong>
            Avaliação de leitura
          </p>
        </div>
        <h1>Entrar</h1>
        <p className="login__subtitle">Use seu e-mail e senha para acessar.</p>
        {error ? (
          <p role="alert">
            {error}
          </p>
        ) : null}
        <div className="login__field">
          <label htmlFor="login-email">E-mail</label>
          <input
            id="login-email"
            name="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>
        <div className="login__field">
          <label htmlFor="login-senha">Senha</label>
          <div className="login__password">
            <input
              id="login-senha"
              name="senha"
              type={mostrarSenha ? 'text' : 'password'}
              autoComplete="current-password"
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
              required
            />
            <button
              type="button"
              className="login__toggle"
              aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
              onClick={() => setMostrarSenha((atual) => !atual)}
            >
              {mostrarSenha ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>
        </div>
        <p className="login__forgot">
          <Link to="/esqueci-senha">Esqueci a senha</Link>
        </p>
        <button className="login__submit" type="submit" disabled={submitting}>
          Entrar
        </button>
      </form>
    </main>
  )
}
