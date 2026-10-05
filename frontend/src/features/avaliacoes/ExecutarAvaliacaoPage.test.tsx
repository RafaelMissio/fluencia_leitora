import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AvaliacaoResponse } from '../../api/types'
import { ExecutarAvaliacaoPage } from './ExecutarAvaliacaoPage'

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
    tempoConfiguradoSegundos: 1,
    status: 'CRIADA',
    iniciadoEm: null,
    finalizadoEm: null,
    quantidadeTotal: 2,
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
    ],
    ...overrides,
  }
}

const FAKE_STREAM = { getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream

function stubGetUserMedia(): ReturnType<typeof vi.fn> {
  const getUserMedia = vi.fn()
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: { getUserMedia } })
  return getUserMedia
}

function renderPagina(inicial: AvaliacaoResponse) {
  vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, inicial))
  return render(
    <MemoryRouter initialEntries={['/avaliacoes/1/executar']}>
      <Routes>
        <Route path="/avaliacoes/:id/executar" element={<ExecutarAvaliacaoPage />} />
        <Route path="/avaliacoes/:id/resultado" element={<p>Tela de resultado</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ExecutarAvaliacaoPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('explains when the avaliação cannot be loaded instead of showing an empty screen', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(403, { code: 'ACESSO_NEGADO' }))
    render(
      <MemoryRouter initialEntries={['/avaliacoes/1/executar']}>
        <Routes>
          <Route path="/avaliacoes/:id/executar" element={<ExecutarAvaliacaoPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar a avaliação')
  })

  it('shows the "Iniciar avaliação" button as the primary action with guidance when the avaliação is CRIADA', async () => {
    renderPagina(avaliacao())

    const botao = await screen.findByRole('button', { name: 'Iniciar avaliação' })
    expect(botao).toHaveClass('primary')
    expect(screen.getByText(/pede acesso ao microfone/)).toBeInTheDocument()
  })

  it('denying the microphone keeps the screen in CRIADA with the error message visible (spec.md AC2)', async () => {
    vi.mocked(isMediaRecorderSupported).mockReturnValue(true)
    const getUserMedia = stubGetUserMedia()
    getUserMedia.mockRejectedValueOnce(new DOMException('Permission denied', 'NotAllowedError'))
    const user = userEvent.setup()

    renderPagina(avaliacao())
    const botaoIniciar = await screen.findByRole('button', { name: 'Iniciar avaliação' })

    await user.click(botaoIniciar)

    expect(await screen.findByRole('alert')).toHaveTextContent('Permita o acesso ao microfone para iniciar a avaliação')
    expect(screen.getByRole('button', { name: 'Iniciar avaliação' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Pausar' })).not.toBeInTheDocument()
  })

  it(
    'happy path: iniciar shows timer+Gravando together, marking a word calls the API, and the timer ' +
      'reaching zero navigates automatically to the resultado screen',
    async () => {
      vi.mocked(isMediaRecorderSupported).mockReturnValue(true)
      vi.mocked(pickSupportedMimeType).mockReturnValue('audio/webm;codecs=opus')
      const recorderHandle = { start: vi.fn(), pause: vi.fn(), resume: vi.fn(), stop: vi.fn().mockResolvedValue(new Blob()) }
      vi.mocked(createRecorder).mockReturnValue(recorderHandle)
      const getUserMedia = stubGetUserMedia()
      getUserMedia.mockResolvedValueOnce(FAKE_STREAM)

      renderPagina(avaliacao())
      const botaoIniciar = await screen.findByRole('button', { name: 'Iniciar avaliação' })

      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'EM_ANDAMENTO' })))

      let now = 0
      vi.useFakeTimers()
      vi.spyOn(performance, 'now').mockImplementation(() => now)

      await act(async () => {
        fireEvent.click(botaoIniciar)
        await vi.advanceTimersByTimeAsync(0)
      })

      expect(screen.getByLabelText('Gravando')).toBeInTheDocument()
      expect(screen.getByRole('timer')).toHaveTextContent('00:01')

      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'EM_ANDAMENTO' })))
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'casa: Pendente' }))
        await vi.advanceTimersByTimeAsync(0)
      })
      expect(vi.mocked(fetch)).toHaveBeenCalledWith('/api/v1/avaliacoes/1/palavras/1', expect.objectContaining({ method: 'PUT' }))

      vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'FINALIZADA', tempoConfiguradoSegundos: 1 })))
      now += 1000
      await act(async () => {
        await vi.advanceTimersByTimeAsync(1000)
      })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })

      // `findByText` faria polling com `setTimeout` real, que fica congelado sob
      // fake timers (`vi.useFakeTimers()` acima) - o `act()` já garantiu que o
      // estado está totalmente propagado, então a asserção síncrona basta.
      expect(screen.getByText('Tela de resultado')).toBeInTheDocument()
    },
  )

  const TODOS_OS_ROTULOS = ['Iniciar avaliação', 'Pausar', 'Continuar', 'Resetar', 'Finalizar']

  // Asserção síncrona (não `waitFor`): sob `vi.useFakeTimers()`, o polling
  // real de `waitFor` fica congelado - os chamadores já usam `act()` para
  // esperar o estado propagar antes de checar os botões.
  function esperarSomenteEstesBotoes(rotulos: string[]): void {
    for (const rotulo of rotulos) {
      expect(screen.getByRole('button', { name: rotulo })).toBeInTheDocument()
    }
    for (const rotulo of TODOS_OS_ROTULOS.filter((item) => !rotulos.includes(item))) {
      expect(screen.queryByRole('button', { name: rotulo })).not.toBeInTheDocument()
    }
  }

  it('enabled buttons match the current status at each step: CRIADA -> EM_ANDAMENTO -> PAUSADA (spec.md AC7)', async () => {
    vi.mocked(isMediaRecorderSupported).mockReturnValue(true)
    vi.mocked(pickSupportedMimeType).mockReturnValue('audio/webm;codecs=opus')
    const recorderHandle = { start: vi.fn(), pause: vi.fn(), resume: vi.fn(), stop: vi.fn().mockResolvedValue(new Blob()) }
    vi.mocked(createRecorder).mockReturnValue(recorderHandle)
    const getUserMedia = stubGetUserMedia()
    getUserMedia.mockResolvedValueOnce(FAKE_STREAM)

    renderPagina(avaliacao({ tempoConfiguradoSegundos: 60 }))
    const botaoIniciar = await screen.findByRole('button', { name: 'Iniciar avaliação' })
    esperarSomenteEstesBotoes(['Iniciar avaliação'])

    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'EM_ANDAMENTO', tempoConfiguradoSegundos: 60 })))
    vi.useFakeTimers()
    vi.spyOn(performance, 'now').mockImplementation(() => 0)
    await act(async () => {
      fireEvent.click(botaoIniciar)
      await vi.advanceTimersByTimeAsync(0)
    })
    esperarSomenteEstesBotoes(['Pausar', 'Resetar', 'Finalizar'])

    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, avaliacao({ status: 'PAUSADA', tempoConfiguradoSegundos: 60 })))
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Pausar' }))
      await vi.advanceTimersByTimeAsync(0)
    })
    esperarSomenteEstesBotoes(['Continuar', 'Resetar', 'Finalizar'])
  })

  it('reloading mid avaliação (EM_ANDAMENTO on mount) shows the interrupted warning and offers only Resetar/Finalizar (spec.md Edge Cases, FE-24)', async () => {
    renderPagina(avaliacao({ status: 'EM_ANDAMENTO' }))

    expect(await screen.findByText('A gravação anterior foi interrompida')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Resetar' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Finalizar' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Pausar' })).not.toBeInTheDocument()
  })
})
