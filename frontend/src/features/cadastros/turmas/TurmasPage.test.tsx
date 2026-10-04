import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnoLetivoResponse, ProfessorResponse, TurmaResponse } from '../../../api/types'
import { TurmasPage } from './TurmasPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const ANOS: AnoLetivoResponse[] = [
  { id: 1, ano: 2026, dataInicio: '2026-02-01', dataFim: '2026-12-15', situacao: 'ATIVO', ativo: true },
]
const PROFESSORES: ProfessorResponse[] = [
  { id: 7, nome: 'Maria Souza', ativo: true, turmas: [] },
  { id: 8, nome: 'Ana Lima', ativo: true, turmas: [] },
]
const TURMA_A: TurmaResponse = { id: 100, nome: 'Turma A', serie: 2, anoLetivoId: 1, professorId: 7, ativo: true }

interface Estado {
  turmas: TurmaResponse[]
  post?: () => Response
}

function mockApi(estado: Estado): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url === '/api/v1/turmas' && method === 'GET') return jsonResponse(200, estado.turmas)
    if (url === '/api/v1/turmas' && method === 'POST') return estado.post!()
    if (url.startsWith('/api/v1/turmas/') && method === 'PUT') {
      const id = Number(url.split('/').pop())
      const { professorId } = JSON.parse(String(init?.body)) as { professorId: number }
      estado.turmas = estado.turmas.map((t) => (t.id === id ? { ...t, professorId } : t))
      return jsonResponse(200, estado.turmas.find((t) => t.id === id))
    }
    if (url === '/api/v1/professores') return jsonResponse(200, PROFESSORES)
    if (url === '/api/v1/anos-letivos') return jsonResponse(200, ANOS)
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <TurmasPage />
    </QueryClientProvider>,
  )
}

async function preencherEEnviar(user: ReturnType<typeof userEvent.setup>): Promise<void> {
  await user.type(screen.getByLabelText('Nome'), 'Turma B')
  await user.selectOptions(await screen.findByLabelText('Ano letivo'), '1')
  await screen.findAllByRole('option', { name: 'Maria Souza' })
  await user.click(screen.getByRole('button', { name: 'Criar turma' }))
}

describe('TurmasPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates a valid turma, shows "Salvo com sucesso" and lists it', async () => {
    const estado: Estado = { turmas: [TURMA_A] }
    estado.post = () => {
      const nova = { ...TURMA_A, id: 101, nome: 'Turma B' }
      estado.turmas = [...estado.turmas, nova]
      return jsonResponse(201, nova)
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Turma A')

    await preencherEEnviar(user)

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    expect(await screen.findByText('Turma B')).toBeInTheDocument()
  })

  it('changes the professor of an existing turma and reflects it in the list', async () => {
    mockApi({ turmas: [TURMA_A] })
    const user = userEvent.setup()
    renderPage()
    const select = await screen.findByLabelText('Professor da turma Turma A')
    const professorDaLinha = () => within(screen.getByText('Turma A').closest('tr')!).getAllByRole('cell')[2]
    await waitFor(() => expect(professorDaLinha()).toHaveTextContent('Maria Souza'))

    await user.selectOptions(select, '8')

    await waitFor(() => expect(professorDaLinha()).toHaveTextContent('Ana Lima'))
    expect(screen.getByText('Salvo com sucesso')).toBeInTheDocument()
  })

  it('shows the 409 message at the top and the 422 message next to the field', async () => {
    const estado: Estado = {
      turmas: [TURMA_A],
      post: () => jsonResponse(409, { code: 'TURMA_DUPLICADA', detail: 'Já existe uma turma com esse nome nesse ano letivo' }),
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Turma A')

    await preencherEEnviar(user)
    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe uma turma com esse nome nesse ano letivo')

    estado.post = () => jsonResponse(422, { errors: [{ field: 'nome', message: 'não deve estar em branco' }] })
    await user.click(screen.getByRole('button', { name: 'Criar turma' }))
    expect(await screen.findByText('não deve estar em branco')).toBeInTheDocument()
  })
})
