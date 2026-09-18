import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { InventoryPage } from './InventoryPage'
import { OrderDetailPage } from './OrderDetailPage'
import { OrdersPage } from './OrdersPage'

vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ accessToken: 'access', ready: true }) }))
const order = { id: '12345678-0000-0000-0000-000000000000', customerId: '87654321-0000-0000-0000-000000000000', status: 'PAYMENT_PENDING', items: [{ productId: 'product', sku: 'BOTA-P', name: 'Bota Serra', slug: 'bota-serra', quantity: 1, unitPrice: '100.00', total: '100.00' }], subtotal: '100.00', discount: '10.00', shipping: '0.00', total: '90.00', currency: 'BRL', coupon: 'DEMO10', reservationId: 'reservation', failureCode: null, createdAt: '2026-09-18T00:00:00Z', updatedAt: '2026-09-18T00:00:01Z', version: 1 }
const response = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status })
afterEach(() => vi.unstubAllGlobals())

describe('Orders and inventory', () => {
  it('renders customer order history and immutable detail snapshots', async () => {
    vi.stubGlobal('fetch', vi.fn(async (url: string) => response(String(url).endsWith('/orders')
      ? { items: [order], page: 0, size: 20, totalElements: 1, totalPages: 1 } : order)))
    const { unmount } = render(<MemoryRouter><OrdersPage /></MemoryRouter>)
    expect(await screen.findByText('12345678')).toBeInTheDocument(); unmount()
    render(<MemoryRouter initialEntries={[`/account/orders/${order.id}`]}><Routes><Route path="/account/orders/:id" element={<OrderDetailPage />} /></Routes></MemoryRouter>)
    expect(await screen.findByText('Bota Serra')).toBeInTheDocument()
    expect(screen.getByText(/nenhuma cobrança/i)).toBeInTheDocument()
  })
  it('renders operational inventory and low-stock state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response([{ sku: 'BOTA-P', physical: 1, reserved: 1, available: 0, minimum: 1, version: 1, lowStock: true }])))
    render(<MemoryRouter><InventoryPage /></MemoryRouter>)
    expect(await screen.findByText('Estoque baixo')).toBeInTheDocument()
    expect(screen.getByText('BOTA-P')).toBeInTheDocument()
  })
  it('shows permission denial instead of operational data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({}, 403)))
    render(<MemoryRouter><OrdersPage admin /></MemoryRouter>)
    expect(await screen.findByRole('alert')).toHaveTextContent('permissão')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
})
