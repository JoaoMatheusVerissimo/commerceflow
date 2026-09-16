import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { catalogRequest } from '../catalog/api'
import { StoreLayout } from '../catalog/StoreLayout'
import { useRemote } from '../catalog/useRemote'
import { RemoteState } from '../catalog/RemoteState'

type Coupon = { code: string; kind: string; amount: string; minimum: string; startsAt: string; endsAt: string; active: boolean; version: number | null }
const empty: Coupon = { code: '', kind: 'PERCENT', amount: '10', minimum: '0', startsAt: '2026-01-01T00:00', endsAt: '2030-01-01T00:00', active: true, version: null }
export function CouponsPage() {
  const { accessToken } = useAuth()
  const permission = useRemote('/admin/products?size=1', accessToken)
  return <StoreLayout><h1>Gestão de cupons</h1>{permission.data ? <CouponEditor token={accessToken!} /> : <RemoteState error={permission.error} retry={permission.reload} />}</StoreLayout>
}
function CouponEditor({ token }: { token: string }) {
  const [coupon, setCoupon] = useState(empty)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [feedback, setFeedback] = useState('')
  async function load(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(''); setFeedback('')
    try {
      const code = String(new FormData(event.currentTarget).get('code')).toUpperCase()
      const data = await catalogRequest<Coupon>(`/admin/coupons/${encodeURIComponent(code)}`, token)
      setCoupon({ ...data, startsAt: data.startsAt.slice(0, 16), endsAt: data.endsAt.slice(0, 16) })
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao carregar.') }
    finally { setBusy(false) }
  }
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(''); setFeedback('')
    try {
      const result = await catalogRequest<Coupon>('/admin/coupons', token, { method: 'PUT', body: JSON.stringify({ ...coupon, startsAt: `${coupon.startsAt}:00Z`, endsAt: `${coupon.endsAt}:00Z` }) })
      setCoupon({ ...result, startsAt: result.startsAt.slice(0, 16), endsAt: result.endsAt.slice(0, 16) }); setFeedback('Cupom salvo.')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao salvar.') }
    finally { setBusy(false) }
  }
  return <><p>Desconto sobre preços promocionais. Uma regra por carrinho, sem contabilização de resgates nesta fase.</p><form onSubmit={load}><label>Buscar código<input name="code" required maxLength={32} pattern="[A-Za-z0-9-]+" /></label><button disabled={busy}>Carregar cupom</button><button type="button" disabled={busy} onClick={() => { setCoupon(empty); setFeedback(''); setError('') }}>Novo cupom</button></form>
    <form className="editor" onSubmit={save}><div className="editor-grid"><label>Código<input required maxLength={32} pattern="[A-Z0-9-]+" disabled={coupon.version !== null} value={coupon.code} onChange={e => setCoupon({ ...coupon, code: e.target.value.toUpperCase() })} /></label>
      <label>Tipo<select value={coupon.kind} onChange={e => setCoupon({ ...coupon, kind: e.target.value })}><option value="PERCENT">Percentual</option><option value="FIXED">Valor fixo em reais</option></select></label>
      <label>Valor<input required type="number" min="0.01" max={coupon.kind === 'PERCENT' ? '100' : '9999999999.99'} step="0.01" value={coupon.amount} onChange={e => setCoupon({ ...coupon, amount: e.target.value })} /></label>
      <label>Compra mínima<input required type="number" min="0" step="0.01" value={coupon.minimum} onChange={e => setCoupon({ ...coupon, minimum: e.target.value })} /></label>
      <label>Início (UTC)<input required type="datetime-local" value={coupon.startsAt} onChange={e => setCoupon({ ...coupon, startsAt: e.target.value })} /></label><label>Fim (UTC)<input required type="datetime-local" value={coupon.endsAt} onChange={e => setCoupon({ ...coupon, endsAt: e.target.value })} /></label></div>
      <label>Estado<select value={String(coupon.active)} onChange={e => setCoupon({ ...coupon, active: e.target.value === 'true' })}><option value="true">Ativo</option><option value="false">Inativo</option></select></label><button className="button primary" disabled={busy}>Salvar cupom</button></form>
    {error && <p role="alert">{error}</p>}{feedback && <p role="status">{feedback}</p>}</>
}
