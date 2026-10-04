import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProfessorResponse } from '../../../api/types'
import { ProfessoresPage } from './ProfessoresPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const MARIA: ProfessorResponse = {
  id: 1, nome: 'Maria Souza', ativo: true, turmas: [{ id: 10, nome: 'Turma A', serie: 2 }],
}

interface Estado {
  professores: ProfessorResponse[]
  post?: () => Response
}

function mockApi(estado: Estado): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url === '/api/v1/professores' && method === 'GET') return jsonResponse(200, estado.professores)
    if (url === '/api/v1/professores' && method === 'POST') return estado.post!()
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ProfessoresPage />
    </QueryClientProvider>,
  )
}

async function criar(user: ReturnType<typeof userEvent.setup>, nome: string): Promise<void> {
  await user.type(screen.getByLabelText('Nome'), nome)
  await user.click(screen.getByRole('button', { name: 'Criar professor' }))
}

describe('ProfessoresPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates a valid professor, shows "Salvo com sucesso" and lists everyone with their turmas', async () => {
    const estado: Estado = { professores: [MARIA] }
    estado.post = () => {
      const novo = { id: 2, nome: 'Ana Lima', ativo: true, turmas: [] }
      estado.professores = [...estado.professores, novo]
      return jsonResponse(201, novo)
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()
    expect(await screen.findByText('Turma A')).toBeInTheDocument()

    await criar(user, 'Ana Lima')

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    expect(await screen.findByText('Ana Lima')).toBeInTheDocument()
  })

  it('shows the 409 message at the top and the 422 message next to the field', async () => {
    const estado: Estado = {
      professores: [MARIA],
      post: () => jsonResponse(409, { code: 'PROFESSOR_DUPLICADO', detail: 'Professor já cadastrado' }),
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Maria Souza')

    await criar(user, 'Maria Souza')
    expect(await screen.findByRole('alert')).toHaveTextContent('Professor já cadastrado')

    estado.post = () =>
      jsonResponse(422, { errors: [{ field: 'nome', message: 'tamanho deve ser entre 3 e 150' }] })
    await user.click(screen.getByRole('button', { name: 'Criar professor' }))
    expect(await screen.findByText('tamanho deve ser entre 3 e 150')).toBeInTheDocument()
  })
})
