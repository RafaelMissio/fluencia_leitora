import { useEffect, useRef } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { request } from '../../api/client'
import type { AvaliacaoResponse, StatusPalavra } from '../../api/types'
import { GradePalavras } from '../../components/GradePalavras'
import { CronometroDisplay } from './CronometroDisplay'
import { useAvaliacaoExecucao, type BotaoExecucao } from './useAvaliacaoExecucao'

const ROTULO_BOTAO: Record<BotaoExecucao, string> = {
  iniciar: 'Iniciar avaliação',
  pausar: 'Pausar',
  continuar: 'Continuar',
  resetar: 'Resetar',
  finalizar: 'Finalizar',
}

const MENSAGEM_INTERROMPIDA = 'A gravação anterior foi interrompida'
const MENSAGEM_CONFIRMAR_RESET = 'Tem certeza que deseja resetar a avaliação? A gravação atual será descartada.'

/**
 * Compõe `useAvaliacaoExecucao` (T16-T18) + `CronometroDisplay` (T19) +
 * `GradePalavras` (T20) na tela do professor (design.md, Components).
 * `onMarcar` chama `PUT /avaliacoes/{id}/palavras/{ordem}` via `apiClient`.
 * Ao finalizar (clique manual ou tempo zerado, disparado internamente pelo
 * hook), navega para `/avaliacoes/{id}/resultado` encaminhando `exec.
 * blobGravado` via estado de navegação - lido do estado do hook (não do
 * retorno de `finalizar()`) para que o caminho automático (tempo esgotado)
 * encaminhe o áudio tão bem quanto o clique manual (spec.md AC2/AC6).
 */
export function ExecutarAvaliacaoPage() {
  const { id } = useParams<{ id: string }>()
  const avaliacaoId = Number(id)
  const navigate = useNavigate()
  const exec = useAvaliacaoExecucao(avaliacaoId)

  const jaNavegouRef = useRef(false)

  useEffect(() => {
    if (exec.status === 'FINALIZADA' && !jaNavegouRef.current) {
      jaNavegouRef.current = true
      navigate(`/avaliacoes/${avaliacaoId}/resultado`, { state: { blob: exec.blobGravado } })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [exec.status, exec.blobGravado, avaliacaoId])

  function aoClicarResetar(): void {
    const confirmado = window.confirm(MENSAGEM_CONFIRMAR_RESET)
    void exec.resetar(confirmado)
  }

  async function aoMarcar(ordem: number, novoStatus: StatusPalavra): Promise<void> {
    await request<AvaliacaoResponse>(`/avaliacoes/${avaliacaoId}/palavras/${ordem}`, {
      method: 'PUT',
      body: JSON.stringify({ status: novoStatus }),
    })
  }

  // spec.md, Edge Cases: página recarregada EM_ANDAMENTO oferece só Resetar/Finalizar (FE-24) -
  // `botoesHabilitados` de EM_ANDAMENTO inclui "pausar", que não se aplica aqui (a gravação em si já se perdeu).
  const botoesExibidos = exec.interrompida
    ? exec.botoesHabilitados.filter((botao) => botao !== 'pausar')
    : exec.botoesHabilitados

  function aoClicarBotao(botao: BotaoExecucao): void {
    switch (botao) {
      case 'iniciar':
        void exec.iniciar()
        break
      case 'pausar':
        void exec.pausar()
        break
      case 'continuar':
        void exec.continuar()
        break
      case 'resetar':
        aoClicarResetar()
        break
      case 'finalizar':
        void exec.finalizar()
        break
    }
  }

  return (
    <div>
      <h1>Executar avaliação</h1>

      {exec.erroMicrofone && <p role="alert">{exec.erroMicrofone}</p>}
      {exec.interrompida && <p>{MENSAGEM_INTERROMPIDA}</p>}

      <CronometroDisplay tempoRestanteMs={exec.tempoRestanteMs} gravando={exec.gravando} />

      <div>
        {botoesExibidos.map((botao) => (
          <button key={botao} type="button" onClick={() => aoClicarBotao(botao)}>
            {ROTULO_BOTAO[botao]}
          </button>
        ))}
      </div>

      {exec.status && <GradePalavras palavras={exec.palavras} avaliacaoStatus={exec.status} onMarcar={aoMarcar} />}
    </div>
  )
}
