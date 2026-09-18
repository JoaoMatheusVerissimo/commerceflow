import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { money } from '../catalog/api'
import { RemoteState } from '../catalog/RemoteState'
import { StoreLayout } from '../catalog/StoreLayout'
import { useRemote } from '../catalog/useRemote'
import { orderStatus, type Order } from './api'

export function OrderDetailPage() {
  const { id = '' } = useParams(); const { accessToken } = useAuth()
  const result = useRemote<Order>(`/orders/${id}`, accessToken)
  return <StoreLayout>{!result.data ? <RemoteState error={result.error} retry={result.reload} /> : <>
    <p className="eyebrow">Pedido {result.data.id.slice(0, 8)}</p><h1>{orderStatus[result.data.status] ?? result.data.status}</h1>
    {result.data.status === 'PAYMENT_PENDING' && <p className="notice">A reserva foi confirmada. O pagamento será implementado na Fase 5; nenhuma cobrança foi realizada.</p>}
    {result.data.failureCode && <p role="alert" className="notice">Pedido cancelado: estoque insuficiente para concluir a reserva.</p>}
    <div className="cart-layout"><section>{result.data.items.map(item => <article className="cart-line" key={item.sku}><h2><Link to={`/products/${item.slug}`}>{item.name}</Link></h2><p>SKU {item.sku} · Quantidade {item.quantity}</p><p>{money(item.unitPrice)} · {money(item.total)}</p></article>)}</section><aside className="cart-summary"><h2>Resumo</h2><dl><dt>Subtotal</dt><dd>{money(result.data.subtotal)}</dd><dt>Desconto</dt><dd>− {money(result.data.discount)}</dd><dt>Frete</dt><dd>{money(result.data.shipping)}</dd><dt>Total</dt><dd><strong>{money(result.data.total)}</strong></dd></dl><p className="muted">Criado em {new Date(result.data.createdAt).toLocaleString('pt-BR')}</p></aside></div>
    <Link to="/account/orders">Voltar ao histórico</Link></>}
  </StoreLayout>
}
