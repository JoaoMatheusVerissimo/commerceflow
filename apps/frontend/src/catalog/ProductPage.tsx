import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { catalogRequest, money, type Page, type Product, type Review } from './api'
import { useRemote } from './useRemote'
import { StoreLayout } from './StoreLayout'
import { RemoteState } from './RemoteState'

export function ProductPage() {
  const { slug } = useParams()
  const result = useRemote<Product>(`/products/${encodeURIComponent(slug ?? '')}`)
  return <StoreLayout>{result.data ? <ProductDetail key={result.data.id} product={result.data} /> : <RemoteState error={result.error} retry={result.reload} />}</StoreLayout>
}

export function ProductDetail({ product }: { product: Product }) {
  const [image, setImage] = useState(0)
  const [sku, setSku] = useState(product.variants[0].sku)
  const variant = product.variants.find(v => v.sku === sku) ?? product.variants[0]
  return <><nav className="breadcrumbs" aria-label="Caminho"><Link to="/products">Coleção</Link> / <Link to={`/categories/${product.category.slug}`}>{product.category.name}</Link> / {product.name}</nav>
    <section className="product-detail"><div><img className="product-main-image" src={product.images[image].url} alt={product.images[image].alt} width="520" height="480" />
      <div className="thumbnails">{product.images.map((img, index) => <button key={img.url + index} aria-label={`Ver imagem ${index + 1}`} aria-pressed={image === index} onClick={() => setImage(index)}><img src={img.url} alt={img.alt} width="90" height="80" /></button>)}</div></div>
      <div className="product-info"><p className="eyebrow">{product.category.name}</p><h1>{product.name}</h1><p className="muted">SKU {variant.sku}</p>
        <div className="detail-price">{variant.promotionalPrice && <del>{money(variant.price)}</del>}<strong>{money(variant.effectivePrice)}</strong></div>
        <p>{product.description}</p><fieldset><legend>Escolha a variação</legend><div className="variant-options">{product.variants.map(v => <button key={v.sku} aria-pressed={sku === v.sku} onClick={() => setSku(v.sku)}>{v.color} · {v.size}</button>)}</div></fieldset>
        <p className="notice">Catálogo demonstrativo. Compra e disponibilidade de estoque ainda não estão habilitadas.</p><p className="muted">Imagens ilustrativas da coleção demo.</p>
      </div></section><Reviews productId={product.id} /></>
}

function Reviews({ productId }: { productId: string }) {
  const { accessToken } = useAuth()
  const [page, setPage] = useState(0)
  const result = useRemote<Page<Review>>(`/products/${productId}/reviews?page=${page}`)
  const [busy, setBusy] = useState(false)
  const [feedback, setFeedback] = useState('')
  const [error, setError] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(''); setFeedback('')
    const form = event.currentTarget
    const data = new FormData(form)
    try {
      await catalogRequest(`/products/${productId}/reviews`, accessToken, { method: 'POST', body: JSON.stringify({ rating: Number(data.get('rating')), comment: data.get('comment') }) })
      setFeedback('Avaliação enviada e aguardando moderação.'); form.reset()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao enviar avaliação.') }
    finally { setBusy(false) }
  }
  return <section className="reviews"><h2>Avaliações</h2><p className="muted">Opiniões moderadas. Não há verificação de compra nesta etapa.</p>
    {!result.data ? <RemoteState error={result.error} retry={result.reload} /> : <>{result.data.items.length === 0 ? <p className="empty-state">Ainda não há avaliações publicadas.</p> : result.data.items.map(r => <article className="review" key={r.id}><strong>Nota {r.rating} de 5</strong><p>{r.comment}</p><small>{new Date(r.createdAt).toLocaleDateString('pt-BR')}</small></article>)}
      {result.data.totalPages > 1 && <nav className="pagination"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Anteriores</button><span>{page + 1} / {result.data.totalPages}</span><button disabled={page + 1 >= result.data.totalPages} onClick={() => setPage(page + 1)}>Próximas</button></nav>}</>}
    {accessToken ? <form className="review-form" onSubmit={submit}><h3>Compartilhe sua opinião</h3><label>Nota<select name="rating" defaultValue="5">{[5, 4, 3, 2, 1].map(n => <option key={n} value={n}>{n}</option>)}</select></label><label>Comentário<textarea name="comment" required maxLength={2000} rows={4} /></label>
      {error && <p role="alert">{error}</p>}{feedback && <p role="status">{feedback}</p>}<button className="button primary" disabled={busy}>{busy ? 'Enviando…' : 'Enviar avaliação'}</button></form> : <p><Link to="/login">Entre na sua conta</Link> para avaliar.</p>}
  </section>
}
