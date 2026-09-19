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
async function customer(label) {
  const response = await request('/auth/register', null, 'POST', {
    name: `${label} Smoke`, email: `${label}-${randomUUID()}@example.test`, password: `Aa1-${randomUUID()}`,
  })
  assert.equal(response.status, 200, 'Registration failed')
  return (await response.json()).accessToken
}

const token = await customer('order')
let cart
for (let attempt = 0; attempt < 60; attempt++) {
  try {
    const response = await request('/cart', token)
    if (response.ok) { cart = await response.json(); break }
  } catch { /* Bounded startup retry. */ }
  await delay(2000)
}
assert.ok(cart, 'Order service did not become ready')

const productResponse = await request('/products/bota-serra')
assert.equal(productResponse.status, 200)
const product = await productResponse.json()
const variant = product.variants[0]
const beforeResponse = await request(`/availability/${variant.sku}`)
assert.equal(beforeResponse.status, 200)
const before = await beforeResponse.json()
assert.ok(before.available > 0, 'Seeded SKU must have stock')

let response = await request('/cart', token, 'PUT', {
  ...cart, items: [{ productId: product.id, sku: variant.sku, quantity: 1 }], coupon: null,
}, randomUUID())
assert.equal(response.status, 200)
cart = await response.json()

const key = randomUUID()
response = await request('/checkout', token, 'POST', { cartVersion: cart.version }, key)
assert.equal(response.status, 202)
const order = await response.json()
assert.equal(order.status, 'PAYMENT_PENDING')
assert.equal(order.items.length, 1)
assert.equal(order.items[0].sku, variant.sku)
assert.ok(order.reservationId)
assert.equal(order.shipping, '0.00')

response = await request('/checkout', token, 'POST', { cartVersion: cart.version }, key)
assert.equal(response.status, 202)
assert.equal((await response.json()).id, order.id, 'Checkout replay must return the original order')

const history = await (await request('/orders', token)).json()
assert.equal(history.totalElements, 1)
assert.equal(history.items[0].id, order.id)
assert.equal((await (await request(`/orders/${order.id}`, token)).json()).id, order.id)
assert.equal((await (await request('/cart', token)).json()).items.length, 0)

const after = await (await request(`/availability/${variant.sku}`)).json()
assert.equal(after.available, before.available - 1)
assert.equal(after.reserved, before.reserved + 1)

const other = await customer('order-other')
assert.equal((await request(`/orders/${order.id}`, other)).status, 404)
assert.equal((await (await request('/orders', other)).json()).totalElements, 0)
assert.equal((await request('/admin/orders', token)).status, 403)

console.log('Order smoke passed: immutable snapshot, reservation, idempotency, ownership and cart clearing')
