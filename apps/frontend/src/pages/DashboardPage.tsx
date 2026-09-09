import { useAuth } from '../auth/AuthContext'

export function DashboardPage() {
  const { logout } = useAuth()
  return <main className="min-h-screen bg-[#f3f5ef]">
    <header className="border-b border-[#dce2d8] bg-white px-6 py-4">
      <div className="mx-auto flex max-w-6xl items-center justify-between">
        <strong className="text-xl text-[#18361f]">CommerceFlow</strong>
        <button onClick={() => void logout()} className="rounded-lg border border-[#ccd3c7] px-4 py-2 text-sm font-medium">Sair</button>
      </div>
    </header>
    <section className="mx-auto max-w-6xl px-6 py-16">
      <p className="text-sm font-semibold uppercase tracking-[.2em] text-[#9b741a]">Fundação pronta</p>
      <h1 className="mt-3 max-w-3xl text-5xl font-semibold tracking-tight text-[#172017]">Sua operação começa com uma identidade segura.</h1>
      <p className="mt-6 max-w-2xl text-lg leading-8 text-[#637064]">A conta e o perfil estão conectados. Catálogo e experiência de compra entram na próxima fase, somente após aprovação.</p>
      <div className="mt-10 rounded-2xl border border-[#dce2d8] bg-white p-6 shadow-sm">
        <span className="inline-flex rounded-full bg-[#e7f2e8] px-3 py-1 text-sm font-semibold text-[#285f33]">Sessão autenticada</span>
        <p className="mt-4 text-[#637064]">Access token mantido apenas em memória e sessão renovável por cookie seguro.</p>
      </div>
    </section>
  </main>
}
