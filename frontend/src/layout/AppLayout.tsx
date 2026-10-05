import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ThemeToggle } from '../theme/ThemeToggle'

interface MenuItem {
  label: string
  to?: string
  /** Submenu: o item vira um grupo (sem rota própria) com links filhos. */
  children?: { label: string; to: string }[]
}

const ALUNOS_BUSCAR = { label: 'Avaliação Aluno', to: '/alunos' }
const ALUNOS_HISTORICO = { label: 'Histórico', to: '/historico' }

// spec.md P1 "Login e navegação por perfil", AC5: Avaliar, Alunos (só Buscar aluno) e Histórico.
const PROFESSOR_MENU: MenuItem[] = [
  { label: 'Avaliar', to: '/avaliar' },
  { label: 'Alunos', children: [ALUNOS_BUSCAR, ALUNOS_HISTORICO] },
]

// spec.md P1 "Login e navegação por perfil", AC6: os 8 itens, nunca "Avaliar".
const COORDENADOR_MENU: MenuItem[] = [
  {
    label: 'Alunos',
    children: [
      { label: 'Cadastrar aluno', to: '/cadastros/alunos' },
      { label: 'Alterar aluno', to: '/cadastros/alunos/alterar' },
      ALUNOS_BUSCAR,
      ALUNOS_HISTORICO,
    ],
  },
  {
    label: 'Turmas',
    children: [
      { label: 'Cadastrar turma', to: '/cadastros/turmas/cadastrar' },
      { label: 'Listar turmas', to: '/cadastros/turmas/listar' },
      { label: 'Alunos da turma', to: '/cadastros/turmas/alunos' },
    ],
  },
  { label: 'Professores', to: '/cadastros/professores' },
  {
    label: 'Anos letivos',
    children: [
      { label: 'Cadastrar ano letivo', to: '/cadastros/anos-letivos/cadastrar' },
      { label: 'Listar anos letivos', to: '/cadastros/anos-letivos/listar' },
    ],
  },
  { label: 'Regras de classificação', to: '/cadastros/regras-classificacao' },
  {
    label: 'Listas de palavras',
    children: [
      { label: 'Cadastrar lista', to: '/cadastros/listas-palavras/cadastrar' },
      { label: 'Buscar listas', to: '/cadastros/listas-palavras/buscar' },
    ],
  },
  {
    label: 'Avaliações',
    children: [
      { label: 'Cadastrar avaliação', to: '/cadastros/avaliacoes/cadastrar' },
      { label: 'Buscar avaliações', to: '/cadastros/avaliacoes/buscar' },
    ],
  },
  { label: 'Usuários', to: '/cadastros/usuarios' },
]

/** Menu lateral condicionado por perfil (FE-04) + área de conteúdo das rotas filhas. */
export function AppLayout() {
  const { perfil, logout } = useAuth()
  const navigate = useNavigate()
  const [recolhidos, setRecolhidos] = useState<Set<string>>(new Set())
  const menu = perfil === 'COORDENADOR' ? COORDENADOR_MENU : PROFESSOR_MENU

  function alternar(label: string) {
    setRecolhidos((atual) => {
      const novo = new Set(atual)
      if (!novo.delete(label)) novo.add(label)
      return novo
    })
  }

  function sair() {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-layout">
      <nav aria-label="Menu principal">
        <div className="app-brand">
          <span className="app-brand__logo" aria-hidden="true">
            F
          </span>
          <span className="app-brand__name">Fluência Leitora</span>
        </div>
        <ul>
          {menu.map((item) => (
            <li key={item.label}>
              {item.children ? (
                <>
                  <button
                    type="button"
                    className="app-nav-group"
                    aria-expanded={!recolhidos.has(item.label)}
                    onClick={() => alternar(item.label)}
                  >
                    {item.label}
                    <span aria-hidden="true">{recolhidos.has(item.label) ? '▸' : '▾'}</span>
                  </button>
                  {recolhidos.has(item.label) ? null : (
                    <ul className="app-nav-sub">
                      {item.children.map((filho) => (
                        <li key={filho.to}>
                          <NavLink to={filho.to} end>
                            {filho.label}
                          </NavLink>
                        </li>
                      ))}
                    </ul>
                  )}
                </>
              ) : (
                <NavLink to={item.to as string}>{item.label}</NavLink>
              )}
            </li>
          ))}
        </ul>
        {/* T38: controle de logout visível para os dois perfis (gap reportado pelo Batch 1). */}
        <div className="app-nav-actions">
          <ThemeToggle />
          <button type="button" onClick={sair}>
            Sair
          </button>
        </div>
      </nav>
      <main>
        <Outlet />
      </main>
    </div>
  )
}
