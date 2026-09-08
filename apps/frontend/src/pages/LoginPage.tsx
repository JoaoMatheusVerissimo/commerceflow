import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'

export function LoginPage() {
  const { login, accessToken, ready } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (ready && accessToken) return <Navigate to="/app" replace />
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    const data = new FormData(event.currentTarget)
    try { await login({ email: String(data.get('email')), password: String(data.get('password')) }); navigate('/app') }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao entrar.') }
    finally { setBusy(false) }
  }
  return <AuthLayout><div className="w-full max-w-md">
    <p className="text-sm font-semibold text-[#9b741a]">BEM-VINDO DE VOLTA</p>
    <h2 className="mt-2 text-4xl font-semibold tracking-tight">Entre na sua conta</h2>
    <p className="mt-3 text-[#637064]">Acesse o centro da sua operação.</p>
    <form className="mt-9 space-y-5" onSubmit={submit}>
      <Field label="E-mail" name="email" type="email" autoComplete="email" />
      <Field label="Senha" name="password" type="password" autoComplete="current-password" />
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <button disabled={busy} className="w-full rounded-xl bg-[#18361f] px-5 py-3 font-semibold text-white hover:bg-[#244c2d] disabled:opacity-60">{busy ? 'Entrando…' : 'Entrar'}</button>
    </form>
    <p className="mt-7 text-center text-sm text-[#637064]">Ainda não tem conta? <Link className="font-semibold text-[#18361f] underline" to="/register">Cadastre-se</Link></p>
  </div></AuthLayout>
}

type FieldProps = React.InputHTMLAttributes<HTMLInputElement> & { label: string; name: string }
export function Field({ label, name, ...props }: FieldProps) {
  return <label className="block text-sm font-medium">{label}<input required name={name} {...props} className="mt-2 w-full rounded-xl border border-[#ccd3c7] bg-white px-4 py-3 outline-none focus:border-[#9b741a] focus:ring-2 focus:ring-[#d9b665]/30" /></label>
}
