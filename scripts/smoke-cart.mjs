import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { setTimeout as delay } from 'node:timers/promises'

const base = process.env.SMOKE_BASE_URL ?? 'http://localhost:3000'
async function request(path, token, method = 'GET', body, key) {
  return fetch(`${base}/api/v1${path}`, { method, signal: AbortSignal.timeout(10000), headers: {
    'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(key ? { 'Idempotency-Key': key } : {}),
  }, ...(body ? { body: JSON.stringify(body) } : {}) })
}
async function customer() {
  const response = await request('/auth/register', null, 'POST', { name: 'Cart Smoke', email: `cart-${randomUUID()}@example.test`, password: `Aa1-${randomUUID()}` })
  assert.equal(response.status, 200, 'Registration failed')
  return (await response.json()).accessToken
}
const token = await customer()
let cart
for (let attempt = 0; attempt < 60; attempt++) {
  try {
    const response = await request('/cart', token)
    if (response.ok) { cart = await response.json(); break }
  } catch { /* Bounded startup retry. */ }
  await delay(2000)
}
assert.ok(cart, 'Order service did not become ready')
assert.equal((await request('/cart')).status, 401)
assert.equal(cart.items.length, 0)
const product = await (await request('/products/bota-serra')).json()
const item = { productId: product.id, sku: product.variants[0].sku, quantity: 2 }
const body = { ...cart, items: [item], coupon: 'DEMO10' }
const key = randomUUID()
let response = await request('/cart', token, 'PUT', body, key)
assert.equal(response.status, 200)
cart = await response.json()
response = await request('/cart', token, 'PUT', body, key)
assert.equal(response.status, 200)
assert.equal((await response.json()).version, cart.version)
assert.equal((await request('/cart', token, 'PUT', { ...body, coupon: null }, key)).status, 409)
assert.equal((await request('/cart', token, 'PUT', body, randomUUID())).status, 409)
response = await request('/cart/quote', token, 'POST')
assert.equal(response.status, 200)
const quote = await response.json()
assert.equal(quote.cartVersion, cart.version)
assert.equal(quote.subtotal, '799.80')
assert.equal(quote.discount, '79.98')
assert.equal(quote.total, '719.82')
const other = await customer()
assert.equal((await (await request('/cart', other)).json()).items.length, 0)
const favorites = `/customers/me/favorites/${product.id}`
assert.equal((await request(favorites, token, 'PUT')).status, 200)
assert.equal((await request(favorites, token, 'PUT')).status, 200)
assert.equal((await (await request('/customers/me/favorites', token)).json()).totalElements, 1)
assert.equal((await (await request('/customers/me/favorites', other)).json()).totalElements, 0)
assert.equal((await request('/admin/coupons/DEMO10', token)).status, 403)
response = await request('/cart', token, 'PUT', { ...cart, coupon: 'INVALID' }, randomUUID())
assert.equal(response.status, 200)
cart = await response.json()
assert.equal((await request('/cart/quote', token, 'POST')).status, 422)
response = await request('/cart', token, 'PUT', { ...cart, items: [], coupon: null }, randomUUID())
assert.equal(response.status, 200)
assert.equal((await response.json()).items.length, 0)
assert.equal((await request('/cart/quote', token, 'POST')).status, 422)
assert.equal((await request(favorites, token, 'DELETE')).status, 200)
assert.equal((await (await request('/customers/me/favorites', token)).json()).totalElements, 0)
console.log('Cart smoke passed: persistence, idempotency, ownership, authoritative quote, coupons and favorites')
