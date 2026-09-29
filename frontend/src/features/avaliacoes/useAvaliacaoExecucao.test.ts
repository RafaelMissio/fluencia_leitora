import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AvaliacaoResponse } from '../../api/types'
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
})
