import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError, catalogRequest, money } from '../catalog/api'
import { StoreLayout } from '../catalog/StoreLayout'
import { RemoteState } from '../catalog/RemoteState'
import { useRemote } from '../catalog/useRemote'
import { command, saveCart, type Cart, type Command, type Quote } from './api'

export function CartPage({ checkout = false }: { checkout?: boolean }) {
  const { accessToken } = useAuth()
  const result = useRemote<Cart>('/cart', accessToken)
  return <StoreLayout><h1>{checkout ? 'Revisão do checkout' : 'Seu carrinho'}</h1>
    {result.data ? <CartEditor key={`${accessToken}:${result.data.version}`} initial={result.data} token={accessToken!} reload={result.reload} checkout={checkout} /> : <RemoteState error={result.error} retry={result.reload} />}
  </StoreLayout>
}

function CartEditor({ initial, token, reload, checkout }: { initial: Cart; token: string; reload: () => void; checkout: boolean }) {
  const [cart, setCart] = useState(initial)
  const [quote, setQuote] = useState<Quote>()
  const [quoteError, setQuoteError] = useState<Error>()
  const [attempt, setAttempt] = useState(0)
  const [busy, setBusy] = useState(false)
  const [pending, setPending] = useState<Command>()
  const [error, setError] = useState('')
  const [feedback, setFeedback] = useState('')
  useEffect(() => {
    if (!cart.items.length) return
    const controller = new AbortController()
    catalogRequest<Quote>('/cart/quote', token, { method: 'POST', signal: controller.signal })
      .then(data => {
        if (data.cartVersion !== cart.version) throw new Error('O carrinho mudou em outra aba. Recarregue a página para revisar os itens atuais.')
        if (!controller.signal.aborted) setQuote(data)
      })
      .catch((reason: Error) => { if (!controller.signal.aborted) setQuoteError(reason) })
    return () => controller.abort()
  }, [cart, token, attempt])
  function refreshQuote() { setQuote(undefined); setQuoteError(undefined); setAttempt(n => n + 1) }
  async function save(next: Cart, retry?: Command) {
    const request = retry ?? command(next)
    setBusy(true); setError(''); setFeedback(''); setPending(request)
    try {
      const updated = await saveCart(token, request)
      setCart(updated); setQuote(undefined); setQuoteError(undefined); setPending(undefined)
      setFeedback('Carrinho salvo. Os valores são recalculados pelo catálogo.')
    } catch (reason) {
      if (reason instanceof ApiError && reason.status < 500) setPending(undefined)
      setError(reason instanceof Error ? reason.message : 'Falha ao salvar.')
    } finally { setBusy(false) }
  }
  function coupon(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const code = String(new FormData(event.currentTarget).get('coupon') ?? '').trim().toUpperCase()
    void save({ ...cart, coupon: code || null })
  }
  if (!cart.items.length) return <div className="empty-state"><h2>Seu carrinho está vazio</h2><Link to="/products">Explorar a coleção</Link></div>
  const disabled = busy || !!pending
  return <><p className="notice">Revisão demonstrativa: não cria pedido, não reserva estoque e não realiza cobrança. Frete ainda não calculado.</p>
    <div className="cart-layout"><section aria-label="Itens do carrinho">{cart.items.map(item => {
      const line = quote?.items.find(i => i.sku === item.sku)
      return <article className="cart-line" key={item.sku}><div><h2>{line ? <Link to={`/products/${line.slug}`}>{line.name}</Link> : item.sku}</h2><p>SKU {item.sku}</p>{line && <p>{money(line.unitPrice)} por unidade · {money(line.total)}</p>}</div>
        {checkout ? <p>Quantidade: {item.quantity}</p> : <div className="quantity-controls"><button disabled={disabled || item.quantity <= 1} aria-label={`Diminuir ${item.sku}`} onClick={() => save({ ...cart, items: cart.items.map(i => i.sku === item.sku ? { ...i, quantity: i.quantity - 1 } : i) })}>−</button><span aria-label={`Quantidade de ${item.sku}`}>{item.quantity}</span><button disabled={disabled || item.quantity >= 99} aria-label={`Aumentar ${item.sku}`} onClick={() => save({ ...cart, items: cart.items.map(i => i.sku === item.sku ? { ...i, quantity: i.quantity + 1 } : i) })}>+</button><button disabled={disabled} onClick={() => save({ ...cart, items: cart.items.filter(i => i.sku !== item.sku) })}>Remover {item.sku}</button></div>}
      </article>
    })}</section><aside className="cart-summary"><h2>Resumo dos produtos</h2>
      {!checkout && <form onSubmit={coupon}><label>Cupom<input name="coupon" defaultValue={cart.coupon ?? ''} maxLength={32} pattern="[A-Za-z0-9-]{1,32}" /></label><button className="button" disabled={disabled}>Aplicar cupom</button>{cart.coupon && <button type="button" disabled={disabled} onClick={() => save({ ...cart, coupon: null })}>Remover cupom {cart.coupon}</button>}</form>}
      {!quote ? <RemoteState error={quoteError} retry={refreshQuote} /> : <><dl><dt>Subtotal</dt><dd>{money(quote.subtotal)}</dd><dt>Desconto</dt><dd>− {money(quote.discount)}</dd><dt>Total dos produtos</dt><dd><strong>{money(quote.total)}</strong></dd></dl><p className="muted">Cotação indicativa de {new Date(quote.quotedAt).toLocaleTimeString('pt-BR')}. Preços podem mudar; não há garantia de estoque.</p><button disabled={disabled} onClick={refreshQuote}>Atualizar valores</button>{!checkout && !disabled && <Link className="button primary" to="/checkout">Revisar checkout</Link>}</>}
    </aside></div>{error && <div role="alert">{error} {pending ? <button disabled={busy} onClick={() => save(pending.cart, pending)}>Repetir mesma operação</button> : <button onClick={reload}>Recarregar carrinho</button>}</div>}{feedback && <p role="status">{feedback}</p>}
    {checkout && <p><Link to="/cart">Voltar e editar carrinho ou cupom</Link></p>}</>
}
