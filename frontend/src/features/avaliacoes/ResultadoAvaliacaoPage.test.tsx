import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AvaliacaoResponse } from '../../api/types'
import { AuthProvider } from '../../auth/AuthContext'
import { ResultadoAvaliacaoPage } from './ResultadoAvaliacaoPage'

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
    status: 'FINALIZADA',
    iniciadoEm: '2026-09-29T10:00:00Z',
    finalizadoEm: '2026-09-29T10:01:00Z',
    quantidadeTotal: 12,
    tempoUtilizadoSegundos: 55,
    quantidadeCorretas: 7,
    quantidadeIncorretas: 2,
    quantidadeNaoLidas: 3,
    quantidadeLidas: 9,
    percentualAcerto: 58.33,
    fase: 'FLUENTE',
    nivel: 4,
    classificacaoPendente: false,
    ativa: true,
    palavras: [],
    ...overrides,
  }
}

function primarSessao(): void {
  sessionStorage.setItem('fluencia.session', JSON.stringify({ token: 'tok', perfil: 'PROFESSOR', professorId: 1 }))
}

/** Valor da `<dd>` ao lado do rótulo `<dt>` (evita ambiguidade entre números repetidos). */
function valorDe(rotulo: string): string | null {
  const dt = screen.getByText(rotulo)
  return dt.nextElementSibling?.textContent ?? null
}

function instalarFetchMock(opts: { avaliacao: AvaliacaoResponse; audioPostRespostas?: number[] }) {
  let postCount = 0
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url.includes('/audio') && method === 'POST') {
      const status = opts.audioPostRespostas?.[postCount] ?? 201
      postCount++
      return new Response(null, { status })
    }
    if (url.includes('/audio') && method === 'GET') {
      // Corpo string (não um Blob construído pelo jsdom): o `Response` do
      // undici (ambiente de teste) só aceita corpos que implementem seu
      // próprio contrato de stream - uma string funciona e `.blob()` a
      // envolve num Blob normalmente.
      return new Response('audio-bytes', { status: 200, headers: { 'Content-Type': 'audio/webm' } })
    }
    if (/\/avaliacoes\/\d+$/.test(url) && method === 'GET') {
      return jsonResponse(200, opts.avaliacao)
    }
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPagina(blob?: Blob) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[{ pathname: '/avaliacoes/1/resultado', state: blob ? { blob } : undefined }]}
        >
          <Routes>
            <Route path="/avaliacoes/:id/resultado" element={<ResultadoAvaliacaoPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </AuthProvider>,
  )
}

describe('ResultadoAvaliacaoPage', () => {
  beforeEach(() => {
    primarSessao()
    vi.stubGlobal('fetch', vi.fn())
    // jsdom não implementa URL.createObjectURL/revokeObjectURL.
    vi.stubGlobal(
      'URL',
      Object.assign(URL, {
        createObjectURL: vi.fn(() => 'blob:mock-url'),
        revokeObjectURL: vi.fn(),
      }),
    )
  })

  afterEach(() => {
    sessionStorage.clear()
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('shows all AC1 fields with the AvaliacaoResponse values', async () => {
    instalarFetchMock({ avaliacao: avaliacao() })
    renderPagina()

    await waitFor(() => expect(screen.getByText('Resultado da avaliação')).toBeInTheDocument())
    expect(valorDe('Total')).toBe('12')
    expect(valorDe('Lidas')).toBe('9')
    expect(valorDe('Corretas')).toBe('7')
    expect(valorDe('Incorretas')).toBe('2')
    expect(valorDe('Não lidas')).toBe('3')
    expect(valorDe('Percentual de acerto')).toBe('58.33%')
    expect(valorDe('Tempo utilizado')).toBe('55s')
    expect(valorDe('Fase')).toBe('FLUENTE')
    expect(valorDe('Nível')).toBe('4')
  })

  it(
    'a simulated send failure shows the retry message/button; retrying successfully shows the player ' +
      'via the streaming endpoint and the download link using ?download=true (AC2/AC3/AC4)',
    async () => {
      vi.useFakeTimers()
      instalarFetchMock({ avaliacao: avaliacao(), audioPostRespostas: [500, 500, 500, 201] })

      const { container } = renderPagina(new Blob(['audio'], { type: 'audio/webm' }))

      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(1000)
      })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2000)
      })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })

      expect(screen.getByRole('alert')).toHaveTextContent('O áudio não foi enviado')
      const botaoReenviar = screen.getByRole('button', { name: 'Tentar enviar novamente' })

      await act(async () => {
        fireEvent.click(botaoReenviar)
        await vi.advanceTimersByTimeAsync(0)
      })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(0)
      })

      const chamouEndpointDeStreaming = vi
        .mocked(fetch)
        .mock.calls.some(([url]) => String(url) === '/api/v1/avaliacoes/1/audio?download=true')
      expect(chamouEndpointDeStreaming).toBe(true)

      expect(container.querySelector('audio')).toHaveAttribute('src', 'blob:mock-url')
      const linkBaixar = screen.getByRole('link', { name: 'Baixar áudio' })
      expect(linkBaixar).toHaveAttribute('href', 'blob:mock-url')
      expect(linkBaixar).toHaveAttribute('download')
    },
  )

  it('classificacaoPendente: true shows the corresponding warning (AC5)', async () => {
    instalarFetchMock({ avaliacao: avaliacao({ classificacaoPendente: true }) })
    renderPagina()

    expect(await screen.findByText('Classificação pendente: nenhuma regra cobre este resultado')).toBeInTheDocument()
  })
})
