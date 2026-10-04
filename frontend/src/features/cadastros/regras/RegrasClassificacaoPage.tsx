import { useState } from 'react'
import type { ApiError, Fase, RegraClassificacaoResponse } from '../../../api/types'
import { calcularRegua, LIMITE_REGUA, type FaixaEdicao, type SegmentoRegua } from './reguaClassificacao'
import { useRegrasClassificacao, useSubstituirRegras } from './useRegrasClassificacao'

const SERIES = [1, 2, 3, 4, 5]
const FASES: Fase[] = ['PRE_LEITOR', 'LEITOR_INICIANTE', 'LEITOR_FLUENTE']
const COR_FASE: Record<Fase, string> = {
  PRE_LEITOR: '#e0a458',
  LEITOR_INICIANTE: '#4f9fd6',
  LEITOR_FLUENTE: '#4caf7a',
}
const COR_LACUNA = '#d64545'
const COR_SOBREPOSICAO = '#b5179e'

function mensagemNoTopo(error: ApiError | null | undefined): string | null {
  if (!error) return null
  if (error.errors?.length) return error.errors.map((item) => item.message).join('; ')
  return error.detail ?? error.code ?? 'Não foi possível salvar'
}

function rotulo(segmento: SegmentoRegua): string {
  const intervalo = `${segmento.inicio}–${segmento.fim} acertos`
  if (segmento.tipo === 'lacuna') return `Lacuna: ${intervalo}`
  if (segmento.tipo === 'sobreposicao') return `Sobreposição: ${intervalo}`
  return `${segmento.fase}${segmento.nivel ? ` (nível ${segmento.nivel})` : ''}: ${intervalo}`
}

function corDe(segmento: SegmentoRegua): string {
  if (segmento.tipo === 'lacuna') return COR_LACUNA
  if (segmento.tipo === 'sobreposicao') return COR_SOBREPOSICAO
  return COR_FASE[segmento.fase]
}

function paraEdicao(regras: RegraClassificacaoResponse[]): FaixaEdicao[] {
  return [...regras]
    .sort((a, b) => a.quantidadeMinimaAcertos - b.quantidadeMinimaAcertos)
    .map((regra) => ({
      min: regra.quantidadeMinimaAcertos,
      max: regra.quantidadeMaximaAcertos,
      fase: regra.fase,
      nivel: regra.nivel,
    }))
}

function EditorFaixas({ serie, inicial }: { serie: number; inicial: FaixaEdicao[] }) {
  const [faixas, setFaixas] = useState<FaixaEdicao[]>(inicial)
  const [salvo, setSalvo] = useState(false)
  const substituir = useSubstituirRegras(serie)
  const regua = calcularRegua(faixas)
  const topo = mensagemNoTopo(substituir.error)

  function atualizar(indice: number, parcial: Partial<FaixaEdicao>): void {
    setSalvo(false)
    setFaixas((atuais) => atuais.map((faixa, i) => (i === indice ? { ...faixa, ...parcial } : faixa)))
  }

  function salvar(): void {
    setSalvo(false)
    substituir.mutate(
      faixas.map((faixa) => ({
        quantidadeMinimaAcertos: faixa.min,
        quantidadeMaximaAcertos: faixa.max,
        fase: faixa.fase,
        nivel: faixa.fase === 'PRE_LEITOR' ? faixa.nivel : null,
      })),
      { onSuccess: () => setSalvo(true) },
    )
  }

  return (
    <div>
      {topo ? <p role="alert">{topo}</p> : null}
      {salvo ? <p role="status">Salvo com sucesso</p> : null}

      <table aria-label="Faixas de classificação">
        <thead>
          <tr>
            <th>Mínimo</th>
            <th>Máximo (vazio = sem limite)</th>
            <th>Fase</th>
            <th>Nível</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {faixas.map((faixa, indice) => (
            <tr key={indice}>
              <td>
                <input
                  type="number"
                  aria-label={`Mínimo da faixa ${indice + 1}`}
                  value={faixa.min}
                  onChange={(event) => atualizar(indice, { min: Number(event.target.value) })}
                />
              </td>
              <td>
                <input
                  type="number"
                  aria-label={`Máximo da faixa ${indice + 1}`}
                  value={faixa.max ?? ''}
                  onChange={(event) =>
                    atualizar(indice, { max: event.target.value === '' ? null : Number(event.target.value) })
                  }
                />
              </td>
              <td>
                <select
                  aria-label={`Fase da faixa ${indice + 1}`}
                  value={faixa.fase}
                  onChange={(event) => atualizar(indice, { fase: event.target.value as Fase })}
                >
                  {FASES.map((fase) => (
                    <option key={fase} value={fase}>
                      {fase}
                    </option>
                  ))}
                </select>
              </td>
              <td>
                {faixa.fase === 'PRE_LEITOR' ? (
                  <input
                    type="number"
                    min={1}
                    max={4}
                    aria-label={`Nível da faixa ${indice + 1}`}
                    value={faixa.nivel ?? ''}
                    onChange={(event) =>
                      atualizar(indice, { nivel: event.target.value === '' ? null : Number(event.target.value) })
                    }
                  />
                ) : (
                  '—'
                )}
              </td>
              <td>
                <button
                  type="button"
                  className="danger"
                  onClick={() => {
                    setSalvo(false)
                    setFaixas((atuais) => atuais.filter((_, i) => i !== indice))
                  }}
                >
                  Remover faixa {indice + 1}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <button
        type="button"
        onClick={() => {
          setSalvo(false)
          setFaixas((atuais) => [...atuais, { min: (atuais.at(-1)?.max ?? LIMITE_REGUA) + 1, max: null, fase: 'LEITOR_FLUENTE', nivel: null }])
        }}
      >
        Adicionar faixa
      </button>

      <h2>Régua de 0 a {LIMITE_REGUA} acertos</h2>
      <div role="img" aria-label="Régua de classificação" style={{ display: 'flex', height: 28, border: '1px solid #888' }}>
        {regua.segmentos.map((segmento, indice) => (
          <div
            key={indice}
            title={rotulo(segmento)}
            style={{ flexGrow: segmento.fim - segmento.inicio + 1, flexBasis: 0, background: corDe(segmento) }}
          />
        ))}
      </div>
      <ul aria-label="Legenda da régua">
        {regua.segmentos.map((segmento, indice) => (
          <li key={indice}>
            <span
              aria-hidden="true"
              style={{ display: 'inline-block', width: 12, height: 12, marginRight: 8, background: corDe(segmento) }}
            />
            {rotulo(segmento)}
          </li>
        ))}
      </ul>
      {regua.temLacuna ? <p role="note">Há lacunas na régua: acertos sem faixa.</p> : null}
      {regua.temSobreposicao ? <p role="note">Há sobreposição entre faixas.</p> : null}

      <button type="button" disabled={substituir.isPending} onClick={salvar}>
        Salvar faixas
      </button>
    </div>
  )
}

/**
 * Regras de classificação por série (spec.md P2, FE-27): edição das faixas
 * com pré-visualização da régua 0-60 e destaque de lacunas/sobreposições
 * antes do envio (não bloqueia - o backend valida; 409/422 aparecem no topo).
 */
export function RegrasClassificacaoPage() {
  const [serie, setSerie] = useState(1)
  const regrasQuery = useRegrasClassificacao(serie)

  return (
    <div>
      <h1>Regras de classificação</h1>
      <label htmlFor="regras-serie">Série</label>
      <select id="regras-serie" value={serie} onChange={(event) => setSerie(Number(event.target.value))}>
        {SERIES.map((valor) => (
          <option key={valor} value={valor}>
            {valor}º ano
          </option>
        ))}
      </select>

      {regrasQuery.data ? (
        // `key` só pela série: o refetch pós-salvar não pode remontar o editor (perderia o aviso de sucesso).
        <EditorFaixas key={serie} serie={serie} inicial={paraEdicao(regrasQuery.data)} />
      ) : (
        <p>Carregando faixas…</p>
      )}
    </div>
  )
}
