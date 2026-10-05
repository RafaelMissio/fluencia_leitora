import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { request } from '../api/client'
import { ThemeToggle } from '../theme/ThemeToggle'
import './LoginPage.css'

/** Pede o link de redefinição por e-mail. A resposta é sempre a mesma, exista ou não a conta. */
export function EsqueciSenhaPage() {
  const [email, setEmail] = useState('')
  const [enviado, setEnviado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await request<void>('/auth/esqueci-senha', { method: 'POST', body: JSON.stringify({ email }) })
      setEnviado(true)
    } catch {
      setError('Não foi possível enviar o pedido. Tente novamente.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login">
      <ThemeToggle className="theme-toggle login__theme" />
      <form className="login__card" onSubmit={(event) => void handleSubmit(event)}>
        <h1>Esqueci a senha</h1>
        {enviado ? (
          <p role="status" className="login__success">
            Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha. O link vale por 1 hora.
          </p>
        ) : (
          <>
            <p className="login__subtitle">Informe seu e-mail e enviaremos um link para criar uma nova senha.</p>
            {error ? <p role="alert">{error}</p> : null}
            <div className="login__field">
              <label htmlFor="esqueci-email">E-mail</label>
              <input
                id="esqueci-email"
                name="email"
                type="email"
                autoComplete="username"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </div>
            <button className="login__submit" type="submit" disabled={submitting}>
              Enviar link
            </button>
          </>
        )}
        <p className="login__link">
          <Link to="/login">Voltar ao login</Link>
        </p>
      </form>
    </main>
  )
}
