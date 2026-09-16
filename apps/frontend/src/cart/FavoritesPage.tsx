import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { catalogRequest, type Product } from '../catalog/api'
import { ProductCard } from '../catalog/ProductCard'
import { RemoteState } from '../catalog/RemoteState'
import { StoreLayout } from '../catalog/StoreLayout'
import { useRemote } from '../catalog/useRemote'

type Favorites = { items: { productId: string }[]; totalPages: number }
export function FavoritesPage() {
  const { accessToken } = useAuth()
  const [page, setPage] = useState(0)
  const result = useRemote<Favorites>(`/customers/me/favorites?page=${page}`, accessToken)
  return <StoreLayout><h1>Seus favoritos</h1>{!result.data ? <RemoteState error={result.error} retry={result.reload} /> : <>
    {!result.data.items.length ? <p className="empty-state">Nenhum favorito nesta página. <Link to="/products">Explorar produtos</Link></p> : <div className="product-grid">{result.data.items.map(item => <Favorite key={item.productId} id={item.productId} token={accessToken!} reload={result.reload} />)}</div>}
    <nav className="pagination"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Anteriores</button><span>Página {page + 1}</span><button disabled={page + 1 >= result.data.totalPages} onClick={() => setPage(page + 1)}>Próximos</button></nav></>}
  </StoreLayout>
}
function Favorite({ id, token, reload }: { id: string; token: string; reload: () => void }) {
  const result = useRemote<Product>(`/products/by-id/${id}`)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function remove() {
    setBusy(true); setError('')
    try { await catalogRequest(`/customers/me/favorites/${id}`, token, { method: 'DELETE' }); reload() }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao remover.'); setBusy(false) }
  }
  return <div>{result.data ? <ProductCard product={result.data} /> : <RemoteState error={result.error} retry={result.reload} />}
    <button className="button" disabled={busy} onClick={remove}>Remover favorito</button>{error && <p role="alert">{error}</p>}</div>
}
