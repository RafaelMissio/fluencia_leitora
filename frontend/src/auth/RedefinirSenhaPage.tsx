import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import type { ApiErrorComRetry } from '../api/client'
import { request } from '../api/client'
import { ThemeToggle } from '../theme/ThemeToggle'
import './LoginPage.css'

/** Define a nova senha a partir do token recebido por e-mail (`?token=`). */
export function RedefinirSenhaPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''

  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [concluido, setConcluido] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setError(null)
    if (senha.length < 8) {
      setError('A senha deve ter pelo menos 8 caracteres')
      return
    }
    if (senha !== confirmacao) {
      setError('As senhas não conferem')
      return
    }
    setSubmitting(true)
    try {
      await request<void>('/auth/redefinir-senha', {
        method: 'POST',
        body: JSON.stringify({ token, novaSenha: senha }),
      })
      setConcluido(true)
    } catch (err) {
      const status = (err as ApiErrorComRetry).status
      setError(
        status === 400
          ? 'Link inválido ou expirado. Solicite um novo.'
          : 'Não foi possível redefinir a senha. Tente novamente.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login">
      <ThemeToggle className="theme-toggle login__theme" />
      <form className="login__card" onSubmit={(event) => void handleSubmit(event)}>
        <h1>Nova senha</h1>
        {concluido ? (
          <p role="status" className="login__success">
            Senha redefinida com sucesso. Você já pode entrar.
          </p>
        ) : !token ? (
          <p role="alert">Link inválido. Solicite um novo em “Esqueci a senha”.</p>
        ) : (
          <>
            {error ? <p role="alert">{error}</p> : null}
            <div className="login__field">
              <label htmlFor="nova-senha">Nova senha</label>
              <div className="login__password">
                <input
                  id="nova-senha"
                  type={mostrarSenha ? 'text' : 'password'}
                  autoComplete="new-password"
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
            <div className="login__field">
              <label htmlFor="confirmar-senha">Confirmar senha</label>
              <input
                id="confirmar-senha"
                type={mostrarSenha ? 'text' : 'password'}
                autoComplete="new-password"
                value={confirmacao}
                onChange={(event) => setConfirmacao(event.target.value)}
                required
              />
            </div>
            <button className="login__submit" type="submit" disabled={submitting}>
              Redefinir senha
            </button>
          </>
        )}
        <p className="login__link">
          <Link to="/login">Ir para o login</Link>
        </p>
      </form>
    </main>
  )
}
