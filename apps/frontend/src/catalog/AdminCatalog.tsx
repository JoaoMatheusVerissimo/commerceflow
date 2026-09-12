import { useState, type FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { catalogRequest, money, type Category, type Page, type Product, type Review } from './api'
import { useRemote } from './useRemote'
import { StoreLayout } from './StoreLayout'
import { RemoteState } from './RemoteState'
import { ProductEditor } from './ProductEditor'

export function AdminCatalog({ mode = 'list' }: { mode?: 'list' | 'new' | 'edit' }) {
  const { accessToken } = useAuth()
  const { id } = useParams()
  const location = useLocation()
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const result = useRemote<Page<Product>>(`/admin/products?page=${page}&q=${encodeURIComponent(query)}`, accessToken)
  const categories = useRemote<Category[]>('/categories')
  return <StoreLayout><div className="admin-heading"><div><p className="eyebrow">GESTÃO · CATÁLOGO</p><h1>Produtos e categorias</h1></div><Link className="button primary" to="/admin/products/new">Novo produto</Link></div>
    {!result.data ? <RemoteState error={result.error} retry={result.reload} /> : !categories.data ? <RemoteState error={categories.error} retry={categories.reload} />
      : mode === 'new' ? <ProductEditor token={accessToken!} categories={categories.data} />
        : mode === 'edit' ? <EditProduct id={id!} token={accessToken!} categories={categories.data} />
          : <>{location.state?.saved && <p role="status" className="notice">Produto salvo com sucesso.</p>}
            <form className="actions" onSubmit={e => { e.preventDefault(); setPage(0); setQuery(String(new FormData(e.currentTarget).get('q') ?? '')) }}><label>Buscar produto<input name="q" maxLength={100} type="search" /></label><button className="button">Buscar</button></form>
            <div className="table-scroll"><table><thead><tr><th>Produto</th><th>Categoria</th><th>Preço inicial</th><th>Status</th><th>Ação</th></tr></thead><tbody>{result.data.items.map(p => <tr key={p.id}><td>{p.name}<small>{p.slug}</small></td><td>{p.category.name}</td><td>{money(p.minPrice)}</td><td>{p.status}</td><td><Link to={`/admin/products/${p.id}/edit`}>Editar</Link></td></tr>)}</tbody></table></div>
            {result.data.items.length === 0 && <p className="empty-state">Nenhum produto cadastrado para esta busca.</p>}
            <nav className="pagination"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button><span>Página {page + 1}</span><button disabled={page + 1 >= result.data.totalPages} onClick={() => setPage(page + 1)}>Próxima</button></nav>
            <Categories categories={categories.data} token={accessToken!} reload={categories.reload} />
            <Moderation token={accessToken!} /></>}
  </StoreLayout>
}

function EditProduct({ id, token, categories }: { id: string; token: string; categories: Category[] }) {
  const result = useRemote<Product>(`/admin/products/${id}`, token)
  return result.data ? <ProductEditor key={`${id}:${result.data.version}`} product={result.data} categories={categories} token={token} /> : <RemoteState error={result.error} retry={result.reload} />
}

function Categories({ categories, token, reload }: { categories: Category[]; token: string; reload: () => void }) {
  const [selected, setSelected] = useState<Category>()
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true); setSaved(false)
    const data = new FormData(event.currentTarget)
    try {
      await catalogRequest(selected ? `/admin/categories/${selected.id}` : '/admin/categories', token, { method: selected ? 'PUT' : 'POST', body: JSON.stringify({ name: data.get('name'), slug: data.get('slug'), version: selected?.version }) })
      setSelected(undefined); setSaved(true); reload()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao salvar.') }
    finally { setBusy(false) }
  }
  return <section className="admin-section"><h2>Categorias</h2><div className="category-tags">{categories.map(c => <button key={c.id} onClick={() => { setSelected(c); setSaved(false) }}>{c.name} · Editar</button>)}</div>
    <form key={selected?.id ?? 'new'} className="catalog-filters" onSubmit={submit}><label>Nome<input name="name" required maxLength={80} defaultValue={selected?.name} /></label><label>Slug<input name="slug" required pattern="[a-z0-9]+(-[a-z0-9]+)*" maxLength={100} defaultValue={selected?.slug} /></label><button className="button primary" disabled={busy}>{busy ? 'Salvando…' : selected ? 'Salvar categoria' : 'Criar categoria'}</button>{selected && <button className="button" type="button" onClick={() => setSelected(undefined)}>Cancelar edição</button>}</form>
    {error && <p role="alert">{error}</p>}{saved && <p role="status">Categoria salva.</p>}</section>
}

function Moderation({ token }: { token: string }) {
  const [page, setPage] = useState(0)
  const result = useRemote<Page<Review>>(`/admin/reviews?page=${page}`, token)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function moderate(review: Review, status: string) {
    setError(''); setBusy(true)
    try { await catalogRequest(`/admin/reviews/${review.id}`, token, { method: 'PATCH', body: JSON.stringify({ status, version: review.version }) }); result.reload() }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao moderar.') }
    finally { setBusy(false) }
  }
  return <section className="admin-section"><h2>Avaliações pendentes</h2>{error && <p role="alert">{error}</p>}
    {!result.data ? <RemoteState error={result.error} retry={result.reload} /> : <>{result.data.items.length === 0 ? <p>Nenhuma avaliação aguardando moderação.</p> : result.data.items.map(r => <article className="review" key={r.id}><p className="muted">Produto {r.productId}</p><strong>Nota {r.rating}/5</strong><p>{r.comment}</p><div className="actions"><button disabled={busy} onClick={() => void moderate(r, 'APPROVED')}>Aprovar</button><button disabled={busy} onClick={() => void moderate(r, 'REJECTED')}>Rejeitar</button></div></article>)}
      <nav className="pagination"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button><span>Página {page + 1}</span><button disabled={page + 1 >= result.data.totalPages} onClick={() => setPage(page + 1)}>Próxima</button></nav></>}
  </section>
}
