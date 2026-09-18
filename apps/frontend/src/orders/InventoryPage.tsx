import { useAuth } from '../auth/AuthContext'
import { RemoteState } from '../catalog/RemoteState'
import { StoreLayout } from '../catalog/StoreLayout'
import { useRemote } from '../catalog/useRemote'

type Stock = { sku: string; physical: number; reserved: number; available: number; minimum: number; version: number; lowStock: boolean }
export function InventoryPage() {
  const { accessToken } = useAuth(); const result = useRemote<Stock[]>('/admin/inventory', accessToken)
  return <StoreLayout><p className="eyebrow">Operação</p><h1>Estoque</h1><p className="notice">Saldos e reservas reais do Inventory Service. Movimentações são auditadas pela API administrativa.</p>
    {!result.data ? <RemoteState error={result.error} retry={result.reload} /> : !result.data.length ? <div className="empty-state">Nenhum SKU cadastrado.</div> : <div className="table-scroll"><table><thead><tr><th>SKU</th><th>Físico</th><th>Reservado</th><th>Disponível</th><th>Mínimo</th><th>Situação</th></tr></thead><tbody>{result.data.map(stock => <tr key={stock.sku}><td>{stock.sku}</td><td>{stock.physical}</td><td>{stock.reserved}</td><td>{stock.available}</td><td>{stock.minimum}</td><td><span className={stock.lowStock ? 'status danger' : 'status'}>{stock.lowStock ? 'Estoque baixo' : 'Normal'}</span></td></tr>)}</tbody></table></div>}
  </StoreLayout>
}
