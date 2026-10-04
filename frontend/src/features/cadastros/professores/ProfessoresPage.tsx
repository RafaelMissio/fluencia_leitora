import { useState, type FormEvent } from 'react'
import type { ApiError } from '../../../api/types'
import { useCriarProfessor, useProfessores } from './useProfessores'

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

function mensagemNoTopo(error: ApiError | null | undefined): string | null {
  if (!error || error.errors) return null
  return error.detail ?? error.code ?? 'Não foi possível salvar'
}

/** Cadastro de professores (spec.md P2, FE-26): lista com as turmas de cada um e criação; mesmo padrão de erro de `AnoLetivoPage`. */
export function ProfessoresPage() {
  const professoresQuery = useProfessores()
  const criar = useCriarProfessor()
  const [nome, setNome] = useState('')

  const erros = mensagensPorCampo(criar.error)
  const topo = mensagemNoTopo(criar.error)

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    criar.mutate({ nome }, { onSuccess: () => setNome('') })
  }

  return (
    <div>
      <h1>Professores</h1>

      <form onSubmit={handleSubmit}>
        {topo ? <p role="alert">{topo}</p> : null}
        {criar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}

        <label htmlFor="professor-nome">Nome</label>
        <input id="professor-nome" value={nome} onChange={(event) => setNome(event.target.value)} />
        {erros.nome ? <p>{erros.nome}</p> : null}

        <button type="submit" disabled={criar.isPending}>
          Criar professor
        </button>
      </form>

      <table aria-label="Professores cadastrados">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Turmas</th>
          </tr>
        </thead>
        <tbody>
          {(professoresQuery.data ?? []).map((professor) => (
            <tr key={professor.id}>
              <td>{professor.nome}</td>
              <td>{professor.turmas.length > 0 ? professor.turmas.map((turma) => turma.nome).join(', ') : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
