import { useState, type FormEvent } from 'react'
import type { ApiError, ListaPalavrasRequest, TipoLeituraCodigo, TipoPalavra } from '../../../api/types'
import { useListasPalavras } from '../../avaliacoes/useListasPalavras'
import {
  buscarListaPorId,
  useAtualizarLista,
  useCriarLista,
  useInativarLista,
} from './useListasPalavrasCadastro'

const SERIES = [1, 2, 3, 4, 5]
const TIPOS_LEITURA: { codigo: TipoLeituraCodigo; label: string }[] = [
  { codigo: 'PALAVRA', label: 'Palavra' },
  { codigo: 'PSEUDOPALAVRA', label: 'Pseudopalavra' },
  { codigo: 'TEXTO_CURTO', label: 'Texto curto' },
]

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

function mensagemNoTopo(error: ApiError | null | undefined): string | null {
  if (!error || error.errors) return null
  return error.detail ?? error.code ?? 'Não foi possível salvar'
}

function separarPalavras(texto: string): string[] {
  return texto.split(/\s+/).map((palavra) => palavra.trim()).filter(Boolean)
}

/**
 * Cadastro de listas de palavras (spec.md P2, FE-26/FE-27): consulta por
 * série + tipo de leitura, criação, edição (com `version`) e inativação. No
 * 1º ano a opção `NAO_CANONICA` fica desabilitada (o backend rejeita com
 * `NAO_CANONICA_PROIBIDA_1_ANO`).
 */
export function ListasPalavrasPage() {
  const [filtroSerie, setFiltroSerie] = useState(1)
  const [filtroTipo, setFiltroTipo] = useState<TipoLeituraCodigo>('PALAVRA')
  const listasQuery = useListasPalavras(filtroSerie, filtroTipo)

  const criar = useCriarLista()
  const atualizar = useAtualizarLista()
  const inativar = useInativarLista()

  const [editando, setEditando] = useState<{ id: number; version: number } | null>(null)
  const [nome, setNome] = useState('')
  const [serie, setSerie] = useState(1)
  const [tipoLeitura, setTipoLeitura] = useState<TipoLeituraCodigo>('PALAVRA')
  const [tipoPalavraEscolhido, setTipoPalavraEscolhido] = useState<TipoPalavra>('CANONICA')
  const [conteudo, setConteudo] = useState('')
  const [erroCarga, setErroCarga] = useState<string | null>(null)
  // `limparFormulario` troca a mutation ativa (editar -> criar), então o sucesso é guardado à parte.
  const [salvo, setSalvo] = useState(false)

  // 1º ano nunca aceita NAO_CANONICA: valor derivado, sem efeito + setState.
  const tipoPalavra: TipoPalavra = serie === 1 ? 'CANONICA' : tipoPalavraEscolhido
  const textoCurto = tipoLeitura === 'TEXTO_CURTO'

  const mutacao = editando ? atualizar : criar
  const erros = mensagensPorCampo(mutacao.error)
  const topo = mensagemNoTopo(mutacao.error) ?? erroCarga

  function limparFormulario(): void {
    setEditando(null)
    setNome('')
    setConteudo('')
  }

  function montarPayload(): ListaPalavrasRequest {
    return {
      nome,
      serie,
      tipoLeitura,
      tipoPalavra,
      ...(textoCurto
        ? { texto: conteudo }
        : { itens: separarPalavras(conteudo).map((palavra) => ({ palavra, tipoPalavra })) }),
    }
  }

  function aoSalvarComSucesso(): void {
    setSalvo(true)
    setFiltroSerie(serie)
    setFiltroTipo(tipoLeitura)
    limparFormulario()
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    setErroCarga(null)
    setSalvo(false)
    if (editando) {
      atualizar.mutate({ ...editando, payload: montarPayload() }, { onSuccess: aoSalvarComSucesso })
    } else {
      criar.mutate(montarPayload(), { onSuccess: aoSalvarComSucesso })
    }
  }

  async function iniciarEdicao(id: number): Promise<void> {
    setErroCarga(null)
    try {
      const lista = await buscarListaPorId(id)
      setEditando({ id: lista.id, version: lista.version })
      setNome(lista.nome)
      setSerie(lista.serie)
      setTipoLeitura(lista.tipoLeitura)
      setTipoPalavraEscolhido(lista.tipoPalavra ?? 'CANONICA')
      setConteudo(lista.tipoLeitura === 'TEXTO_CURTO' ? (lista.texto ?? '') : lista.itens.map((item) => item.palavra).join(' '))
    } catch {
      setErroCarga('Não foi possível carregar a lista para edição')
    }
  }

  return (
    <div>
      <h1>Listas de palavras</h1>

      <form onSubmit={handleSubmit}>
        <h2>{editando ? 'Editar lista' : 'Nova lista'}</h2>
        {topo ? <p role="alert">{topo}</p> : null}
        {salvo ? <p role="status">Salvo com sucesso</p> : null}

        <label htmlFor="lista-nome">Nome</label>
        <input id="lista-nome" value={nome} onChange={(event) => setNome(event.target.value)} />
        {erros.nome ? <p>{erros.nome}</p> : null}

        <label htmlFor="lista-serie">Série</label>
        <select id="lista-serie" value={serie} onChange={(event) => setSerie(Number(event.target.value))}>
          {SERIES.map((valor) => (
            <option key={valor} value={valor}>
              {valor}º ano
            </option>
          ))}
        </select>
        {erros.serie ? <p>{erros.serie}</p> : null}

        <label htmlFor="lista-tipo-leitura">Tipo de leitura</label>
        <select
          id="lista-tipo-leitura"
          value={tipoLeitura}
          onChange={(event) => setTipoLeitura(event.target.value as TipoLeituraCodigo)}
        >
          {TIPOS_LEITURA.map((tipo) => (
            <option key={tipo.codigo} value={tipo.codigo}>
              {tipo.label}
            </option>
          ))}
        </select>

        <label htmlFor="lista-tipo-palavra">Tipo de palavra</label>
        <select
          id="lista-tipo-palavra"
          value={tipoPalavra}
          onChange={(event) => setTipoPalavraEscolhido(event.target.value as TipoPalavra)}
        >
          <option value="CANONICA">CANONICA</option>
          <option value="NAO_CANONICA" disabled={serie === 1}>
            NAO_CANONICA
          </option>
        </select>
        {serie === 1 ? <p>O 1º ano aceita apenas palavras canônicas.</p> : null}

        <label htmlFor="lista-conteudo">{textoCurto ? 'Texto' : 'Palavras (separadas por espaço)'}</label>
        <textarea id="lista-conteudo" value={conteudo} onChange={(event) => setConteudo(event.target.value)} />
        {erros.itens ? <p>{erros.itens}</p> : null}
        {erros.texto ? <p>{erros.texto}</p> : null}

        <button type="submit" disabled={mutacao.isPending}>
          {editando ? 'Salvar alterações' : 'Criar lista'}
        </button>
        {editando ? (
          <button type="button" onClick={limparFormulario}>
            Cancelar edição
          </button>
        ) : null}
      </form>

      <h2>Listas cadastradas</h2>
      <label htmlFor="filtro-serie">Série</label>
      <select id="filtro-serie" value={filtroSerie} onChange={(event) => setFiltroSerie(Number(event.target.value))}>
        {SERIES.map((valor) => (
          <option key={valor} value={valor}>
            {valor}º ano
          </option>
        ))}
      </select>
      <label htmlFor="filtro-tipo">Tipo de leitura</label>
      <select
        id="filtro-tipo"
        value={filtroTipo}
        onChange={(event) => setFiltroTipo(event.target.value as TipoLeituraCodigo)}
      >
        {TIPOS_LEITURA.map((tipo) => (
          <option key={tipo.codigo} value={tipo.codigo}>
            {tipo.label}
          </option>
        ))}
      </select>

      <ul aria-label="Listas cadastradas">
        {(listasQuery.data ?? []).map((lista) => (
          <li key={lista.id}>
            {lista.nome} ({lista.quantidadePalavras} palavras)
            <button type="button" onClick={() => void iniciarEdicao(lista.id)}>
              Editar {lista.nome}
            </button>
            <button type="button" className="danger" disabled={inativar.isPending} onClick={() => inativar.mutate(lista.id)}>
              Inativar {lista.nome}
            </button>
          </li>
        ))}
      </ul>
      {mensagemNoTopo(inativar.error) ? <p role="alert">{mensagemNoTopo(inativar.error)}</p> : null}
    </div>
  )
}
