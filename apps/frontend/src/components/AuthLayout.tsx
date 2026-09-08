import type { PropsWithChildren } from 'react'

export function AuthLayout({ children }: PropsWithChildren) {
  return <main className="grid min-h-screen lg:grid-cols-[1.1fr_.9fr]">
    <section className="hidden bg-[#18361f] p-14 text-[#f7f1dd] lg:flex lg:flex-col lg:justify-between">
      <div className="text-xl font-semibold tracking-tight">CommerceFlow</div>
      <div className="max-w-xl">
        <p className="mb-5 text-sm font-medium uppercase tracking-[.24em] text-[#d9b665]">Operação conectada</p>
        <h1 className="text-5xl font-semibold leading-[1.05]">Comércio, relacionamento e decisões no mesmo fluxo.</h1>
        <p className="mt-6 max-w-lg text-lg leading-8 text-[#cbd6c7]">Uma base segura para crescer sem perder a visão do cliente.</p>
      </div>
      <p className="text-sm text-[#9fb09f]">Fundação CommerceFlow · Fase 1</p>
    </section>
    <section className="flex items-center justify-center px-6 py-12 sm:px-12">{children}</section>
  </main>
}
