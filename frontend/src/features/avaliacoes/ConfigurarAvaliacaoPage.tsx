import { useMemo, useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { request } from '../../api/client'
import type {
  ApiError,
  AlunoBuscaItem,
  HistoricoAvaliacaoItem,
  NovaAvaliacaoRequest,
  Page,
  TipoLeituraCodigo,
} from '../../api/types'
import { calcularCicloAtual } from '../alunos/cicloAtual'
import { useCriarAvaliacao } from './useCriarAvaliacao'
import { useListasPalavras } from './useListasPalavras'

const TEMPO_PADRAO_SEGUNDOS = 60
const OPCAO_DIGITAR = 'digitar'

const TIPOS_LEITURA: { codigo: TipoLeituraCodigo; label: string }[] = [
  { codigo: 'PALAVRA', label: 'Palavra' },
  { codigo: 'PSEUDOPALAVRA', label: 'Pseudopalavra' },
  { codigo: 'TEXTO_CURTO', label: 'Texto curto' },
]

/** `/api/v1/ciclos` (domínio fixo): só `id`/`codigo` são usados aqui, para mapear o ciclo atual (T9) num `cicloId` numérico. */
interface CicloDominio {
  id: number
  codigo: string
}

interface ConfiguracaoAvaliacaoLimites {
  quantidadeMinima: number
  quantidadeMaxima: number
}

/**
 * `GET /anos-letivos/ativo/configuracoes/{serie}` (backend, adicionado para
 * esta task - resolve o ano letivo ATIVO no servidor, aberto a
 * PROFESSOR+COORDENADOR): limites de palavras da série no ano ATIVO
 * (spec.md P1 "Configurar avaliação", AC3).
 */
export function useConfiguracaoAvaliacao(serie: number | undefined) {
  return useQuery({
    queryKey: ['configuracao-avaliacao', serie],
    queryFn: () => request<ConfiguracaoAvaliacaoLimites>(`/anos-letivos/ativo/configuracoes/${serie}`),
    enabled: serie !== undefined,
  })
}

export function useCiclosDominio() {
  return useQuery({
    queryKey: ['ciclos'],
    queryFn: () => request<CicloDominio[]>('/ciclos'),
  })
}

/** `GET /alunos/{id}` + `GET /alunos/{id}/historico-avaliacoes` - só o necessário para os defaults (série, ciclo atual via T9), sem a evolução de T10 (fora do escopo desta tela). */
function useDadosDoAluno(alunoId: number | undefined) {
  const alunoQuery = useQuery({
    queryKey: ['config-avaliacao', 'aluno', alunoId],
    queryFn: () => request<AlunoBuscaItem>(`/alunos/${alunoId}`),
    enabled: alunoId !== undefined,
  })
  const historicoQuery = useQuery({
    queryKey: ['config-avaliacao', 'historico', alunoId],
    queryFn: () => request<Page<HistoricoAvaliacaoItem>>(`/alunos/${alunoId}/historico-avaliacoes?page=0`),
    enabled: alunoId !== undefined,
  })

  const cicloAtualCodigo = calcularCicloAtual(historicoQuery.data?.content ?? [])

  return {
    serie: alunoQuery.data?.serie,
    cicloAtualCodigo,
    isLoading: alunoQuery.isLoading || historicoQuery.isLoading,
  }
}

function hojeISO(): string {
  const agora = new Date()
  const ano = agora.getFullYear()
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  const dia = String(agora.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

function contarPalavras(texto: string): string[] {
  return texto
    .split(/\s+/)
    .map((palavra) => palavra.trim())
    .filter(Boolean)
}

function mensagensPorCampo(error: ApiError | null | undefined): Record<string, string> {
  if (!error?.errors) return {}
  return Object.fromEntries(error.errors.map((item) => [item.field, item.message]))
}

/**
 * Formulário de configuração da avaliação (spec.md P1 "Configurar
 * avaliação", AC1-AC5): `alunoId` vem de `?alunoId=` (navegado a partir de
 * `ResumoAlunoPanel`, T11). Ao criar com sucesso, navega para
 * `/avaliacoes/{id}/executar` (rota adicionada em T23).
 */
export function ConfigurarAvaliacaoPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const alunoIdParam = searchParams.get('alunoId')
  const alunoId = alunoIdParam ? Number(alunoIdParam) : undefined

  const { serie, cicloAtualCodigo, isLoading: alunoCarregando } = useDadosDoAluno(alunoId)
  const ciclosQuery = useCiclosDominio()
  const configuracaoQuery = useConfiguracaoAvaliacao(serie)

  const [tipoLeitura, setTipoLeitura] = useState<TipoLeituraCodigo | ''>('')
  const [cicloIdManual, setCicloIdManual] = useState<number | ''>('')
  const [data, setData] = useState(() => hojeISO())
  const [tempo, setTempo] = useState(TEMPO_PADRAO_SEGUNDOS)
  const [fonte, setFonte] = useState<string>(OPCAO_DIGITAR)
  const [palavrasDigitadas, setPalavrasDigitadas] = useState('')
  const [textoLivre, setTextoLivre] = useState('')

  const listasQuery = useListasPalavras(serie, tipoLeitura || undefined)
  const criarAvaliacao = useCriarAvaliacao()
  const erros = mensagensPorCampo(criarAvaliacao.error)

  // AC1: `ciclo` derivado do ciclo atual (T9) assim que a lista de ciclos do
  // domínio fixo carrega - valor puro calculado no render (sem efeito +
  // setState), até o professor escolher manualmente outro ciclo.
  const cicloIdPadrao = useMemo(() => {
    const encontrado = ciclosQuery.data?.find((item) => item.codigo === cicloAtualCodigo)
    return encontrado ? encontrado.id : ('' as const)
  }, [ciclosQuery.data, cicloAtualCodigo])
  const cicloId = cicloIdManual !== '' ? cicloIdManual : cicloIdPadrao

  const digitandoTexto = tipoLeitura === 'TEXTO_CURTO'
  const digitandoPalavras = fonte === OPCAO_DIGITAR && tipoLeitura !== '' && !digitandoTexto
  const palavrasArray = useMemo(() => contarPalavras(palavrasDigitadas), [palavrasDigitadas])
  const quantidadePalavras = palavrasArray.length

  const limites = configuracaoQuery.data
  const foraDoIntervalo =
    digitandoPalavras && !!limites &&
    (quantidadePalavras < limites.quantidadeMinima || quantidadePalavras > limites.quantidadeMaxima)

  const fonteValida =
    fonte === OPCAO_DIGITAR
      ? digitandoTexto
        ? textoLivre.trim().length > 0
        : quantidadePalavras > 0 && !foraDoIntervalo
      : fonte !== ''

  const podeCriar =
    alunoId !== undefined &&
    tipoLeitura !== '' &&
    cicloId !== '' &&
    data !== '' &&
    tempo > 0 &&
    fonteValida &&
    !criarAvaliacao.isPending

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    if (alunoId === undefined || tipoLeitura === '' || cicloId === '') return
    if (!podeCriar) return

    const payload: NovaAvaliacaoRequest = {
      alunoId,
      tipoLeitura,
      cicloId,
      dataAvaliacao: data,
      tempoSegundos: tempo,
      ...(fonte !== OPCAO_DIGITAR ? { listaPalavrasId: Number(fonte) } : {}),
      ...(fonte === OPCAO_DIGITAR && digitandoTexto ? { texto: textoLivre } : {}),
      ...(fonte === OPCAO_DIGITAR && !digitandoTexto
        ? { palavras: palavrasArray.map((palavra) => ({ palavra, tipoPalavra: 'CANONICA' })) }
        : {}),
    }

    const avaliacao = await criarAvaliacao.mutateAsync(payload)
    navigate(`/avaliacoes/${avaliacao.id}/executar`)
  }

  return (
    <div>
      <h1>Configurar avaliação</h1>
      {alunoCarregando ? <p>Carregando dados do aluno…</p> : null}

      <form onSubmit={(event) => void handleSubmit(event)}>
        {criarAvaliacao.error && !criarAvaliacao.error.errors ? (
          <p role="alert">
            {criarAvaliacao.error.status === 403
              ? 'Seu perfil não pode criar avaliações.'
              : (criarAvaliacao.error.detail ?? 'Não foi possível criar a avaliação')}
          </p>
        ) : null}
        <label htmlFor="config-avaliacao-tipo-leitura">Tipo de leitura</label>
        <select
          id="config-avaliacao-tipo-leitura"
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

        <label htmlFor="config-avaliacao-ciclo">Ciclo</label>
        <select
          id="config-avaliacao-ciclo"
          value={cicloId}
          onChange={(event) => setCicloIdManual(event.target.value ? Number(event.target.value) : '')}
        >
          <option value="">Selecione</option>
          {(ciclosQuery.data ?? []).map((ciclo) => (
            <option key={ciclo.id} value={ciclo.id}>
              {ciclo.codigo}
            </option>
          ))}
        </select>
        {erros.cicloId ? <p>{erros.cicloId}</p> : null}

        <label htmlFor="config-avaliacao-data">Data</label>
        <input
          id="config-avaliacao-data"
          type="date"
          value={data}
          onChange={(event) => setData(event.target.value)}
        />
        {erros.dataAvaliacao ? <p>{erros.dataAvaliacao}</p> : null}

        <label htmlFor="config-avaliacao-tempo">Tempo (segundos)</label>
        <input
          id="config-avaliacao-tempo"
          type="number"
          value={tempo}
          onChange={(event) => setTempo(Number(event.target.value))}
        />
        {erros.tempoSegundos ? <p>{erros.tempoSegundos}</p> : null}

        {tipoLeitura ? (
          <>
            <label htmlFor="config-avaliacao-fonte">Palavras</label>
            <select
              id="config-avaliacao-fonte"
              value={fonte}
              onChange={(event) => setFonte(event.target.value)}
            >
              <option value={OPCAO_DIGITAR}>{digitandoTexto ? 'Digitar texto' : 'Digitar palavras'}</option>
              {(listasQuery.data ?? []).map((lista) => (
                <option key={lista.id} value={lista.id}>
                  {lista.nome} ({lista.quantidadePalavras} palavras)
                </option>
              ))}
            </select>
            {erros.listaPalavrasId ? <p>{erros.listaPalavrasId}</p> : null}

            {fonte === OPCAO_DIGITAR && digitandoTexto ? (
              <>
                <label htmlFor="config-avaliacao-texto">Texto</label>
                <textarea
                  id="config-avaliacao-texto"
                  value={textoLivre}
                  onChange={(event) => setTextoLivre(event.target.value)}
                />
                {erros.texto ? <p>{erros.texto}</p> : null}
              </>
            ) : null}

            {fonte === OPCAO_DIGITAR && !digitandoTexto ? (
              <>
                <label htmlFor="config-avaliacao-palavras">Palavras (separadas por espaço)</label>
                <textarea
                  id="config-avaliacao-palavras"
                  value={palavrasDigitadas}
                  onChange={(event) => setPalavrasDigitadas(event.target.value)}
                />
                {limites ? (
                  <p>
                    {quantidadePalavras} palavras (mín. {limites.quantidadeMinima}, máx.{' '}
                    {limites.quantidadeMaxima})
                  </p>
                ) : null}
                {erros.palavras ? <p>{erros.palavras}</p> : null}
              </>
            ) : null}
          </>
        ) : null}

        <button type="submit" disabled={!podeCriar}>
          Criar avaliação
        </button>
      </form>
    </div>
  )
}
