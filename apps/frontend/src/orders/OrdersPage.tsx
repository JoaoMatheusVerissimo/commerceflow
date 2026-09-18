import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { money } from '../catalog/api'
import { RemoteState } from '../catalog/RemoteState'
import { StoreLayout } from '../catalog/StoreLayout'
import { useRemote } from '../catalog/useRemote'
import { orderStatus, type OrderPage } from './api'

export function OrdersPage({ admin = false }: { admin?: boolean }) {
  const { accessToken } = useAuth()
  const result = useRemote<OrderPage>(admin ? '/admin/orders' : '/orders', accessToken)
  return <StoreLayout><div className="collection-heading"><div><p className="eyebrow">{admin ? 'Operação' : 'Minha conta'}</p><h1>{admin ? 'Pedidos' : 'Meus pedidos'}</h1></div></div>
    {!result.data ? <RemoteState error={result.error} retry={result.reload} /> : !result.data.items.length
      ? <div className="empty-state"><h2>Nenhum pedido encontrado</h2>{!admin && <Link to="/products">Explorar produtos</Link>}</div>
      : <div className="table-scroll"><table><thead><tr><th>Pedido</th>{admin && <th>Cliente</th>}<th>Data</th><th>Status</th><th>Total</th></tr></thead><tbody>{result.data.items.map(order => <tr key={order.id}><td><Link to={`/account/orders/${order.id}`}>{order.id.slice(0, 8)}</Link></td>{admin && <td>{order.customerId.slice(0, 8)}</td>}<td>{new Date(order.createdAt).toLocaleString('pt-BR')}</td><td>{orderStatus[order.status] ?? order.status}</td><td>{money(order.total)}</td></tr>)}</tbody></table></div>}
  </StoreLayout>
}
