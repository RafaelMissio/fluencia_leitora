import { useState, type FormEvent } from 'react'
import type { AlunoBuscaItem, ApiError, TurmaResponse } from '../../../api/types'
import { useAlunoBusca } from '../../alunos/useAlunoBusca'
import { useTurmas } from '../turmas/useTurmas'
import {
  useAlterarSituacaoAluno,
  useAtualizarMatricula,
  useAtualizarNomeAluno,
  useCriarAluno,
} from './useAlunosCadastro'

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
    <select
      id={id}
      aria-label={label}
      value={value}
      onChange={(event) => onChange(event.target.value)}
    >
      <option value="">Selecione</option>
      {turmas.map((turma) => (
        <option key={turma.id} value={turma.id}>
          {turma.nome}
        </option>
      ))}
    </select>
  )
}

/** Formulário de alteração: mesmo layout do cadastro (Nome, Turma, Status), com os dados atuais do aluno. */
function AlunoForm({ aluno, turmas }: { aluno: AlunoBuscaItem; turmas: TurmaResponse[] }) {
  const matriculas = aluno.matriculas ?? []
  const matriculaAtiva = matriculas.find((m) => m.situacaoAnoLetivo === 'ATIVO')
  const [nome, setNome] = useState(aluno.nome)
  const [turmaId, setTurmaId] = useState(matriculaAtiva ? String(matriculaAtiva.turmaId) : '')
  const [ativo, setAtivo] = useState(aluno.ativo !== false)
  const atualizarNome = useAtualizarNomeAluno()
  const atualizarMatricula = useAtualizarMatricula()
  const alterarSituacao = useAlterarSituacaoAluno()

  const topo =
    mensagemNoTopo(atualizarNome.error) ??
    mensagemNoTopo(atualizarMatricula.error) ??
    mensagemNoTopo(alterarSituacao.error)
  const erros = mensagensPorCampo(atualizarNome.error)
  const salvando =
    atualizarNome.isPending || atualizarMatricula.isPending || alterarSituacao.isPending

  function salvar(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (nome !== aluno.nome) atualizarNome.mutate({ alunoId: aluno.alunoId, nome })
    if (matriculaAtiva && turmaId !== '' && Number(turmaId) !== matriculaAtiva.turmaId) {
      atualizarMatricula.mutate({
        matriculaId: matriculaAtiva.matriculaId,
        turmaId: Number(turmaId),
      })
    }
    if (ativo !== (aluno.ativo !== false)) alterarSituacao.mutate({ alunoId: aluno.alunoId, ativo })
  }

  return (
    <form onSubmit={salvar} aria-label={`Alterar ${aluno.nome}`}>
      {topo ? <p role="alert">{topo}</p> : null}
      {atualizarNome.isSuccess || atualizarMatricula.isSuccess || alterarSituacao.isSuccess ? (
        <p role="status">Salvo com sucesso</p>
      ) : null}

      <label htmlFor="aluno-alterar-nome">Nome</label>
      <input
        id="aluno-alterar-nome"
        aria-label={`Nome de ${aluno.nome}`}
        value={nome}
        onChange={(event) => setNome(event.target.value)}
      />
      {erros.nome ? <p>{erros.nome}</p> : null}

      <label htmlFor="aluno-alterar-turma">Turma</label>
      <TurmaSelect
        id="aluno-alterar-turma"
        label={`Turma de ${aluno.nome}`}
        turmas={turmas.filter(
          (t) => !matriculaAtiva || t.anoLetivoId === matriculaAtiva.anoLetivoId,
        )}
        value={turmaId}
        onChange={setTurmaId}
      />

      <label htmlFor="aluno-alterar-status">Status</label>
      <select
        id="aluno-alterar-status"
        aria-label={`Status de ${aluno.nome}`}
        value={ativo ? 'ATIVO' : 'INATIVO'}
        onChange={(event) => setAtivo(event.target.value === 'ATIVO')}
      >
        <option value="ATIVO">Ativo</option>
        <option value="INATIVO">Inativo</option>
      </select>

      <button type="submit" disabled={salvando}>
        Salvar alterações de {aluno.nome}
      </button>
    </form>
  )
}

/**
 * Manutenção cadastral de alunos e matrículas (spec.md P2, FE-26). Sem
 * endpoint de listagem geral, a "lista" é a busca por nome (decisão do
 * usuário): um aluno recém-criado aparece porque a busca passa a usar o nome
 * dele.
 */
export function AlunosCadastroPage({ modo }: { modo?: 'cadastrar' | 'alterar' } = {}) {
  const mostrarCadastro = modo !== 'alterar'
  const mostrarAlteracao = modo !== 'cadastrar'
  const turmasQuery = useTurmas()
  const criar = useCriarAluno()
  const [busca, setBusca] = useState('')
  const [nome, setNome] = useState('')
  const [turmaId, setTurmaId] = useState('')
  const [selecionadoId, setSelecionadoId] = useState<number | null>(null)

  const buscaQuery = useAlunoBusca(busca)
  const turmas = turmasQuery.data ?? []
  const selecionado = (buscaQuery.data?.content ?? []).find((a) => a.alunoId === selecionadoId)
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
      <h1>
        {modo === 'alterar' ? 'Alterar aluno' : modo === 'cadastrar' ? 'Cadastrar aluno' : 'Alunos'}
      </h1>

      {mostrarCadastro ? (
        <form onSubmit={handleSubmit}>
          {topo ? <p role="alert">{topo}</p> : null}
          {criar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}

          <label htmlFor="aluno-cadastro-nome">Nome</label>
          <input
            id="aluno-cadastro-nome"
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
          {erros.nome ? <p>{erros.nome}</p> : null}

          <label htmlFor="aluno-cadastro-turma">Turma</label>
          <TurmaSelect
            id="aluno-cadastro-turma"
            label="Turma"
            turmas={turmas}
            value={turmaId}
            onChange={setTurmaId}
          />
          {erros.turmaId ? <p>{erros.turmaId}</p> : null}

          <button type="submit" disabled={criar.isPending || turmaId === ''}>
            Criar aluno
          </button>
        </form>
      ) : null}

      {mostrarAlteracao ? (
        <>
          <label htmlFor="aluno-cadastro-busca">Buscar aluno por nome</label>
          <input
            id="aluno-cadastro-busca"
            value={busca}
            onChange={(event) => setBusca(event.target.value)}
          />

          <ul aria-label="Alunos encontrados">
            {(buscaQuery.data?.content ?? []).map((aluno) => (
              <li key={aluno.alunoId}>
                <button type="button" onClick={() => setSelecionadoId(aluno.alunoId)}>
                  Selecionar {aluno.nome}
                </button>
              </li>
            ))}
          </ul>

          {selecionado ? (
            <AlunoForm key={selecionado.alunoId} aluno={selecionado} turmas={turmas} />
          ) : null}
        </>
      ) : null}
    </div>
  )
}
