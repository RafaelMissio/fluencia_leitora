import { useState, type FormEvent } from 'react'
import type { AlunoBuscaItem, ApiError, TurmaResponse } from '../../../api/types'
import { useAlunoBusca } from '../../alunos/useAlunoBusca'
import { useTurmas } from '../turmas/useTurmas'
import { useAtualizarNomeAluno, useCriarAluno, useInativarAluno, useNovaMatricula } from './useAlunosCadastro'

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

function mensagemNoTopo(error: ApiError | null | undefined): string | null {
  if (!error || error.errors) return null
  return error.detail ?? error.code ?? 'Não foi possível salvar'
}

function TurmaSelect({
  id,
  label,
  turmas,
  value,
  onChange,
}: {
  id: string
  label: string
  turmas: TurmaResponse[]
  value: string
  onChange: (valor: string) => void
}) {
  return (
    <select id={id} aria-label={label} value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">Selecione</option>
      {turmas.map((turma) => (
        <option key={turma.id} value={turma.id}>
          {turma.nome}
        </option>
      ))}
    </select>
  )
}

function AlunoLinha({ aluno, turmas }: { aluno: AlunoBuscaItem; turmas: TurmaResponse[] }) {
  const [nome, setNome] = useState(aluno.nome)
  const [turmaId, setTurmaId] = useState('')
  const atualizarNome = useAtualizarNomeAluno()
  const inativar = useInativarAluno()
  const matricular = useNovaMatricula()

  const erroTopo = mensagemNoTopo(atualizarNome.error) ?? mensagemNoTopo(inativar.error) ?? mensagemNoTopo(matricular.error)
  const erroNome = mensagensPorCampo(atualizarNome.error).nome

  return (
    <li>
      <p>
        {aluno.turma ?? 'Sem matrícula ativa'}
        {aluno.anoLetivo ? ` - ${aluno.anoLetivo}` : ''}
      </p>

      <input aria-label={`Nome de ${aluno.nome}`} value={nome} onChange={(event) => setNome(event.target.value)} />
      <button type="button" onClick={() => atualizarNome.mutate({ alunoId: aluno.alunoId, nome })}>
        Salvar nome de {aluno.nome}
      </button>
      {erroNome ? <p>{erroNome}</p> : null}

      <TurmaSelect
        id={`matricula-turma-${aluno.alunoId}`}
        label={`Turma da nova matrícula de ${aluno.nome}`}
        turmas={turmas}
        value={turmaId}
        onChange={setTurmaId}
      />
      <button
        type="button"
        disabled={turmaId === '' || matricular.isPending}
        onClick={() => matricular.mutate({ alunoId: aluno.alunoId, turmaId: Number(turmaId) })}
      >
        Nova matrícula de {aluno.nome}
      </button>

      <button type="button" className="danger" disabled={inativar.isPending} onClick={() => inativar.mutate(aluno.alunoId)}>
        Inativar {aluno.nome}
      </button>

      {atualizarNome.isSuccess || matricular.isSuccess ? <p role="status">Salvo com sucesso</p> : null}
      {erroTopo ? <p role="alert">{erroTopo}</p> : null}
    </li>
  )
}

/**
 * Manutenção cadastral de alunos e matrículas (spec.md P2, FE-26). Sem
 * endpoint de listagem geral, a "lista" é a busca por nome (decisão do
 * usuário): um aluno recém-criado aparece porque a busca passa a usar o nome
 * dele.
 */
export function AlunosCadastroPage() {
  const turmasQuery = useTurmas()
  const criar = useCriarAluno()
  const [busca, setBusca] = useState('')
  const [nome, setNome] = useState('')
  const [turmaId, setTurmaId] = useState('')

  const buscaQuery = useAlunoBusca(busca)
  const turmas = turmasQuery.data ?? []
  const erros = mensagensPorCampo(criar.error)
  const topo = mensagemNoTopo(criar.error)

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    const nomeCriado = nome
    criar.mutate(
      { nome: nomeCriado, turmaId: Number(turmaId) },
      {
        onSuccess: () => {
          setBusca(nomeCriado)
          setNome('')
        },
      },
    )
  }

  return (
    <div>
      <h1>Alunos</h1>

      <form onSubmit={handleSubmit}>
        {topo ? <p role="alert">{topo}</p> : null}
        {criar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}

        <label htmlFor="aluno-cadastro-nome">Nome</label>
        <input id="aluno-cadastro-nome" value={nome} onChange={(event) => setNome(event.target.value)} />
        {erros.nome ? <p>{erros.nome}</p> : null}

        <label htmlFor="aluno-cadastro-turma">Turma</label>
        <TurmaSelect id="aluno-cadastro-turma" label="Turma" turmas={turmas} value={turmaId} onChange={setTurmaId} />
        {erros.turmaId ? <p>{erros.turmaId}</p> : null}

        <button type="submit" disabled={criar.isPending || turmaId === ''}>
          Criar aluno
        </button>
      </form>

      <label htmlFor="aluno-cadastro-busca">Buscar aluno por nome</label>
      <input id="aluno-cadastro-busca" value={busca} onChange={(event) => setBusca(event.target.value)} />

      <ul aria-label="Alunos encontrados">
        {(buscaQuery.data?.content ?? []).map((aluno) => (
          <AlunoLinha key={aluno.alunoId} aluno={aluno} turmas={turmas} />
        ))}
      </ul>
    </div>
  )
}
