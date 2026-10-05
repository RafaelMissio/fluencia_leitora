import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { request } from '../../api/client'
import type { AvaliacaoResponse } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useEnvioAudio } from './useEnvioAudio'
import { useRefazerAvaliacao } from './useRefazerAvaliacao'

const MENSAGEM_AUDIO_NAO_ENVIADO = 'O áudio não foi enviado'
const MENSAGEM_CLASSIFICACAO_PENDENTE = 'Classificação pendente: nenhuma regra cobre este resultado'

/**
 * `GET /avaliacoes/{id}/audio` exige `Authorization: Bearer` (AD-009); um
 * `<audio src>`/`<a href>` nativo não envia esse header. Busca os bytes com
 * `fetch` autenticado e expõe um Object URL para o player e o download
 * usarem, revogado ao trocar/desmontar.
 *
 * SPEC_DEVIATION: `AvaliacaoController.baixarAudio` (backend) não lê nenhum
 * `download` query param nem define `Content-Disposition` - `?download=true`
 * não tem efeito no servidor hoje. O botão "Baixar áudio" ainda funciona
 * corretamente porque o download é forçado no cliente (atributo HTML
 * `download` do link, com o Object URL já obtido para o player), não pelo
 * servidor; a URL requisitada mantém `?download=true` (Done when desta task),
 * mesmo sendo ignorada pelo backend.
 */
function useAudioObjectUrl(avaliacaoId: number, ativo: boolean, token: string | null): string | null {
  const [url, setUrl] = useState<string | null>(null)

  useEffect(() => {
    // `ativo === false` não limpa `url` sincronamente: quem usa este hook já
    // condiciona a exibição do player a `ativo` (`audioDisponivel`), então um
    // valor antigo aqui não vaza para a UI - evita um `setState` síncrono
    // direto no corpo do efeito.
    if (!ativo) return

    let objectUrl: string | null = null
    let cancelado = false
    fetch(`/api/v1/avaliacoes/${avaliacaoId}/audio?download=true`, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
      .then((resposta) => resposta.blob())
      .then((blob) => {
        if (cancelado) return
        objectUrl = URL.createObjectURL(blob)
        setUrl(objectUrl)
      })

    return () => {
      cancelado = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [ativo, avaliacaoId, token])

  return url
}

/**
 * Painel de resultado (design.md, Components): total/lidas/corretas/
 * incorretas/não lidas/percentual/tempo/fase/nível (AC1), envio automático
 * do áudio com reenvio manual (AC2/AC3), player + download (AC4) e o aviso
 * de classificação pendente (AC5).
 */
export function ResultadoAvaliacaoPage() {
  const { id } = useParams<{ id: string }>()
  const avaliacaoId = Number(id)
  const location = useLocation()
  const blob = (location.state as { blob?: Blob } | null)?.blob ?? null
  const { token } = useAuth()
  const navigate = useNavigate()

  const avaliacaoQuery = useQuery({
    queryKey: ['avaliacao', avaliacaoId],
    queryFn: () => request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}`),
    enabled: Number.isFinite(avaliacaoId),
  })

  const envioAudio = useEnvioAudio(avaliacaoId, blob)
  const audioDisponivel = envioAudio.status === 'enviado'
  const audioUrl = useAudioObjectUrl(avaliacaoId, audioDisponivel, token)

  const avaliacao = avaliacaoQuery.data
  const refazer = useRefazerAvaliacao(avaliacao?.alunoId ?? 0)
  if (!avaliacao) {
    return <p>Carregando resultado…</p>
  }

  return (
    <div>
      <h1>Resultado da avaliação</h1>

      <dl>
        <dt>Total</dt>
        <dd>{avaliacao.quantidadeTotal}</dd>
        <dt>Lidas</dt>
        <dd>{avaliacao.quantidadeLidas}</dd>
        <dt>Corretas</dt>
        <dd>{avaliacao.quantidadeCorretas}</dd>
        <dt>Incorretas</dt>
        <dd>{avaliacao.quantidadeIncorretas}</dd>
        <dt>Não lidas</dt>
        <dd>{avaliacao.quantidadeNaoLidas}</dd>
        <dt>Percentual de acerto</dt>
        <dd>{avaliacao.percentualAcerto}%</dd>
        <dt>Tempo utilizado</dt>
        <dd>{avaliacao.tempoUtilizadoSegundos}s</dd>
        <dt>Fase</dt>
        <dd>{avaliacao.fase ?? '—'}</dd>
        <dt>Nível</dt>
        <dd>{avaliacao.nivel ?? '—'}</dd>
      </dl>

      {avaliacao.classificacaoPendente && <p>{MENSAGEM_CLASSIFICACAO_PENDENTE}</p>}

      {envioAudio.status === 'falhou' && (
        <div role="alert">
          <p>{MENSAGEM_AUDIO_NAO_ENVIADO}</p>
          <button type="button" onClick={() => void envioAudio.reenviar()}>
            Tentar enviar novamente
          </button>
        </div>
      )}

      {avaliacao.status === 'FINALIZADA' && avaliacao.ativa && avaliacao.podeRefazer !== false && (
        <div>
          <button
            type="button"
            disabled={refazer.isPending}
            onClick={() =>
              void refazer.mutateAsync(avaliacaoId).then(
                (nova) => navigate(`/avaliacoes/${nova.id}/executar?modo=refazer`),
                () => {},
              )
            }
          >
            Refazer avaliação
          </button>
          <p>Ao refazer, esta avaliação fica inativa e deixa de contar no histórico.</p>
          {refazer.error ? <p role="alert">{refazer.error.detail ?? 'Não foi possível refazer a avaliação'}</p> : null}
        </div>
      )}
      {avaliacao.status === 'FINALIZADA' && avaliacao.ativa && avaliacao.podeRefazer === false && (
        <p role="note">Limite de refazer atingido: esta avaliação já foi refeita {avaliacao.refeitas} de {avaliacao.maxRefazeres} vezes e não pode mais ser refeita.</p>
      )}
      {avaliacao.refeitas != null ? <p>Refeita {avaliacao.refeitas} de {avaliacao.maxRefazeres} vezes</p> : null}
      {!avaliacao.ativa && <p role="note">Esta avaliação foi refeita e está inativa.</p>}

      {audioDisponivel && audioUrl && (
        <div>
          <audio controls src={audioUrl} />
          <a href={audioUrl} download={`avaliacao-${avaliacaoId}.audio`}>
            Baixar áudio
          </a>
        </div>
      )}
    </div>
  )
}
