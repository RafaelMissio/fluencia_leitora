import { useState, type FormEvent } from 'react'
import type { ApiError, Perfil, UsuarioResponse } from '../../../api/types'
import { useProfessores } from '../professores/useProfessores'
import { useAlterarSenha, useCriarUsuario, useUsuarios } from './useUsuarios'

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

function mensagemNoTopo(error: ApiError | null | undefined): string | null {
  if (!error || error.errors) return null
  return error.detail ?? error.code ?? 'Não foi possível salvar'
}

function UsuarioLinha({ usuario }: { usuario: UsuarioResponse }) {
  const [senha, setSenha] = useState('')
  const alterar = useAlterarSenha()
  const topo = mensagemNoTopo(alterar.error)
  const erroSenha = mensagensPorCampo(alterar.error).senha

  return (
    <tr>
      <td>{usuario.email}</td>
      <td>{usuario.perfil}</td>
      <td>
        <input
          type="password"
          autoComplete="new-password"
          aria-label={`Nova senha de ${usuario.email}`}
          value={senha}
          onChange={(event) => setSenha(event.target.value)}
        />
        <button
          type="button"
          disabled={senha === '' || alterar.isPending}
          onClick={() =>
            alterar.mutate({ usuarioId: usuario.id, senha }, { onSuccess: () => setSenha('') })
          }
        >
          Alterar senha de {usuario.email}
        </button>
        {alterar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}
        {erroSenha ? <p>{erroSenha}</p> : null}
        {topo ? <p role="alert">{topo}</p> : null}
      </td>
    </tr>
  )
}

/** Cadastro de usuários (spec.md P2, FE-26): lista, criação e troca de senha; a senha nunca é exibida nem mantida após o envio. */
export function UsuariosPage() {
  const usuariosQuery = useUsuarios()
  const professoresQuery = useProfessores()
  const criar = useCriarUsuario()

  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [perfil, setPerfil] = useState<Perfil>('PROFESSOR')
  const [professorId, setProfessorId] = useState('')

  const erros = mensagensPorCampo(criar.error)
  const topo = mensagemNoTopo(criar.error)

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    criar.mutate(
      {
        email,
        senha,
        perfil,
        professorId: perfil === 'PROFESSOR' && professorId ? Number(professorId) : null,
      },
      {
        onSuccess: () => {
          setEmail('')
          setSenha('')
        },
      },
    )
  }

  return (
    <div>
      <h1>Usuários</h1>

      <form onSubmit={handleSubmit}>
        {topo ? <p role="alert">{topo}</p> : null}
        {criar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}

        <label htmlFor="usuario-email">E-mail</label>
        <input id="usuario-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
        {erros.email ? <p>{erros.email}</p> : null}

        <label htmlFor="usuario-senha">Senha</label>
        <input
          id="usuario-senha"
          type="password"
          autoComplete="new-password"
          value={senha}
          onChange={(event) => setSenha(event.target.value)}
        />
        {erros.senha ? <p>{erros.senha}</p> : null}

        <label htmlFor="usuario-perfil">Perfil</label>
        <select id="usuario-perfil" value={perfil} onChange={(event) => setPerfil(event.target.value as Perfil)}>
          <option value="PROFESSOR">PROFESSOR</option>
          <option value="COORDENADOR">COORDENADOR</option>
        </select>

        {perfil === 'PROFESSOR' ? (
          <>
            <label htmlFor="usuario-professor">Professor</label>
            <select
              id="usuario-professor"
              value={professorId}
              onChange={(event) => setProfessorId(event.target.value)}
            >
              <option value="">Selecione</option>
              {(professoresQuery.data ?? []).map((professor) => (
                <option key={professor.id} value={professor.id}>
                  {professor.nome}
                </option>
              ))}
            </select>
          </>
        ) : null}

        <button type="submit" disabled={criar.isPending}>
          Criar usuário
        </button>
      </form>

      <table aria-label="Usuários cadastrados">
        <thead>
          <tr>
            <th>E-mail</th>
            <th>Perfil</th>
            <th>Alterar senha</th>
          </tr>
        </thead>
        <tbody>
          {(usuariosQuery.data ?? []).map((usuario) => (
            <UsuarioLinha key={usuario.id} usuario={usuario} />
          ))}
        </tbody>
      </table>
    </div>
  )
}
