import { useState, type FormEvent } from 'react'
import type { ApiError, StatusMatricula, TurmaResponse } from '../../../api/types'
import { useAnosLetivos } from '../anosletivos/useAnosLetivos'
import { useProfessores } from '../professores/useProfessores'
import { useAlunosDaTurma, useCriarTurma, useTodasTurmas, useTrocarProfessorDaTurma } from './useTurmas'

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

function mensagemNoTopo(error: ApiError | null | undefined): string | null {
  if (!error || error.errors) return null
  return error.detail ?? error.code ?? 'Não foi possível salvar'
}

function TurmaLinha({ turma, professores }: { turma: TurmaResponse; professores: { id: number; nome: string }[] }) {
  const trocar = useTrocarProfessorDaTurma()
  const topo = mensagemNoTopo(trocar.error)
  const nomeProfessor = professores.find((item) => item.id === turma.professorId)?.nome ?? '—'

  return (
    <tr>
      <td>{turma.nome}</td>
      <td>{turma.serie}º ano</td>
      <td>{nomeProfessor}</td>
      <td>{statusTurma(turma)}</td>
      <td>
        <select
          aria-label={`Professor da turma ${turma.nome}`}
          value={turma.professorId ?? ''}
          disabled={trocar.isPending}
          onChange={(event) => {
            if (event.target.value) trocar.mutate({ turmaId: turma.id, professorId: Number(event.target.value) })
          }}
        >
          <option value="">Selecione</option>
          {professores.map((professor) => (
            <option key={professor.id} value={professor.id}>
              {professor.nome}
            </option>
          ))}
        </select>
        {trocar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}
        {topo ? <p role="alert">{topo}</p> : null}
      </td>
    </tr>
  )
}

const STATUS_MATRICULA: Record<StatusMatricula, string> = {
  CURSANDO: 'Cursando',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado',
}

const statusTurma = (turma: TurmaResponse) => (turma.ativo ? 'Ativa' : 'Inativa')

export type ModoTurmas = 'cadastrar' | 'listar' | 'alunos'

/** Turmas (spec.md P2, FE-26): um submenu para cadastrar, outro para listar e outro para ver os alunos de cada turma. */
export function TurmasPage({ modo = 'cadastrar' }: { modo?: ModoTurmas }) {
  if (modo === 'listar') return <ListarTurmas />
  if (modo === 'alunos') return <AlunosDaTurma />
  return <CadastrarTurma />
}

function ListarTurmas() {
  const turmasQuery = useTodasTurmas()
  const professoresQuery = useProfessores()
  const professores = professoresQuery.data ?? []

  return (
    <div>
      <h1>Listar turmas</h1>
      <table aria-label="Turmas cadastradas">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Série</th>
            <th>Professor</th>
            <th>Status</th>
            <th>Trocar professor</th>
          </tr>
        </thead>
        <tbody>
          {(turmasQuery.data ?? []).map((turma) => (
            <TurmaLinha key={turma.id} turma={turma} professores={professores} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function AlunosDaTurma() {
  const turmasQuery = useTodasTurmas()
  const [turmaId, setTurmaId] = useState('')
  const alunosQuery = useAlunosDaTurma(turmaId ? Number(turmaId) : null)
  const turmas = turmasQuery.data ?? []
  const turma = turmas.find((item) => String(item.id) === turmaId)
  const alunos = alunosQuery.data ?? []

  return (
    <div>
      <h1>Alunos da turma</h1>

      <label htmlFor="alunos-turma">Turma</label>
      <select id="alunos-turma" value={turmaId} onChange={(event) => setTurmaId(event.target.value)}>
        <option value="">Selecione</option>
        {turmas.map((item) => (
          <option key={item.id} value={item.id}>
            {item.nome}
          </option>
        ))}
      </select>

      {turma ? <p>Status da turma: {statusTurma(turma)}</p> : null}
      {alunosQuery.isError ? <p role="alert">Não foi possível carregar os alunos da turma</p> : null}
      {turma && alunosQuery.isSuccess && alunos.length === 0 ? <p>Nenhum aluno matriculado nesta turma</p> : null}

      {turma && alunos.length > 0 ? (
        <table aria-label="Alunos da turma">
          <thead>
            <tr>
              <th>Aluno</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {alunos.map((aluno) => (
              <tr key={aluno.matriculaId}>
                <td>{aluno.nome}</td>
                <td>{STATUS_MATRICULA[aluno.status]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </div>
  )
}

function CadastrarTurma() {
  const professoresQuery = useProfessores()
  const anosQuery = useAnosLetivos()
  const criar = useCriarTurma()

  const [nome, setNome] = useState('')
  const [serie, setSerie] = useState('1')
  const [anoLetivoId, setAnoLetivoId] = useState('')
  const [professorId, setProfessorId] = useState('')

  const professores = professoresQuery.data ?? []
  const erros = mensagensPorCampo(criar.error)
  const topo = mensagemNoTopo(criar.error)

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    criar.mutate(
      {
        nome,
        serie: Number(serie),
        anoLetivoId: Number(anoLetivoId),
        professorId: professorId ? Number(professorId) : null,
      },
      { onSuccess: () => setNome('') },
    )
  }

  return (
    <div>
      <h1>Cadastrar turma</h1>

      <form onSubmit={handleSubmit}>
        {topo ? <p role="alert">{topo}</p> : null}
        {criar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}

        <label htmlFor="turma-nome">Nome</label>
        <input id="turma-nome" value={nome} onChange={(event) => setNome(event.target.value)} />
        {erros.nome ? <p>{erros.nome}</p> : null}

        <label htmlFor="turma-serie">Série</label>
        <select id="turma-serie" value={serie} onChange={(event) => setSerie(event.target.value)}>
          {[1, 2, 3, 4, 5].map((valor) => (
            <option key={valor} value={valor}>
              {valor}º ano
            </option>
          ))}
        </select>
        {erros.serie ? <p>{erros.serie}</p> : null}

        <label htmlFor="turma-ano-letivo">Ano letivo</label>
        <select id="turma-ano-letivo" value={anoLetivoId} onChange={(event) => setAnoLetivoId(event.target.value)}>
          <option value="">Selecione</option>
          {(anosQuery.data ?? []).map((ano) => (
            <option key={ano.id} value={ano.id}>
              {ano.ano}
            </option>
          ))}
        </select>
        {erros.anoLetivoId ? <p>{erros.anoLetivoId}</p> : null}

        <label htmlFor="turma-professor">Professor</label>
        <select id="turma-professor" value={professorId} onChange={(event) => setProfessorId(event.target.value)}>
          <option value="">Sem professor</option>
          {professores.map((professor) => (
            <option key={professor.id} value={professor.id}>
              {professor.nome}
            </option>
          ))}
        </select>

        <button type="submit" disabled={criar.isPending || anoLetivoId === ''}>
          Criar turma
        </button>
      </form>
    </div>
  )
}
