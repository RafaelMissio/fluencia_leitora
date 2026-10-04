import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProfessorResponse, UsuarioResponse } from '../../../api/types'
import { UsuariosPage } from './UsuariosPage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const ADMIN: UsuarioResponse = { id: 1, email: 'admin@escola.com', perfil: 'COORDENADOR', professorId: null, ativo: true }
const PROFESSORES: ProfessorResponse[] = [{ id: 7, nome: 'Maria Souza', ativo: true, turmas: [] }]

interface Estado {
  usuarios: UsuarioResponse[]
  post?: () => Response
  putSenha?: () => Response
}

function mockApi(estado: Estado): void {
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url === '/api/v1/usuarios' && method === 'GET') return jsonResponse(200, estado.usuarios)
    if (url === '/api/v1/usuarios' && method === 'POST') return estado.post!()
    if (url.endsWith('/senha') && method === 'PUT') return estado.putSenha!()
    if (url === '/api/v1/professores') return jsonResponse(200, PROFESSORES)
    throw new Error(`unexpected fetch call: ${method} ${url}`)
  })
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <UsuariosPage />
    </QueryClientProvider>,
  )
}

async function criarProfessor(user: ReturnType<typeof userEvent.setup>): Promise<void> {
  await user.type(screen.getByLabelText('E-mail'), 'maria@escola.com')
  await user.type(screen.getByLabelText('Senha'), 'SenhaForte1')
  await screen.findByRole('option', { name: 'Maria Souza' })
  await user.selectOptions(screen.getByLabelText('Professor'), '7')
  await user.click(screen.getByRole('button', { name: 'Criar usuário' }))
}

describe('UsuariosPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates a valid usuário, shows "Salvo com sucesso" and lists it', async () => {
    const estado: Estado = { usuarios: [ADMIN] }
    estado.post = () => {
      const novo: UsuarioResponse = { id: 2, email: 'maria@escola.com', perfil: 'PROFESSOR', professorId: 7, ativo: true }
      estado.usuarios = [...estado.usuarios, novo]
      return jsonResponse(201, novo)
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@escola.com')

    await criarProfessor(user)

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    expect(await screen.findByText('maria@escola.com')).toBeInTheDocument()
    const post = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'POST')
    expect(JSON.parse(String(post?.[1]?.body))).toEqual({
      email: 'maria@escola.com', senha: 'SenhaForte1', perfil: 'PROFESSOR', professorId: 7,
    })
    expect(screen.getByLabelText('Senha')).toHaveValue('')
  })

  it('changes a usuário password without ever rendering it', async () => {
    mockApi({ usuarios: [ADMIN], putSenha: () => new Response(null, { status: 204 }) })
    const user = userEvent.setup()
    const { container } = renderPage()
    await user.type(await screen.findByLabelText('Nova senha de admin@escola.com'), 'SenhaNova123')
    expect(screen.getByLabelText('Nova senha de admin@escola.com')).toHaveAttribute('type', 'password')
    await user.click(screen.getByRole('button', { name: 'Alterar senha de admin@escola.com' }))

    expect(await screen.findByText('Salvo com sucesso')).toBeInTheDocument()
    expect(screen.getByLabelText('Nova senha de admin@escola.com')).toHaveValue('')
    expect(container.innerHTML).not.toContain('SenhaNova123')
    const put = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(String(put?.[0])).toBe('/api/v1/usuarios/1/senha')
    expect(JSON.parse(String(put?.[1]?.body))).toEqual({ senha: 'SenhaNova123' })
  })

  it('shows the 409 message at the top and the 422 message next to the field', async () => {
    const estado: Estado = {
      usuarios: [ADMIN],
      post: () => jsonResponse(409, { code: 'EMAIL_DUPLICADO', detail: 'E-mail já está em uso' }),
    }
    mockApi(estado)
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@escola.com')

    await criarProfessor(user)
    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail já está em uso')

    estado.post = () => jsonResponse(422, { errors: [{ field: 'email', message: 'deve ser um e-mail válido' }] })
    await user.type(screen.getByLabelText('Senha'), 'SenhaForte1')
    await user.click(screen.getByRole('button', { name: 'Criar usuário' }))
    expect(await screen.findByText('deve ser um e-mail válido')).toBeInTheDocument()
  })
})
