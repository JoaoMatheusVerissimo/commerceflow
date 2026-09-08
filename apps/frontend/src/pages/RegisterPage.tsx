import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { Field } from './LoginPage'

export function RegisterPage() {
  const { register } = useAuth(); const navigate = useNavigate()
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    const data = new FormData(event.currentTarget)
    try { await register({ name: String(data.get('name')), email: String(data.get('email')), password: String(data.get('password')) }); navigate('/app') }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao cadastrar.') }
    finally { setBusy(false) }
  }
  return <AuthLayout><div className="w-full max-w-md">
    <p className="text-sm font-semibold text-[#9b741a]">COMECE PELO ESSENCIAL</p>
    <h2 className="mt-2 text-4xl font-semibold tracking-tight">Crie sua conta</h2>
    <p className="mt-3 text-[#637064]">Seu perfil de cliente será criado com segurança.</p>
    <form className="mt-8 space-y-4" onSubmit={submit}>
      <Field label="Nome" name="name" autoComplete="name" minLength={2} maxLength={120} />
      <Field label="E-mail" name="email" type="email" autoComplete="email" />
      <Field label="Senha" name="password" type="password" autoComplete="new-password" minLength={12} pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{12,}" title="Use 12 caracteres, maiúscula, minúscula e número." />
      <p className="text-xs leading-5 text-[#637064]">Mínimo de 12 caracteres, com maiúscula, minúscula e número.</p>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <button disabled={busy} className="w-full rounded-xl bg-[#18361f] px-5 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Criando…' : 'Criar conta'}</button>
    </form>
    <p className="mt-6 text-center text-sm text-[#637064]">Já tem conta? <Link className="font-semibold text-[#18361f] underline" to="/login">Entrar</Link></p>
  </div></AuthLayout>
}
