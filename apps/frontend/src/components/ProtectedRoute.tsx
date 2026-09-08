import type { PropsWithChildren } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const { accessToken, ready } = useAuth()
  if (!ready) return <div className="grid min-h-screen place-items-center">Carregando sessão…</div>
  return accessToken ? children : <Navigate to="/login" replace />
}
