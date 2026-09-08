import { createContext, type PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react'
import { authApi, type Credentials, type Registration } from './api'

type AuthState = {
  accessToken: string | null
  ready: boolean
  login: (data: Credentials) => Promise<void>
  register: (data: Registration) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: PropsWithChildren) {
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    authApi.refresh().then((result) => setAccessToken(result.accessToken)).catch(() => setAccessToken(null))
      .finally(() => setReady(true))
  }, [])

  const value = useMemo<AuthState>(() => ({
    accessToken,
    ready,
    login: async (data) => setAccessToken((await authApi.login(data)).accessToken),
    register: async (data) => setAccessToken((await authApi.register(data)).accessToken),
    logout: async () => {
      if (accessToken) await authApi.logout(accessToken)
      setAccessToken(null)
    },
  }), [accessToken, ready])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
