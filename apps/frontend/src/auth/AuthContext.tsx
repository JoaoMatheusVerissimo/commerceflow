/* eslint-disable react-refresh/only-export-components */
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
  const [expiresIn, setExpiresIn] = useState(900)

  useEffect(() => {
    let active = true
    authApi.refresh().then((result) => {
      if (active) { setAccessToken(result.accessToken); setExpiresIn(result.expiresIn) }
    }).catch(() => { if (active) setAccessToken(null) })
      .finally(() => { if (active) setReady(true) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!accessToken) return
    let active = true
    const timer = setTimeout(() => {
      authApi.refresh().then((result) => {
        if (active) { setAccessToken(result.accessToken); setExpiresIn(result.expiresIn) }
      }).catch(() => { if (active) setAccessToken(null) })
    }, Math.max(1, expiresIn - 60) * 1000)
    return () => { active = false; clearTimeout(timer) }
  }, [accessToken, expiresIn])

  const value = useMemo<AuthState>(() => ({
    accessToken,
    ready,
    login: async (data) => {
      const result = await authApi.login(data)
      setAccessToken(result.accessToken); setExpiresIn(result.expiresIn)
    },
    register: async (data) => {
      const result = await authApi.register(data)
      setAccessToken(result.accessToken); setExpiresIn(result.expiresIn)
    },
    logout: async () => {
      await authApi.logout()
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
