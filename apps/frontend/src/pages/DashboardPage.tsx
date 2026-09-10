import { useAuth } from '../auth/AuthContext'
import { useState } from 'react'
import { AccountProfile } from './AccountProfile'

export function DashboardPage() {
  const { logout, accessToken } = useAuth()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function signOut() {
    setError(''); setBusy(true)
    try { await logout() }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao sair.') }
    finally { setBusy(false) }
  }
  return <main className="min-h-screen bg-[#f3f5ef]">
    <header className="border-b border-[#dce2d8] bg-white px-6 py-4">
      <div className="mx-auto flex max-w-6xl items-center justify-between">
        <strong className="text-xl text-[#18361f]">CommerceFlow</strong>
        <button disabled={busy} onClick={() => void signOut()} className="rounded-lg border border-[#ccd3c7] px-4 py-2 text-sm font-medium">{busy ? 'Saindo…' : 'Sair'}</button>
      </div>
    </header>
    <section className="mx-auto max-w-6xl px-6 py-16">
      <p className="text-sm font-semibold uppercase tracking-[.2em] text-[#9b741a]">Minha conta</p>
      <h1 className="mt-3 max-w-3xl text-4xl font-semibold tracking-tight text-[#172017]">Bem-vindo ao CommerceFlow.</h1>
      <p className="mt-6 max-w-2xl text-lg leading-8 text-[#637064]">Confira os dados do seu perfil. A loja ainda não possui produtos disponíveis.</p>
      {error && <p role="alert" className="mt-4 text-red-700">{error}</p>}
      <div className="mt-10 rounded-2xl border border-[#dce2d8] bg-white p-6 shadow-sm">
        {accessToken && <AccountProfile accessToken={accessToken} />}
      </div>
    </section>
  </main>
}
