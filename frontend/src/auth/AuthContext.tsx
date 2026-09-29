import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { request, setAuthToken, setUnauthorizedHandler } from '../api/client'
import type { LoginResponse, Perfil } from '../api/types'

const SESSION_STORAGE_KEY = 'fluencia.session'

interface Session {
  token: string
  perfil: Perfil
  professorId: number | null
}

export interface AuthContextValue {
  token: string | null
  perfil: Perfil | null
  professorId: number | null
  isAuthenticated: boolean
  login(email: string, senha: string): Promise<void>
  logout(): void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function readSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(SESSION_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<Session>
    if (!parsed.token || !parsed.perfil) return null
    return { token: parsed.token, perfil: parsed.perfil, professorId: parsed.professorId ?? null }
  } catch {
    return null
  }
}

function writeSession(session: Session): void {
  sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
}

function clearSession(): void {
  sessionStorage.removeItem(SESSION_STORAGE_KEY)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // Lazy initializer: roda de forma síncrona no boot, antes do primeiro
  // paint, para que `isAuthenticated` já comece `true` sem esperar nenhuma
  // chamada (Done when) e o apiClient já tenha o token registrado antes de
  // qualquer query disparada por uma página protegida.
  const [session, setSession] = useState<Session | null>(() => {
    const existing = readSession()
    setAuthToken(existing?.token ?? null)
    return existing
  })

  const logout = useCallback((): void => {
    clearSession()
    setAuthToken(null)
    setSession(null)
  }, [])

  // Callback de 401 registrado no apiClient (T3): qualquer requisição
  // autenticada que volte 401 descarta a sessão (FE-03).
  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  const login = useCallback(async (email: string, senha: string): Promise<void> => {
    const response = await request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, senha }),
    })
    const newSession: Session = {
      token: response.accessToken,
      perfil: response.perfil,
      professorId: response.professorId,
    }
    writeSession(newSession)
    setAuthToken(newSession.token)
    setSession(newSession)
  }, [])

  const value: AuthContextValue = {
    token: session?.token ?? null,
    perfil: session?.perfil ?? null,
    professorId: session?.professorId ?? null,
    isAuthenticated: session !== null,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
