import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

interface MenuItem {
  label: string
  to: string
}

// spec.md P1 "Login e navegação por perfil", AC5: só Avaliar, Meus alunos e Histórico.
const PROFESSOR_MENU: MenuItem[] = [
  { label: 'Avaliar', to: '/avaliar' },
  { label: 'Meus alunos', to: '/alunos' },
  { label: 'Histórico', to: '/historico' },
]

// spec.md P1 "Login e navegação por perfil", AC6: os 8 itens, nunca "Avaliar".
const COORDENADOR_MENU: MenuItem[] = [
  { label: 'Alunos', to: '/cadastros/alunos' },
  { label: 'Turmas', to: '/cadastros/turmas' },
  { label: 'Professores', to: '/cadastros/professores' },
  { label: 'Anos letivos', to: '/cadastros/anos-letivos' },
  { label: 'Regras de classificação', to: '/cadastros/regras-classificacao' },
  { label: 'Listas de palavras', to: '/cadastros/listas-palavras' },
  { label: 'Usuários', to: '/cadastros/usuarios' },
  { label: 'Avaliações', to: '/cadastros/avaliacoes' },
]

/** Menu lateral condicionado por perfil (FE-04) + área de conteúdo das rotas filhas. */
export function AppLayout() {
  const { perfil, logout } = useAuth()
  const navigate = useNavigate()
  const menu = perfil === 'COORDENADOR' ? COORDENADOR_MENU : PROFESSOR_MENU

  function sair() {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-layout">
      <nav aria-label="Menu principal">
        <ul>
          {menu.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to}>{item.label}</NavLink>
            </li>
          ))}
        </ul>
        {/* T38: controle de logout visível para os dois perfis (gap reportado pelo Batch 1). */}
        <button type="button" onClick={sair}>
          Sair
        </button>
      </nav>
      <main>
        <Outlet />
      </main>
    </div>
  )
}
