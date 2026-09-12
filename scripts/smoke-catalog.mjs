import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { setTimeout as delay } from 'node:timers/promises'

const base = process.env.SMOKE_BASE_URL ?? 'http://localhost:3000'
async function request(path, options = {}) {
  return fetch(`${base}/api/v1${path}`, { ...options, signal: AbortSignal.timeout(10000), headers: { 'Content-Type': 'application/json', ...options.headers } })
}
let listing
for (let attempt = 0; attempt < 90; attempt++) {
  try {
    const response = await request('/products?q=serra&category=botas&sort=price-asc&size=1')
    if (response.ok) { listing = await response.json(); break }
  } catch { /* Bounded startup retry; failures become an assertion below. */ }
  await delay(2000)
}
assert.ok(listing, 'Catalog did not become ready')
assert.equal(listing.items[0].slug, 'bota-serra')
assert.equal(listing.items[0].minPrice, '399.90')
assert.equal((await request('/products?size=1000')).status, 400)
const product = await (await request('/products/bota-serra')).json()
assert.equal(product.variants.length, 2)
for (const image of product.images) {
  const response = await fetch(`${base}${image.url}`)
  assert.equal(response.status, 200)
  assert.match(response.headers.get('content-type'), /image\/svg/)
}
assert.equal((await request('/admin/products')).status, 401)
const response = await request('/auth/register', { method: 'POST', body: JSON.stringify({ name: 'Catalog Smoke', email: `catalog-${randomUUID()}@example.test`, password: `Aa1-${randomUUID()}` }) })
assert.equal(response.status, 200)
const { accessToken } = await response.json()
const headers = { Authorization: `Bearer ${accessToken}` }
assert.equal((await request('/customers/me', { headers })).status, 200)
assert.equal((await request('/admin/products', { headers })).status, 403)
const review = await request(`/products/${product.id}/reviews`, { method: 'POST', headers, body: JSON.stringify({ rating: 4, comment: 'Avaliação do teste integrado.' }) })
assert.equal(review.status, 201)
const pending = await review.json()
assert.equal(pending.status, 'PENDING')
const publicReviews = await (await request(`/products/${product.id}/reviews`)).json()
assert.ok(!publicReviews.items.some(item => item.id === pending.id))
console.log('Catalog smoke passed: public browsing, images, auth integration, permissions and review privacy')
