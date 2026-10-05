import { useMemo, useState, type FormEvent } from 'react'
import type {
  ApiError,
  NovaAvaliacaoProgramadaRequest,
  TipoLeituraCodigo,
} from '../../../api/types'
import {
  useCiclosDominio,
  useConfiguracaoAvaliacao,
} from '../../avaliacoes/ConfigurarAvaliacaoPage'
import { useAnosLetivos } from '../anosletivos/useAnosLetivos'
import { useListasPalavras } from '../../avaliacoes/useListasPalavras'
import {
  useAvaliacoesProgramadas,
  useCriarAvaliacaoProgramada,
  useRemoverAvaliacaoProgramada,
} from './useAvaliacoesProgramadas'

const SERIES = [1, 2, 3, 4, 5]
const OPCAO_DIGITAR = 'digitar'
const TIPOS_LEITURA: { codigo: TipoLeituraCodigo; label: string }[] = [
  { codigo: 'PALAVRA', label: 'Palavra' },
  { codigo: 'PSEUDOPALAVRA', label: 'Pseudopalavra' },
  { codigo: 'TEXTO_CURTO', label: 'Texto curto' },
]

function rotuloTipo(tipo: TipoLeituraCodigo): string {
  return TIPOS_LEITURA.find((item) => item.codigo === tipo)?.label ?? tipo
}

function contarPalavras(texto: string): number {
  return texto.split(/\s+/).filter(Boolean).length
}

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

/**
 * Tela do coordenador: configura as avaliações de cada série. Cada uma
 * aparece como pendente na tela de todos os alunos da série (ano letivo ativo).
 */
export function ProgramarAvaliacoesPage({ modo }: { modo: 'cadastrar' | 'buscar' }) {
  const anosQuery = useAnosLetivos()
  const [anoLetivoId, setAnoLetivoId] = useState<number | ''>('')
  const anos = anosQuery.data ?? []
  const anoSelecionado = anoLetivoId !== '' ? anoLetivoId : (anos.find((a) => a.ativo)?.id ?? '')
  const programadasQuery = useAvaliacoesProgramadas(
    anoSelecionado === '' ? undefined : anoSelecionado,
  )
  const ciclosQuery = useCiclosDominio()
  const criar = useCriarAvaliacaoProgramada()
  const remover = useRemoverAvaliacaoProgramada()

  const [nome, setNome] = useState('')
  const [serie, setSerie] = useState(1)
  const [tipoLeitura, setTipoLeitura] = useState<TipoLeituraCodigo | ''>('')
  const [cicloId, setCicloId] = useState<number | ''>('')
  const [tempo, setTempo] = useState(60)
  const [maxRefazeres, setMaxRefazeres] = useState(3)
  const [fonte, setFonte] = useState(OPCAO_DIGITAR)
  const [palavras, setPalavras] = useState('')
  const [texto, setTexto] = useState('')
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroSerie, setFiltroSerie] = useState<number | ''>('')

  const listasQuery = useListasPalavras(serie, tipoLeitura || undefined)
  const limites = useConfiguracaoAvaliacao(serie).data
  const erros = mensagensPorCampo(criar.error)

  const digitandoTexto = tipoLeitura === 'TEXTO_CURTO'
  const digitando = fonte === OPCAO_DIGITAR
  const quantidade = useMemo(() => contarPalavras(palavras), [palavras])

  const conteudoValido = !digitando
    ? true
    : digitandoTexto
      ? texto.trim().length > 0
      : quantidade > 0
  const podeSalvar =
    nome.trim() !== '' &&
    tipoLeitura !== '' &&
    cicloId !== '' &&
    tempo > 0 &&
    conteudoValido &&
    !criar.isPending

  async function salvar(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    if (tipoLeitura === '' || cicloId === '' || !podeSalvar) return
    const payload: NovaAvaliacaoProgramadaRequest = {
      ...(anoSelecionado !== '' ? { anoLetivoId: anoSelecionado } : {}),
      nome: nome.trim(),
      serie,
      tipoLeitura,
      cicloId,
      tempoSegundos: tempo,
      maxRefazeres,
      ...(!digitando ? { listaPalavrasId: Number(fonte) } : {}),
      ...(digitando && digitandoTexto ? { texto } : {}),
      ...(digitando && !digitandoTexto ? { palavras } : {}),
    }
    await criar.mutateAsync(payload)
    setNome('')
    setPalavras('')
    setTexto('')
    setFonte(OPCAO_DIGITAR)
  }

  const termo = filtroNome.trim().toLowerCase()
  const programadas = (programadasQuery.data ?? []).filter(
    (p) =>
      (filtroSerie === '' || p.serie === filtroSerie) &&
      (termo === '' || p.nome.toLowerCase().includes(termo)),
  )

  const seletorAno = (
    <>
      <label htmlFor="prog-ano">Ano letivo</label>
      <select
        id="prog-ano"
        value={anoSelecionado}
        onChange={(event) => setAnoLetivoId(event.target.value ? Number(event.target.value) : '')}
      >
        {anos.map((ano) => (
          <option key={ano.id} value={ano.id}>
            {ano.ano}
            {ano.ativo ? ' (ativo)' : ''}
          </option>
        ))}
      </select>
    </>
  )

  return (
    <div>
      <h1>{modo === 'cadastrar' ? 'Cadastrar avaliação' : 'Buscar avaliações'}</h1>
      <p>
        As avaliações configuradas aqui aparecem como pendentes para todos os alunos da série, no
        ano letivo ativo.
      </p>

      {modo === 'cadastrar' ? (
        <form onSubmit={(event) => void salvar(event)}>
          {criar.error && !criar.error.errors ? (
            <p role="alert">{criar.error.detail ?? 'Não foi possível salvar a avaliação'}</p>
          ) : null}

          {seletorAno}

          <label htmlFor="prog-nome">Nome da avaliação</label>
          <input
            id="prog-nome"
            type="text"
            maxLength={150}
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
          {erros.nome ? <p>{erros.nome}</p> : null}

          <label htmlFor="prog-serie">Série</label>
          <select
            id="prog-serie"
            value={serie}
            onChange={(event) => {
              setSerie(Number(event.target.value))
              setFonte(OPCAO_DIGITAR)
            }}
          >
            {SERIES.map((item) => (
              <option key={item} value={item}>
                {item}º ano
              </option>
            ))}
          </select>
          {erros.serie ? <p>{erros.serie}</p> : null}

          <label htmlFor="prog-tipo">Tipo de leitura</label>
          <select
            id="prog-tipo"
            value={tipoLeitura}
            onChange={(event) => {
              setTipoLeitura(event.target.value as TipoLeituraCodigo | '')
              setFonte(OPCAO_DIGITAR)
            }}
          >
            <option value="">Selecione</option>
            {TIPOS_LEITURA.map((tipo) => (
              <option key={tipo.codigo} value={tipo.codigo}>
                {tipo.label}
              </option>
            ))}
          </select>
          {erros.tipoLeitura ? <p>{erros.tipoLeitura}</p> : null}

          <label htmlFor="prog-ciclo">Ciclo</label>
          <select
            id="prog-ciclo"
            value={cicloId}
            onChange={(event) => setCicloId(event.target.value ? Number(event.target.value) : '')}
          >
            <option value="">Selecione</option>
            {(ciclosQuery.data ?? []).map((ciclo) => (
              <option key={ciclo.id} value={ciclo.id}>
                {ciclo.codigo}
              </option>
            ))}
          </select>
          {erros.cicloId ? <p>{erros.cicloId}</p> : null}

          <label htmlFor="prog-tempo">Tempo (segundos)</label>
          <input
            id="prog-tempo"
            type="number"
            value={tempo}
            onChange={(event) => setTempo(Number(event.target.value))}
          />
          {erros.tempoSegundos ? <p>{erros.tempoSegundos}</p> : null}

          <label htmlFor="prog-max-refazeres">Quantas vezes pode ser refeita</label>
          <input
            id="prog-max-refazeres"
            type="number"
            min={0}
            max={20}
            value={maxRefazeres}
            onChange={(event) => setMaxRefazeres(Number(event.target.value))}
          />
          {erros.maxRefazeres ? <p>{erros.maxRefazeres}</p> : null}

          {tipoLeitura ? (
            <>
              <label htmlFor="prog-fonte">Palavras</label>
              <select
                id="prog-fonte"
                value={fonte}
                onChange={(event) => setFonte(event.target.value)}
              >
                <option value={OPCAO_DIGITAR}>
                  {digitandoTexto ? 'Digitar texto' : 'Digitar palavras'}
                </option>
                {(listasQuery.data ?? []).map((lista) => (
                  <option key={lista.id} value={lista.id}>
                    {lista.nome} ({lista.quantidadePalavras} palavras)
                  </option>
                ))}
              </select>

              {digitando && digitandoTexto ? (
                <>
                  <label htmlFor="prog-texto">Texto</label>
                  <textarea
                    id="prog-texto"
                    value={texto}
                    onChange={(event) => setTexto(event.target.value)}
                  />
                </>
              ) : null}
              {digitando && !digitandoTexto ? (
                <>
                  <label htmlFor="prog-palavras">Palavras (separadas por espaço)</label>
                  <textarea
                    id="prog-palavras"
                    value={palavras}
                    onChange={(event) => setPalavras(event.target.value)}
                  />
                  {limites ? (
                    <p>
                      {quantidade} palavras (mín. {limites.quantidadeMinima}, máx.{' '}
                      {limites.quantidadeMaxima})
                    </p>
                  ) : null}
                </>
              ) : null}
              {erros.palavras ? <p>{erros.palavras}</p> : null}
              {erros.texto ? <p>{erros.texto}</p> : null}
            </>
          ) : null}

          <button type="submit" className="primary" disabled={!podeSalvar}>
            Configurar avaliação
          </button>
          {criar.isSuccess ? <p role="status">Avaliação configurada</p> : null}
        </form>
      ) : (
        <>
          {seletorAno}

          <label htmlFor="prog-filtro-nome">Nome</label>
          <input
            id="prog-filtro-nome"
            type="search"
            value={filtroNome}
            onChange={(event) => setFiltroNome(event.target.value)}
          />

          <label htmlFor="prog-filtro-serie">Série</label>
          <select
            id="prog-filtro-serie"
            value={filtroSerie}
            onChange={(event) =>
              setFiltroSerie(event.target.value ? Number(event.target.value) : '')
            }
          >
            <option value="">Todas</option>
            {SERIES.map((item) => (
              <option key={item} value={item}>
                {item}º ano
              </option>
            ))}
          </select>

          <h2>Configuradas</h2>
          {programadasQuery.isLoading ? <p>Carregando…</p> : null}
          {!programadasQuery.isLoading && programadas.length === 0 ? (
            <p>Nenhuma avaliação configurada.</p>
          ) : null}
          {SERIES.filter((item) => programadas.some((p) => p.serie === item)).map((item) => (
            <section key={item}>
              <h3>{item}º ano</h3>
              <ul>
                {programadas
                  .filter((p) => p.serie === item)
                  .map((p) => (
                    <li key={p.id}>
                      {p.nome} - {rotuloTipo(p.tipoLeitura)} - {p.tempoSegundos}s - refazer até {p.maxRefazeres}x{' '}
                      <button
                        type="button"
                        disabled={remover.isPending}
                        onClick={() => remover.mutate(p.id)}
                        aria-label={`Remover ${p.nome} do ${item}º ano`}
                      >
                        Remover
                      </button>
                    </li>
                  ))}
              </ul>
            </section>
          ))}
        </>
      )}
    </div>
  )
}
