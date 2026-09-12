import { type FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import type { Category, Page, Product } from './api'
import { useRemote } from './useRemote'
import { StoreLayout } from './StoreLayout'
import { RemoteState } from './RemoteState'
import { ProductCard } from './ProductCard'

export function CatalogPage({ home = false }: { home?: boolean }) {
  const { slug } = useParams()
  const [params, setParams] = useSearchParams()
  const query = new URLSearchParams(params)
  if (slug) query.set('category', slug)
  query.set('size', '12')
  const result = useRemote<Page<Product>>(`/products?${query.toString()}`)
  const categories = useRemote<Category[]>('/categories')
  const currentPage = Number(params.get('page') ?? 0)
  function filter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const next = new URLSearchParams()
    for (const [key, value] of data) if (String(value).trim()) next.set(key, String(value).trim())
    setParams(next)
  }
  return <StoreLayout>
    {home && <section className="store-hero"><div><p className="eyebrow">DESENHO ATEMPORAL. ESPÍRITO LIVRE.</p>
      <h1>Feito para<br />seguir seu caminho.</h1><p>Explore botas, chapéus e acessórios em uma coleção inspirada na vida ao ar livre.</p><Link className="button primary" to="/products">Explorar a coleção ↗</Link></div>
      <img src="/catalog-images/boot.svg" alt="Ilustração de uma bota western marrom" width="520" height="480" /></section>}
    <section className="collection-heading"><div><p className="eyebrow">A COLEÇÃO</p><h1>{slug ? categories.data?.find(c => c.slug === slug)?.name ?? 'Categoria' : 'Escolha seu próximo essencial'}</h1></div>
      <p className="muted">{result.data ? `${result.data.totalElements} produtos encontrados` : 'Catálogo CommerceFlow'}</p></section>
    <form key={`${params.toString()}:${slug ?? ''}`} className="catalog-filters" onSubmit={filter} aria-label="Filtros do catálogo">
      <label>Buscar<input name="q" type="search" defaultValue={params.get('q') ?? ''} maxLength={100} placeholder="Nome ou descrição" /></label>
      {!slug && <label>Categoria<select name="category" defaultValue={params.get('category') ?? ''}><option value="">Todas</option>{categories.data?.map(c => <option key={c.id} value={c.slug}>{c.name}</option>)}</select></label>}
      <label>Preço mínimo<input name="minPrice" type="number" min="0" step="0.01" defaultValue={params.get('minPrice') ?? ''} /></label>
      <label>Preço máximo<input name="maxPrice" type="number" min="0" step="0.01" defaultValue={params.get('maxPrice') ?? ''} /></label>
      <label>Ordenar<select name="sort" defaultValue={params.get('sort') ?? 'newest'}><option value="newest">Mais recentes</option><option value="price-asc">Menor preço</option><option value="price-desc">Maior preço</option><option value="name">Nome</option></select></label>
      <button className="button primary">Aplicar</button><button className="button" type="button" onClick={() => setParams({})}>Limpar</button>
    </form>
    {categories.error && <div role="alert" className="notice">Não foi possível carregar as categorias. <button onClick={categories.reload}>Tentar novamente</button></div>}
    {!result.data ? <RemoteState error={result.error} retry={result.reload} /> : result.data.items.length === 0
      ? <section className="empty-state"><h2>Nenhum produto encontrado</h2><p>Tente outra busca ou remova os filtros.</p><Link to="/products">Ver toda a coleção</Link></section>
      : <><div className="product-grid">{result.data.items.map(p => <ProductCard key={p.id} product={p} />)}</div>
        <nav className="pagination" aria-label="Paginação"><button disabled={currentPage <= 0} onClick={() => { const next = new URLSearchParams(params); next.set('page', String(currentPage - 1)); setParams(next) }}>Anterior</button><span>Página {currentPage + 1} de {result.data.totalPages}</span><button disabled={currentPage + 1 >= result.data.totalPages} onClick={() => { const next = new URLSearchParams(params); next.set('page', String(currentPage + 1)); setParams(next) }}>Próxima</button></nav></>}
  </StoreLayout>
}
