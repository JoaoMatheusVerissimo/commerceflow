import { useEffect, useState } from 'react'

type Profile = { name: string; email: string }

export function AccountProfile({ accessToken }: { accessToken: string }) {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    fetch('/api/v1/customers/me', { headers: { Authorization: `Bearer ${accessToken}` }, signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error(response.status === 403 ? 'Você não tem permissão para acessar este perfil.' : 'Não foi possível carregar seu perfil.')
        return response.json() as Promise<Profile>
      }).then(setProfile).catch((reason: unknown) => {
        if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : 'Falha ao carregar.')
      })
    return () => controller.abort()
  }, [accessToken, attempt])

  if (error) return <div role="alert"><p>{error}</p><button className="mt-3 underline" onClick={() => { setError(''); setAttempt(attempt + 1) }}>Tentar novamente</button></div>
  if (!profile) return <p role="status">Carregando seu perfil…</p>
  return <div><p role="status" className="text-sm font-medium text-[#285f33]">Sua conta está ativa</p>
    <h2 className="mt-4 text-2xl font-semibold">{profile.name}</h2>
    <p className="mt-2 text-[#637064]">{profile.email}</p></div>
}
