import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { Perfil } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { LoginPage } from '../auth/LoginPage'
import { AlunoBuscaPage } from '../features/alunos/AlunoBuscaPage'
import { ConfigurarAvaliacaoPage } from '../features/avaliacoes/ConfigurarAvaliacaoPage'
import { AppLayout } from '../layout/AppLayout'

/**
 * Bloqueia rotas sem sessão: redireciona para `/login` preservando a rota de
 * origem em `state.from`, para que o login volte a ela (spec.md FE-03, AC4).
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <>{children}</>
}

/**
 * Renderiza os filhos só quando o `perfil` da sessão está em `allow`; caso
 * contrário some o conteúdo (usado para restringir abas/menus ao COORDENADOR
 * sem tratar como um 403 na tela - context.md).
 */
export function RoleGate({
  allow,
  children,
  fallback = null,
}: {
  allow: Perfil[]
  children: ReactNode
  fallback?: ReactNode
}) {
  const { perfil } = useAuth()

  if (!perfil || !allow.includes(perfil)) {
    return <>{fallback}</>
  }

  return <>{children}</>
}

/**
 * `<Routes>` raiz: só `/login` é pública. Tudo o mais entra sob o layout
 * protegido - as páginas de cada feature adicionam suas próprias `<Route>`
 * filhas aqui nas próximas tasks (T8, T14, T22, T23, T28, T30..T36).
 */
export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        {/* Rotas de features entram aqui como <Route> filhas (tasks seguintes) */}
        <Route path="/alunos" element={<AlunoBuscaPage />} />
        <Route path="/avaliacoes/nova" element={<ConfigurarAvaliacaoPage />} />
      </Route>
    </Routes>
  )
}
