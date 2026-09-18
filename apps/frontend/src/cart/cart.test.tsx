import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { CartPage } from './CartPage'
import { FavoritesPage } from './FavoritesPage'
import { ProductActions } from './ProductActions'
import { CouponsPage } from './CouponsPage'
import type { Cart, Quote } from './api'

vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ accessToken: 'access', ready: true }) }))
const item = { productId: 'product-id', sku: 'BOTA-P', quantity: 1 }
const initial: Cart = { version: 1, items: [item], coupon: null }
const quote: Quote = { cartVersion: 1, items: [{ ...item, name: 'Bota Serra', slug: 'bota-serra', unitPrice: '100.00', total: '100.00' }], subtotal: '100.00', discount: '10.00', total: '90.00', currency: 'BRL', quotedAt: '2026-09-15T12:00:00Z', expiresAt: '2026-09-15T12:05:00Z' }
const response = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status })
const wrap = (node: React.ReactNode) => <MemoryRouter>{node}</MemoryRouter>
afterEach(() => vi.unstubAllGlobals())

describe('Cart and checkout', () => {
  it('shows an empty cart without a checkout action', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({ ...initial, items: [] })))
    render(wrap(<CartPage />))
    expect(await screen.findByText('Seu carrinho está vazio')).toBeInTheDocument()
    expect(screen.queryByText('Revisar checkout')).not.toBeInTheDocument()
  })
  it('uses server totals and saves quantity with version and idempotency key', async () => {
    let cart = structuredClone(initial)
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (url.endsWith('/quote')) return response({ ...quote, cartVersion: cart.version })
      if (init?.method === 'PUT') { cart = { ...JSON.parse(String(init.body)), version: cart.version + 1 }; return response(cart) }
      return response(cart)
    })
    vi.stubGlobal('fetch', fetchMock)
    render(wrap(<CartPage />))
    expect(await screen.findByText(/90,00/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Aumentar BOTA-P' }))
    await waitFor(() => expect(screen.getByLabelText('Quantidade de BOTA-P')).toHaveTextContent('2'))
    const call = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')!
    expect(JSON.parse(String(call[1]?.body))).toMatchObject({ version: 1, items: [{ quantity: 2 }] })
    expect(call[1]?.headers).toMatchObject({ Authorization: 'Bearer access', 'Idempotency-Key': expect.any(String) })
  })
  it('retries the same uncertain command rather than adding twice', async () => {
    const requests: RequestInit[] = []
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      if (url.endsWith('/quote')) return response(quote)
      if (init?.method === 'PUT') {
        requests.push(init)
        if (requests.length === 1) throw new TypeError('Network unavailable')
        return response({ ...initial, version: 2, items: [] })
      }
      return response(initial)
    }))
    render(wrap(<CartPage />))
    fireEvent.click(await screen.findByRole('button', { name: 'Remover BOTA-P' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Repetir mesma operação' }))
    expect(await screen.findByText('Seu carrinho está vazio')).toBeInTheDocument()
    expect(requests[0].body).toBe(requests[1].body)
    expect(requests[0].headers).toEqual(requests[1].headers)
  })
  it('applies an uppercase coupon and blocks review on invalid pricing', async () => {
    let saved: Cart | undefined
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      if (init?.method === 'PUT') { saved = JSON.parse(String(init.body)); return response({ ...saved, version: 2 }) }
      if (url.endsWith('/quote')) return saved ? response({}, 422) : response(quote)
      return response(initial)
    }))
    render(wrap(<CartPage />))
    await screen.findByText(/90,00/)
    fireEvent.change(screen.getByLabelText('Cupom'), { target: { value: 'invalido' } })
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar cupom' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('cupom inválido')
    expect(saved?.coupon).toBe('INVALIDO')
    expect(screen.queryByText('Revisar checkout')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Remover BOTA-P' })).toBeEnabled()
  })
  it('displays a conflict with a reload action', async () => {
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => init?.method === 'PUT' ? response({}, 409) : response(url.endsWith('/quote') ? quote : initial)))
    render(wrap(<CartPage />))
    fireEvent.click(await screen.findByRole('button', { name: 'Aumentar BOTA-P' }))
    expect(await screen.findByRole('button', { name: 'Recarregar carrinho' })).toBeInTheDocument()
  })
  it('does not display a quote for a different cart version', async () => {
    vi.stubGlobal('fetch', vi.fn(async (url: string) => response(url.endsWith('/quote') ? { ...quote, cartVersion: 2 } : initial)))
    render(wrap(<CartPage />))
    expect(await screen.findByRole('alert')).toHaveTextContent('outra aba')
    expect(screen.queryByText(/90,00/)).not.toBeInTheDocument()
  })
  it('creates an order with server totals and stock reservation without a payment action', async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => response(url.endsWith('/checkout') && init?.method === 'POST' ? {
      id: '12345678-order', status: 'PAYMENT_PENDING', total: '90.00', items: quote.items,
    } : url.endsWith('/quote') ? quote : initial))
    vi.stubGlobal('fetch', fetchMock)
    render(wrap(<CartPage checkout />))
    expect(await screen.findByText(/90,00/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar pedido e reservar estoque' }))
    expect(await screen.findByText(/Estoque reservado com sucesso/)).toBeInTheDocument()
    const request = fetchMock.mock.calls.find(([url]) => String(url).endsWith('/checkout'))!
    expect(request[1]?.headers).toMatchObject({ 'Idempotency-Key': expect.any(String) })
    expect(JSON.parse(String(request[1]?.body))).toEqual({ cartVersion: 1 })
    expect(screen.queryByRole('button', { name: /pagar|finalizar/i })).not.toBeInTheDocument()
  })
})

describe('Favorites and product actions', () => {
  it('adds selected SKU and saves favorite through authenticated APIs', async () => {
    const fetchMock = vi.fn(async (_url: string, init?: RequestInit) => response(init?.method === 'PUT' ? { ...initial, version: 2 } : { ...initial, items: [] }))
    vi.stubGlobal('fetch', fetchMock)
    render(wrap(<ProductActions productId="product-id" sku="BOTA-M" />))
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar ao carrinho' }))
    expect(await screen.findByRole('status')).toHaveTextContent('adicionado')
    const request = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')!
    expect(JSON.parse(String(request[1]?.body)).items[0].sku).toBe('BOTA-M')
    fireEvent.click(screen.getByRole('button', { name: 'Salvar nos favoritos' }))
    expect(await screen.findByText(/Produto salvo nos favoritos/)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/customers/me/favorites/product-id', expect.objectContaining({ method: 'PUT' }))
  })
  it('can remove a favorite whose product is no longer public', async () => {
    let removed = false
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      if (init?.method === 'DELETE') { removed = true; return response({ saved: false }) }
      if (url.includes('/products/')) return response({}, 404)
      return response({ items: removed ? [] : [{ productId: 'missing' }], totalPages: 1 })
    }))
    render(wrap(<FavoritesPage />))
    expect(await screen.findByRole('alert')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Remover favorito' }))
    expect(await screen.findByText(/Nenhum favorito/)).toBeInTheDocument()
  })
  it('hides coupon administration after permission denial', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({}, 403)))
    render(wrap(<CouponsPage />))
    expect(await screen.findByRole('alert')).toHaveTextContent('permissão')
    expect(screen.queryByRole('button', { name: 'Salvar cupom' })).not.toBeInTheDocument()
  })
})
