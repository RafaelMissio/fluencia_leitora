import { useState } from 'react'
import { ResumoAlunoPanel } from './ResumoAlunoPanel'
import { useAlunoBusca } from './useAlunoBusca'

/**
 * Campo de busca (300ms debounce, T7) + lista de resultados; seleção guarda
 * o `alunoId` em estado local (spec.md P1 "Buscar aluno", AC1/AC3) e embute
 * o painel de resumo (`ResumoAlunoPanel`, T11).
 */
export function AlunoBuscaPage() {
  const [nome, setNome] = useState('')
  const [alunoSelecionadoId, setAlunoSelecionadoId] = useState<number | null>(null)
  const { data, isLoading } = useAlunoBusca(nome)

  const resultados = data?.content ?? []
  const buscaAtiva = nome.trim().length >= 2

  return (
    <div>
      <h1>Buscar aluno</h1>
      <label htmlFor="busca-aluno-nome">Nome do aluno</label>
      <input
        id="busca-aluno-nome"
        type="text"
        value={nome}
        onChange={(event) => setNome(event.target.value)}
      />

      {buscaAtiva && !isLoading && resultados.length === 0 ? <p>Nenhum aluno encontrado</p> : null}

      <ul>
        {resultados.map((aluno) => (
          <li key={aluno.alunoId}>
            <button
              type="button"
              aria-pressed={aluno.alunoId === alunoSelecionadoId}
              onClick={() => setAlunoSelecionadoId(aluno.alunoId)}
            >
              {aluno.nome} - {aluno.turma ?? '—'} - {aluno.serie}ª série
            </button>
          </li>
        ))}
      </ul>

      {alunoSelecionadoId !== null ? <ResumoAlunoPanel alunoId={alunoSelecionadoId} /> : null}
    </div>
  )
}
