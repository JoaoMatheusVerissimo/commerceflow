import type { PropsWithChildren } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function StoreLayout({ children }: PropsWithChildren) {
  const { accessToken } = useAuth()
  return <div className="store">
    <div className="store-note">Coleção demonstrativa · Produtos ilustrados · Sem vendas nesta etapa</div>
    <header className="store-header"><Link className="store-brand" to="/">CommerceFlow<span>ESSENCIAIS DO CAMPO</span></Link>
      <nav aria-label="Navegação principal"><Link to="/products">Coleção</Link><Link to="/categories/botas">Botas</Link><Link to="/categories/chapeus">Chapéus</Link>
        <Link to="/favorites">Favoritos</Link><Link to="/cart">Carrinho</Link><Link to="/account/orders">Pedidos</Link>
        <Link to={accessToken ? '/account' : '/login'}>{accessToken ? 'Minha conta' : 'Entrar'}</Link></nav>
    </header>
    <main className="store-main">{children}</main>
    <footer className="store-footer"><strong>CommerceFlow</strong><p>Explore, favorite, reserve estoque e acompanhe seus pedidos. Pagamentos chegam na próxima fase.</p><Link to="/admin/products">Catálogo</Link> · <Link to="/admin/coupons">Cupons</Link> · <Link to="/admin/inventory">Estoque</Link> · <Link to="/admin/orders">Pedidos</Link></footer>
  </div>
}
