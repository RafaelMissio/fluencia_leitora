import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AvaliacaoResponse, StatusAvaliacao } from '../../api/types'
import { useAvaliacaoExecucao } from './useAvaliacaoExecucao'

vi.mock('../../media/recorder', () => ({
  isMediaRecorderSupported: vi.fn(),
  pickSupportedMimeType: vi.fn(),
  createRecorder: vi.fn(),
}))

import { createRecorder, isMediaRecorderSupported, pickSupportedMimeType } from '../../media/recorder'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function avaliacao(overrides: Partial<AvaliacaoResponse> = {}): AvaliacaoResponse {
  return {
    id: 1,
    alunoId: 1,
    professorId: 1,
    professorNome: 'Prof',
    turmaId: 1,
    turmaNome: 'Turma A',
    serie: 2,
    anoLetivoId: 1,
    cicloId: 1,
    tipoLeitura: 'PALAVRA',
    dataAvaliacao: '2026-09-29',
    tempoConfiguradoSegundos: 60,
    status: 'CRIADA',
    iniciadoEm: null,
    finalizadoEm: null,
    quantidadeTotal: 3,
    tempoUtilizadoSegundos: null,
    quantidadeCorretas: null,
    quantidadeIncorretas: null,
    quantidadeNaoLidas: null,
    quantidadeLidas: null,
    percentualAcerto: null,
    fase: null,
    nivel: null,
    classificacaoPendente: false,
    ativa: true,
    palavras: [
      { ordem: 1, palavra: 'casa', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
      { ordem: 2, palavra: 'bola', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
      { ordem: 3, palavra: 'pato', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
    ],
    ...overrides,
  }
}

const FAKE_STREAM = {
  getTracks: () => [{ stop: vi.fn() }],
} as unknown as MediaStream

function stubGetUserMedia(): ReturnType<typeof vi.fn> {
  const getUserMedia = vi.fn()
  Object.defineProperty(navigator, 'mediaDevices', {
    configurable: true,
    value: { getUserMedia },
  })
  return getUserMedia
}

describe('useAvaliacaoExecucao', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  async function esperarCarregamento(): Promise<ReturnType<typeof renderHook<ReturnType<typeof useAvaliacaoExecucao>, unknown>>> {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao()))
    const hook = renderHook(() => useAvaliacaoExecucao(1))
    await waitFor(() => expect(hook.result.current.status).toBe('CRIADA'))
    return hook
  }

  it('without MediaRecorder support, sets erroMicrofone and never calls the iniciar API (FE-25)', async () => {
    vi.mocked(isMediaRecorderSupported).mockReturnValue(false)
    const getUserMedia = stubGetUserMedia()
    const { result } = await esperarCarregamento()

    await act(async () => {
      await result.current.iniciar()
    })

    expect(result.current.erroMicrofone).toBe('Navegador sem suporte à gravação. Use Chrome, Edge, Firefox ou Safari 17+')
    expect(getUserMedia).not.toHaveBeenCalled()
    expect(vi.mocked(fetch)).not.toHaveBeenCalledWith(expect.stringContaining('/iniciar'), expect.anything())
    expect(result.current.status).toBe('CRIADA')
  })

  it('denied microphone sets erroMicrofone and never calls iniciar; status stays CRIADA (FE-12)', async () => {
    vi.mocked(isMediaRecorderSupported).mockReturnValue(true)
    const getUserMedia = stubGetUserMedia()
    getUserMedia.mockRejectedValueOnce(new DOMException('Permission denied', 'NotAllowedError'))
    const { result } = await esperarCarregamento()

    await act(async () => {
      await result.current.iniciar()
    })

    expect(getUserMedia).toHaveBeenCalledWith({ audio: true })
    expect(result.current.erroMicrofone).toBe('Permita o acesso ao microfone para iniciar a avaliação')
    expect(result.current.status).toBe('CRIADA')
    const chamouIniciar = vi
      .mocked(fetch)
      .mock.calls.some(([url]) => typeof url === 'string' && url.includes('/iniciar'))
    expect(chamouIniciar).toBe(false)
  })

  it('released microphone and iniciar returning 200 start recording and countdown together (FE-13)', async () => {
    vi.mocked(isMediaRecorderSupported).mockReturnValue(true)
    vi.mocked(pickSupportedMimeType).mockReturnValue('audio/webm;codecs=opus')
    const recorderHandle = { start: vi.fn(), pause: vi.fn(), resume: vi.fn(), stop: vi.fn().mockResolvedValue(new Blob()) }
    vi.mocked(createRecorder).mockReturnValue(recorderHandle)
    const getUserMedia = stubGetUserMedia()
    getUserMedia.mockResolvedValueOnce(FAKE_STREAM)
    const { result } = await esperarCarregamento()

    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'EM_ANDAMENTO' })))

    // Cronômetro depende de performance.now(); troca para timers falsos só
    // depois que o carregamento inicial (real, via waitFor) já terminou.
    let now = 0
    vi.useFakeTimers()
    vi.spyOn(performance, 'now').mockImplementation(() => now)

    await act(async () => {
      await result.current.iniciar()
    })

    expect(result.current.status).toBe('EM_ANDAMENTO')
    expect(result.current.gravando).toBe(true)
    expect(recorderHandle.start).toHaveBeenCalledTimes(1)

    const tempoAntes = result.current.tempoRestanteMs
    now += 1000
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000)
    })
    expect(result.current.tempoRestanteMs).toBeLessThan(tempoAntes)
  })

  it('mount with an avaliação already EM_ANDAMENTO on the server activates the interrompida flag (FE-24)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'EM_ANDAMENTO' })))

    const { result } = renderHook(() => useAvaliacaoExecucao(1))

    await waitFor(() => expect(result.current.status).toBe('EM_ANDAMENTO'))
    expect(result.current.interrompida).toBe(true)
    expect(result.current.gravando).toBe(false)
  })

  /** Leva o hook a EM_ANDAMENTO/gravando via `iniciar()`, com timers falsos já ativos (T17/T18 usam o cronômetro). */
  async function iniciarEmAndamento(
    tempoConfiguradoSegundos = 60,
  ): Promise<{
    result: Awaited<ReturnType<typeof esperarCarregamento>>['result']
    recorderHandle: { start: ReturnType<typeof vi.fn>; pause: ReturnType<typeof vi.fn>; resume: ReturnType<typeof vi.fn>; stop: ReturnType<typeof vi.fn> }
    now: { valor: number }
  }> {
    vi.mocked(isMediaRecorderSupported).mockReturnValue(true)
    vi.mocked(pickSupportedMimeType).mockReturnValue('audio/webm;codecs=opus')
    const recorderHandle = { start: vi.fn(), pause: vi.fn(), resume: vi.fn(), stop: vi.fn().mockResolvedValue(new Blob()) }
    vi.mocked(createRecorder).mockReturnValue(recorderHandle)
    const getUserMedia = stubGetUserMedia()
    getUserMedia.mockResolvedValueOnce(FAKE_STREAM)
    const { result } = await esperarCarregamento()

    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(200, avaliacao({ status: 'EM_ANDAMENTO', tempoConfiguradoSegundos })),
    )

    const now = { valor: 0 }
    vi.useFakeTimers()
    vi.spyOn(performance, 'now').mockImplementation(() => now.valor)

    await act(async () => {
      await result.current.iniciar()
    })

    return { result, recorderHandle, now }
  }

  describe('pausar / continuar / resetar (T17)', () => {
    it('pausar calls the API and pauses recording, stopping the countdown (spec.md AC4)', async () => {
      const { result, recorderHandle, now } = await iniciarEmAndamento()
      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'PAUSADA' })))

      await act(async () => {
        await result.current.pausar()
      })

      expect(result.current.status).toBe('PAUSADA')
      expect(result.current.gravando).toBe(false)
      expect(recorderHandle.pause).toHaveBeenCalledTimes(1)
      const [url, init] = vi.mocked(fetch).mock.calls.at(-1)!
      expect(url).toBe('/api/v1/avaliacoes/1/pausar')
      expect(init?.method).toBe('POST')

      const tempoAntes = result.current.tempoRestanteMs
      now.valor += 2000
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2000)
      })
      expect(result.current.tempoRestanteMs).toBe(tempoAntes)
    })

    it('continuar calls the API, resumes recording and the countdown (spec.md AC4)', async () => {
      const { result, recorderHandle, now } = await iniciarEmAndamento()
      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'PAUSADA' })))
      await act(async () => {
        await result.current.pausar()
      })

      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'EM_ANDAMENTO' })))
      await act(async () => {
        await result.current.continuar()
      })

      expect(result.current.status).toBe('EM_ANDAMENTO')
      expect(result.current.gravando).toBe(true)
      expect(recorderHandle.resume).toHaveBeenCalledTimes(1)
      const [url, init] = vi.mocked(fetch).mock.calls.at(-1)!
      expect(url).toBe('/api/v1/avaliacoes/1/continuar')
      expect(init?.method).toBe('POST')

      const tempoAntes = result.current.tempoRestanteMs
      now.valor += 1000
      await act(async () => {
        await vi.advanceTimersByTimeAsync(1000)
      })
      expect(result.current.tempoRestanteMs).toBeLessThan(tempoAntes)
    })

    it('resetar(true) calls the API, discards the recording, resets the countdown and all palavras go back to PENDENTE', async () => {
      const { result, recorderHandle } = await iniciarEmAndamento(60)
      const avaliacaoResetada = avaliacao({
        status: 'CRIADA',
        tempoConfiguradoSegundos: 60,
        palavras: [
          { ordem: 1, palavra: 'casa', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
          { ordem: 2, palavra: 'bola', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
          { ordem: 3, palavra: 'pato', tipoPalavra: 'CANONICA', status: 'PENDENTE' },
        ],
      })
      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacaoResetada))

      await act(async () => {
        await result.current.resetar(true)
      })

      expect(result.current.status).toBe('CRIADA')
      expect(result.current.tempoRestanteMs).toBe(60000)
      expect(result.current.palavras.every((palavra) => palavra.status === 'PENDENTE')).toBe(true)
      expect(recorderHandle.stop).toHaveBeenCalledTimes(1)
      const chamouResetar = vi
        .mocked(fetch)
        .mock.calls.some(([url]) => url === '/api/v1/avaliacoes/1/resetar')
      expect(chamouResetar).toBe(true)
    })

    it('resetar(false) does nothing: no API call, no state change', async () => {
      const { result } = await iniciarEmAndamento()
      const statusAntes = result.current.status
      const chamadasAntes = vi.mocked(fetch).mock.calls.length

      await act(async () => {
        await result.current.resetar(false)
      })

      expect(result.current.status).toBe(statusAntes)
      expect(vi.mocked(fetch).mock.calls.length).toBe(chamadasAntes)
    })
  })

  describe('finalizar, botoesHabilitados e resync em 409 (T18)', () => {
    it('the countdown reaching 0 calls finalizar with motivo TEMPO_ESGOTADO automatically (spec.md AC6)', async () => {
      const { result, now } = await iniciarEmAndamento(1)
      vi.mocked(fetch).mockResolvedValueOnce(
        jsonResponse(200, avaliacao({ status: 'FINALIZADA', tempoConfiguradoSegundos: 1 })),
      )

      now.valor += 1000
      await act(async () => {
        await vi.advanceTimersByTimeAsync(1000)
      })
      // Segunda passagem para deixar o `finalizar()` disparado pelo efeito (assíncrono) resolver.
      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })

      const chamouFinalizar = vi
        .mocked(fetch)
        .mock.calls.some(([url]) => url === '/api/v1/avaliacoes/1/finalizar')
      expect(chamouFinalizar).toBe(true)
      // Regressão: o finalizar() automático do efeito é fire-and-forget (seu retorno
      // nunca é lido) - sem expor o Blob via estado, o áudio de uma avaliação encerrada
      // por tempo esgotado nunca chegaria ao envio (spec.md AC2, FE-20).
      expect(result.current.blobGravado).toBeInstanceOf(Blob)
    })

    it('a manual click on Finalizar calls finalizar() without motivo (spec.md AC6)', async () => {
      const { result, recorderHandle } = await iniciarEmAndamento()
      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'FINALIZADA' })))

      let blob: Blob | null = null
      await act(async () => {
        blob = await result.current.finalizar()
      })

      expect(blob).toBeInstanceOf(Blob)
      expect(recorderHandle.stop).toHaveBeenCalledTimes(1)
      const [url, init] = vi.mocked(fetch).mock.calls.at(-1)!
      expect(url).toBe('/api/v1/avaliacoes/1/finalizar')
      expect(init?.method).toBe('POST')
      expect(init?.body).toBeUndefined()
      expect(result.current.status).toBe('FINALIZADA')
    })

    it.each([
      ['CRIADA', ['iniciar']],
      ['EM_ANDAMENTO', ['pausar', 'resetar', 'finalizar']],
      ['PAUSADA', ['continuar', 'resetar', 'finalizar']],
      ['FINALIZADA', []],
      ['CANCELADA', []],
    ] satisfies [StatusAvaliacao, string[]][])(
      'botoesHabilitados for status %s is exactly %j (spec.md AC7)',
      async (status, esperado) => {
        vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status })))
        const { result } = renderHook(() => useAvaliacaoExecucao(1))

        await waitFor(() => expect(result.current.status).toBe(status))
        expect(result.current.botoesHabilitados).toEqual(esperado)
      },
    )

    it('a 409 TRANSICAO_INVALIDA response in any transition reloads and resyncs, without a visible error (FE-15)', async () => {
      const { result } = await iniciarEmAndamento()
      vi.mocked(fetch).mockResolvedValueOnce(
        jsonResponse(409, { code: 'TRANSICAO_INVALIDA', detail: 'Ação pausar não permitida no status FINALIZADA' }),
      )
      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'FINALIZADA' })))

      await act(async () => {
        await result.current.pausar()
      })

      expect(result.current.status).toBe('FINALIZADA')
      const [url] = vi.mocked(fetch).mock.calls.at(-1)!
      expect(url).toBe('/api/v1/avaliacoes/1')
    })
  })
})
