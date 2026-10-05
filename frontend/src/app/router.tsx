import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { Perfil } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { LoginPage } from '../auth/LoginPage'
import { AlunoBuscaPage } from '../features/alunos/AlunoBuscaPage'
import { ConfigurarAvaliacaoPage } from '../features/avaliacoes/ConfigurarAvaliacaoPage'
import { ExecutarAvaliacaoPage } from '../features/avaliacoes/ExecutarAvaliacaoPage'
import { ResultadoAvaliacaoPage } from '../features/avaliacoes/ResultadoAvaliacaoPage'
import { ProgramarAvaliacoesPage } from '../features/cadastros/avaliacoes/ProgramarAvaliacoesPage'
import { AnoLetivoPage } from '../features/cadastros/anosletivos/AnoLetivoPage'
import { TurmasPage } from '../features/cadastros/turmas/TurmasPage'
import { ProfessoresPage } from '../features/cadastros/professores/ProfessoresPage'
import { AlunosCadastroPage } from '../features/cadastros/alunos/AlunosCadastroPage'
import { UsuariosPage } from '../features/cadastros/usuarios/UsuariosPage'
import { ListasPalavrasPage } from '../features/cadastros/listas/ListasPalavrasPage'
import { RegrasClassificacaoPage } from '../features/cadastros/regras/RegrasClassificacaoPage'
import { HistoricoPage } from '../features/historico/HistoricoPage'
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
        {/* Itens de menu que partem da busca de aluno (escolhe o aluno e segue para avaliar/ver histórico) */}
        <Route path="/avaliar" element={<AlunoBuscaPage />} />
        <Route path="/historico" element={<AlunoBuscaPage modo="historico" />} />
        <Route path="/cadastros/avaliacoes" element={<Navigate to="/cadastros/avaliacoes/cadastrar" replace />} />
        <Route
          path="/cadastros/avaliacoes/cadastrar"
          element={
            <RoleGate allow={['COORDENADOR']} fallback={<Navigate to="/alunos" replace />}>
              <ProgramarAvaliacoesPage modo="cadastrar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/avaliacoes/buscar"
          element={
            <RoleGate allow={['COORDENADOR']} fallback={<Navigate to="/alunos" replace />}>
              <ProgramarAvaliacoesPage modo="buscar" />
            </RoleGate>
          }
        />
        <Route
          path="/avaliacoes/nova"
          element={
            <RoleGate allow={['PROFESSOR', 'COORDENADOR']} fallback={<Navigate to="/alunos" replace />}>
              <ConfigurarAvaliacaoPage />
            </RoleGate>
          }
        />
        <Route
          path="/avaliacoes/:id/executar"
          element={
            <RoleGate allow={['PROFESSOR', 'COORDENADOR']} fallback={<Navigate to="/alunos" replace />}>
              <ExecutarAvaliacaoPage />
            </RoleGate>
          }
        />
        <Route path="/avaliacoes/:id/resultado" element={<ResultadoAvaliacaoPage />} />
        <Route path="/alunos/:id/historico" element={<HistoricoPage />} />
        <Route
          path="/cadastros/regras-classificacao"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <RegrasClassificacaoPage />
            </RoleGate>
          }
        />
        <Route path="/cadastros/listas-palavras" element={<Navigate to="/cadastros/listas-palavras/cadastrar" replace />} />
        <Route
          path="/cadastros/listas-palavras/cadastrar"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <ListasPalavrasPage modo="cadastrar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/listas-palavras/buscar"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <ListasPalavrasPage modo="buscar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/usuarios"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <UsuariosPage />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/alunos"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <AlunosCadastroPage modo="cadastrar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/alunos/alterar"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <AlunosCadastroPage modo="alterar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/professores"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <ProfessoresPage />
            </RoleGate>
          }
        />
        <Route path="/cadastros/turmas" element={<Navigate to="/cadastros/turmas/cadastrar" replace />} />
        <Route
          path="/cadastros/turmas/cadastrar"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <TurmasPage modo="cadastrar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/turmas/listar"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <TurmasPage modo="listar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/turmas/alunos"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <TurmasPage modo="alunos" />
            </RoleGate>
          }
        />
        <Route path="/cadastros/anos-letivos" element={<Navigate to="/cadastros/anos-letivos/cadastrar" replace />} />
        <Route
          path="/cadastros/anos-letivos/cadastrar"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <AnoLetivoPage modo="cadastrar" />
            </RoleGate>
          }
        />
        <Route
          path="/cadastros/anos-letivos/listar"
          element={
            <RoleGate allow={['COORDENADOR']}>
              <AnoLetivoPage modo="listar" />
            </RoleGate>
          }
        />
      </Route>
    </Routes>
  )
}
