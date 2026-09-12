import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { CatalogPage } from './CatalogPage'
import { ProductDetail } from './ProductPage'
import { ProductEditor } from './ProductEditor'
import { AdminCatalog } from './AdminCatalog'
import type { Product } from './api'

const product: Product = { id: 'product-id', name: 'Bota Serra', slug: 'bota-serra', description: 'Couro marrom.', status: 'ACTIVE', version: 0, minPrice: '99.90', currency: 'BRL',
  category: { id: 'category-id', name: 'Botas', slug: 'botas', version: 0 }, images: [{ url: '/catalog-images/boot.svg', alt: 'Bota marrom' }, { url: '/catalog-images/boot-detail.svg', alt: 'Detalhe da bota' }],
  variants: [{ sku: 'SERRA-P', color: 'Marrom', size: 'P', price: '120.00', promotionalPrice: '99.90', effectivePrice: '99.90' }, { sku: 'SERRA-M', color: 'Areia', size: 'M', price: '140.00', promotionalPrice: null, effectivePrice: '140.00' }] }
const page = { items: [product], page: 0, size: 12, totalElements: 1, totalPages: 1 }
function respond(value: unknown, status = 200) { return new Response(JSON.stringify(value), { status }) }
function mockApi() {
  const mock = vi.fn(async (url: string) => {
    if (url.includes('/auth/refresh')) return respond({}, 401)
    if (url.includes('/categories')) return respond([product.category])
    if (url.includes('/reviews')) return respond({ ...page, items: [], totalElements: 0 })
    return respond(page)
  })
  vi.stubGlobal('fetch', mock)
  return mock
}
function wrap(node: React.ReactNode) { return <MemoryRouter><AuthProvider>{node}</AuthProvider></MemoryRouter> }
afterEach(() => vi.unstubAllGlobals())

describe('Catalog', () => {
  it('loads products and serializes submitted filters into the API URL', async () => {
    const fetchMock = mockApi()
    render(wrap(<CatalogPage />))
    expect(await screen.findByText('Bota Serra')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Buscar'), { target: { value: 'Serra' } })
    fireEvent.change(screen.getByLabelText('Preço mínimo'), { target: { value: '99' } })
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }))
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url.includes('q=Serra') && url.includes('minPrice=99'))).toBe(true))
  })
  it('shows a useful empty state without fictitious products', async () => {
    mockApi().mockImplementation(async url => url.includes('/categories') ? respond([]) : url.includes('/auth') ? respond({}, 401) : respond({ ...page, items: [], totalElements: 0 }))
    render(wrap(<CatalogPage />))
    expect(await screen.findByText('Nenhum produto encontrado')).toBeInTheDocument()
    expect(screen.queryByText('Bota Serra')).not.toBeInTheDocument()
  })
  it('shows an error and supports retrying a failed catalog request', async () => {
    const fetchMock = mockApi().mockImplementation(async url => url.includes('/categories') ? respond([]) : respond({}, 503))
    render(wrap(<CatalogPage />))
    expect(await screen.findByRole('alert')).toBeInTheDocument()
    fetchMock.mockImplementation(async () => respond(page))
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByText('Bota Serra')).toBeInTheDocument()
  })
  it('changes the selected SKU, price and gallery image', async () => {
    mockApi()
    render(wrap(<ProductDetail product={product} />))
    fireEvent.click(screen.getByRole('button', { name: 'Areia · M' }))
    expect(screen.getByText('SKU SERRA-M')).toBeInTheDocument()
    expect(screen.getByText(/140,00/)).toBeInTheDocument()
    expect(screen.queryByText(/99,90/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ver imagem 2' }))
    expect(screen.getAllByAltText('Detalhe da bota')[0]).toHaveAttribute('src', '/catalog-images/boot-detail.svg')
    expect(await screen.findByText('Ainda não há avaliações publicadas.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /carrinho/i })).not.toBeInTheDocument()
  })
  it('does not show administrative data after permission denial', async () => {
    mockApi().mockImplementation(async url => url.includes('/categories') ? respond([]) : respond({}, 403))
    render(wrap(<AdminCatalog />))
    expect(await screen.findByRole('alert')).toHaveTextContent('Você não tem permissão')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
  it('saves a product with its concurrency version and bearer token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respond(product))
    vi.stubGlobal('fetch', fetchMock)
    render(<MemoryRouter initialEntries={['/edit']}><Routes><Route path="/edit" element={<ProductEditor product={product} categories={[product.category]} token="access" />} /><Route path="/admin/products" element={<p>Salvo</p>} /></Routes></MemoryRouter>)
    fireEvent.click(screen.getByRole('button', { name: 'Salvar produto' }))
    expect(await screen.findByText('Salvo')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/products/product-id', expect.objectContaining({ method: 'PUT', headers: expect.objectContaining({ Authorization: 'Bearer access' }), body: expect.stringContaining('"version":0') }))
  })
})
