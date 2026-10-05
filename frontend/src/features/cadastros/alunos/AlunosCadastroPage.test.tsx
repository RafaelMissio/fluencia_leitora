import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AlunoBuscaItem, TurmaResponse } from '../../../api/types'
import { AlunosCadastroPage } from './AlunosCadastroPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const TURMAS: TurmaResponse[] = [
  { id: 1, nome: 'Turma A', serie: 2, anoLetivoId: 1, professorId: null, ativo: true },
  { id: 2, nome: 'Turma B', serie: 3, anoLetivoId: 2, professorId: null, ativo: true },
  { id: 3, nome: 'Turma C', serie: 2, anoLetivoId: 1, professorId: null, ativo: true },
]

function aluno(alunoId: number, nome: string, turma: string): AlunoBuscaItem {
  return {
    alunoId,
    nome,
    turma,
    serie: 2,
    professor: null,
    anoLetivo: 2026,
    situacao: 'EM_ANDAMENTO',
  }
}

interface Estado {
  alunos: AlunoBuscaItem[]
  post?: () => Response
  delete?: () => Response
  matricula?: () => Response
  put?: () => Response
}

function mockApi(estado: Estado): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url === '/api/v1/turmas') return jsonResponse(200, TURMAS)
    if (url.startsWith('/api/v1/alunos?nome=')) {
      const termo = decodeURIComponent(url.split('nome=')[1].split('&')[0]).toLowerCase()
      const content = estado.alunos.filter((item) => item.nome.toLowerCase().includes(termo))
      return jsonResponse(200, {
        content,
        totalElements: content.length,
        totalPages: 1,
        number: 0,
        size: 20,
      })
    }
    if (url === '/api/v1/alunos' && method === 'POST') return estado.post!()
    if (url.endsWith('/matriculas') && method === 'POST') return estado.matricula!()
    if (/\/alunos\/\d+$/.test(url) && method === 'DELETE') return estado.delete!()
    if (/\/alunos\/\d+$/.test(url) && method === 'PUT') return estado.put!()
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AlunosCadastroPage />
    </QueryClientProvider>,
  )
}

async function criar(user: ReturnType<typeof userEvent.setup>, nome: string): Promise<void> {
  await user.type(screen.getByLabelText('Nome'), nome)
  await screen.findByRole('option', { name: 'Turma A' })
  await user.selectOptions(screen.getByLabelText('Turma'), '1')
  await user.click(screen.getByRole('button', { name: 'Criar aluno' }))
}

async function buscar(user: ReturnType<typeof userEvent.setup>, termo: string): Promise<void> {
  await user.type(screen.getByLabelText('Buscar aluno por nome'), termo)
}

describe('AlunosCadastroPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates aluno + matrícula, shows "Salvo com sucesso" and lists the new aluno', async () => {
    const estado: Estado = { alunos: [] }
    estado.post = () => {
      estado.alunos = [aluno(5, 'Pedro Alves', 'Turma A')]
      return jsonResponse(201, { alunoId: 5, matriculaId: 9 })
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()

    await criar(user, 'Pedro Alves')

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    expect(
      await screen.findByRole('button', { name: 'Selecionar Pedro Alves' }),
    ).toBeInTheDocument()
  })

  it('changes the turma of the active matrícula via PATCH', async () => {
    const base = aluno(5, 'Pedro Alves', 'Turma A')
    const estado: Estado = {
      alunos: [
        {
          ...base,
          matriculas: [
            {
              matriculaId: 10,
              anoLetivoId: 1,
              anoLetivo: 2026,
              situacaoAnoLetivo: 'ATIVO',
              turmaId: 1,
              turma: 'Turma A',
              serie: 2,
              professorId: null,
              status: 'CURSANDO',
            },
          ],
        },
      ],
    }
    mockApi(estado)
    const original = vi.mocked(fetch).getMockImplementation()!
    let corpo = ''
    vi.mocked(fetch).mockImplementation(async (input, init) => {
      if (String(input) === '/api/v1/matriculas/10' && init?.method === 'PATCH') {
        corpo = String(init.body)
        return jsonResponse(200, {
          id: 10,
          alunoId: 5,
          anoLetivoId: 1,
          turmaId: 3,
          serie: 2,
          professorId: null,
          anoFinalizado: false,
          status: 'CURSANDO',
        })
      }
      return original(input, init)
    })
    const user = userEvent.setup()
    renderPage()
    await buscar(user, 'Pedro')
    await user.click(await screen.findByRole('button', { name: 'Selecionar Pedro Alves' }))
    expect(await screen.findByLabelText('Status de Pedro Alves')).toHaveValue('ATIVO')
    await screen.findAllByRole('option', { name: 'Turma A' })
    await user.selectOptions(screen.getByLabelText('Turma de Pedro Alves'), '3')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações de Pedro Alves' }))

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    expect(JSON.parse(corpo)).toEqual({ turmaId: 3 })
  })

  it('inactivates the aluno through the Status field', async () => {
    const estado: Estado = { alunos: [{ ...aluno(5, 'Pedro Alves', 'Turma A'), ativo: true }] }
    mockApi(estado)
    const original = vi.mocked(fetch).getMockImplementation()!
    let corpo = ''
    vi.mocked(fetch).mockImplementation(async (input, init) => {
      if (String(input) === '/api/v1/alunos/5/situacao' && init?.method === 'PATCH') {
        corpo = String(init.body)
        return jsonResponse(200, { id: 5, nome: 'Pedro Alves', ativo: false })
      }
      return original(input, init)
    })
    const user = userEvent.setup()
    renderPage()
    await buscar(user, 'Pedro')
    await user.click(await screen.findByRole('button', { name: 'Selecionar Pedro Alves' }))
    await user.selectOptions(screen.getByLabelText('Status de Pedro Alves'), 'INATIVO')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações de Pedro Alves' }))

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    expect(JSON.parse(corpo)).toEqual({ ativo: false })
  })

  it('shows the 409 message at the top and the 422 message next to the field', async () => {
    const estado: Estado = {
      alunos: [],
      post: () =>
        jsonResponse(409, { code: 'ALUNO_DUPLICADO', detail: 'Aluno já matriculado nessa turma' }),
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()

    await criar(user, 'Pedro Alves')
    expect(await screen.findByRole('alert')).toHaveTextContent('Aluno já matriculado nessa turma')

    estado.post = () =>
      jsonResponse(422, { errors: [{ field: 'nome', message: 'tamanho deve ser entre 3 e 150' }] })
    await user.click(screen.getByRole('button', { name: 'Criar aluno' }))
    expect(await screen.findByText('tamanho deve ser entre 3 e 150')).toBeInTheDocument()
  })

  it('renames an existing aluno', async () => {
    const estado: Estado = {
      alunos: [aluno(5, 'Pedro Alves', 'Turma A')],
      put: () => jsonResponse(200, { id: 5, nome: 'Pedro Alves Jr', ativo: true }),
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()
    await buscar(user, 'Pedro')
    await user.click(await screen.findByRole('button', { name: 'Selecionar Pedro Alves' }))
    const campo = await screen.findByLabelText('Nome de Pedro Alves')
    await user.type(campo, ' Jr')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações de Pedro Alves' }))

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    const put = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(JSON.parse(String(put?.[1]?.body))).toEqual({ nome: 'Pedro Alves Jr' })
  })
})
