export type Credentials = { email: string; password: string }
export type Registration = Credentials & { name: string }
export type AuthResponse = { accessToken: string; tokenType: string; expiresIn: number }

const API = '/api/v1/auth'
let refreshInFlight: Promise<AuthResponse> | null = null

export function refreshSession(): Promise<AuthResponse> {
  if (!refreshInFlight) {
    refreshInFlight = request('/refresh', { method: 'POST', headers: { 'X-CSRF-Token': csrfToken() } })
      .finally(() => { refreshInFlight = null })
  }
  return refreshInFlight
}

function csrfToken(): string {
  return document.cookie.split('; ').find((item) => item.startsWith('commerceflow_csrf='))?.split('=')[1] ?? ''
}

async function request(path: string, init: RequestInit): Promise<AuthResponse> {
  const response = await fetch(`${API}${path}`, { credentials: 'include', ...init,
    headers: { 'Content-Type': 'application/json', ...init.headers } })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Não foi possível concluir a operação.' }))
    throw new Error(error.message ?? 'Não foi possível concluir a operação.')
  }
  return response.json()
}

export const authApi = {
  login: (data: Credentials) => request('/login', { method: 'POST', body: JSON.stringify(data) }),
  register: (data: Registration) => request('/register', { method: 'POST', body: JSON.stringify(data) }),
  refresh: refreshSession,
  logout: async () => {
    const response = await fetch(`${API}/logout`, { method: 'POST', credentials: 'include',
      headers: { 'X-CSRF-Token': csrfToken() } })
    if (!response.ok) throw new Error('Não foi possível encerrar a sessão. Tente novamente.')
  },
}
