import { useState, type FormEvent } from 'react'
import type { ApiError, ConfiguracaoAvaliacaoResponse } from '../../../api/types'
import {
  useAlterarSituacao,
  useAnosLetivos,
  useAtualizarConfiguracao,
  useConfiguracoesDoAno,
  useCriarAnoLetivo,
} from './useAnosLetivos'

const SITUACOES = [
  { valor: 'PLANEJADO', rotulo: 'Planejado' },
  { valor: 'ATIVO', rotulo: 'Ativo' },
  { valor: 'ENCERRADO', rotulo: 'Inativo' },
]

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

/** 409 (e 422 sem `errors[]`, ex. regras de negócio da configuração) mostram `detail`/`code` no topo do formulário (FE-26). */
function mensagemNoTopo(error: ApiError | null | undefined): string | null {
  if (!error || error.errors) return null
  return error.detail ?? error.code ?? 'Não foi possível salvar'
}

function ConfiguracaoLinha({
  anoLetivoId,
  configuracao,
}: {
  anoLetivoId: number
  configuracao: ConfiguracaoAvaliacaoResponse
}) {
  const [minimo, setMinimo] = useState(configuracao.quantidadeMinima)
  const [maximo, setMaximo] = useState(configuracao.quantidadeMaxima)
  const atualizar = useAtualizarConfiguracao(anoLetivoId)
  const topo = mensagemNoTopo(atualizar.error)

  return (
    <tr>
      <td>{configuracao.serie}º ano</td>
      <td>
        <input
          aria-label={`Mínimo da série ${configuracao.serie}`}
          type="number"
          value={minimo}
          onChange={(event) => setMinimo(Number(event.target.value))}
        />
      </td>
      <td>
        <input
          aria-label={`Máximo da série ${configuracao.serie}`}
          type="number"
          value={maximo}
          onChange={(event) => setMaximo(Number(event.target.value))}
        />
      </td>
      <td>
        <button
          type="button"
          disabled={atualizar.isPending}
          onClick={() =>
            atualizar.mutate({
              serie: configuracao.serie,
              quantidadeMinima: minimo,
              quantidadeMaxima: maximo,
            })
          }
        >
          Salvar série {configuracao.serie}
        </button>
        {atualizar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}
        {topo ? <p role="alert">{topo}</p> : null}
      </td>
    </tr>
  )
}

function ConfiguracoesDoAno({ anoLetivoId }: { anoLetivoId: number }) {
  const configuracoesQuery = useConfiguracoesDoAno(anoLetivoId)

  return (
    <table aria-label="Configuração de palavras por série">
      <thead>
        <tr>
          <th>Série</th>
          <th>Mínimo de palavras</th>
          <th>Máximo de palavras</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {(configuracoesQuery.data ?? []).map((configuracao) => (
          <ConfiguracaoLinha
            key={configuracao.id}
            anoLetivoId={anoLetivoId}
            configuracao={configuracao}
          />
        ))}
      </tbody>
    </table>
  )
}

/**
 * Cadastro de anos letivos (spec.md P2, FE-26): lista, criação e edição dos
 * limites de palavras por série. Sucesso mostra "Salvo com sucesso"; 409 vai
 * para o topo do formulário e 422 para o campo (`errors[].field`).
 */
export type ModoAnoLetivo = 'cadastrar' | 'listar'

export function AnoLetivoPage({ modo }: { modo: ModoAnoLetivo }) {
  const anosQuery = useAnosLetivos()
  const criar = useCriarAnoLetivo()
  const alterarSituacao = useAlterarSituacao()
  const [ano, setAno] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [situacao, setSituacao] = useState('PLANEJADO')
  const [anoSelecionado, setAnoSelecionado] = useState<number | undefined>()

  const erros = mensagensPorCampo(criar.error)
  const topo = mensagemNoTopo(criar.error)

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    criar.mutate(
      { ano: Number(ano), dataInicio, dataFim, situacao },
      {
        onSuccess: () => {
          setAno('')
          setDataInicio('')
          setDataFim('')
          setSituacao('PLANEJADO')
        },
      },
    )
  }

  return (
    <div>
      <h1>{modo === 'cadastrar' ? 'Cadastrar ano letivo' : 'Anos letivos cadastrados'}</h1>

      {modo === 'cadastrar' ? (
        <form onSubmit={handleSubmit}>
          {topo ? <p role="alert">{topo}</p> : null}
          {criar.isSuccess ? <p role="status">Salvo com sucesso</p> : null}

          <label htmlFor="ano-letivo-ano">Ano</label>
          <input
            id="ano-letivo-ano"
            type="number"
            value={ano}
            onChange={(event) => setAno(event.target.value)}
          />
          {erros.ano ? <p>{erros.ano}</p> : null}

          <label htmlFor="ano-letivo-inicio">Data de início</label>
          <input
            id="ano-letivo-inicio"
            type="date"
            value={dataInicio}
            onChange={(event) => setDataInicio(event.target.value)}
          />
          {erros.dataInicio ? <p>{erros.dataInicio}</p> : null}

          <label htmlFor="ano-letivo-fim">Data de fim</label>
          <input
            id="ano-letivo-fim"
            type="date"
            value={dataFim}
            onChange={(event) => setDataFim(event.target.value)}
          />
          {erros.dataFim ? <p>{erros.dataFim}</p> : null}

          <label htmlFor="ano-letivo-situacao">Situação</label>
          <select
            id="ano-letivo-situacao"
            value={situacao}
            onChange={(event) => setSituacao(event.target.value)}
          >
            <option value="PLANEJADO">Planejado</option>
            <option value="ATIVO">Ativo</option>
          </select>

          <button type="submit" disabled={criar.isPending}>
            Criar ano letivo
          </button>
        </form>
      ) : (
        <>
          <table aria-label="Anos letivos cadastrados">
            <thead>
              <tr>
                <th>Ano</th>
                <th>Início</th>
                <th>Fim</th>
                <th>Situação</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {(anosQuery.data ?? []).map((item) => (
                <tr key={item.id}>
                  <td>{item.ano}</td>
                  <td>{item.dataInicio}</td>
                  <td>{item.dataFim}</td>
                  <td>
                    <select
                      aria-label={`Situação de ${item.ano}`}
                      value={item.situacao}
                      disabled={alterarSituacao.isPending}
                      onChange={(event) =>
                        alterarSituacao.mutate({ id: item.id, situacao: event.target.value })
                      }
                    >
                      {SITUACOES.map((opcao) => (
                        <option key={opcao.valor} value={opcao.valor}>
                          {opcao.rotulo}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <button type="button" onClick={() => setAnoSelecionado(item.id)}>
                      Configurar {item.ano}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {anoSelecionado !== undefined ? (
            <ConfiguracoesDoAno anoLetivoId={anoSelecionado} />
          ) : null}
        </>
      )}
    </div>
  )
}
