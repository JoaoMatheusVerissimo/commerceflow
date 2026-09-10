import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { setTimeout as delay } from 'node:timers/promises'

const base = process.env.SMOKE_BASE_URL ?? 'http://localhost:3000'
const jar = new Map()
async function request(path, { token, body, method = 'GET', cookies = jar } = {}) {
  const response = await fetch(`${base}/api/v1${path}`, {
    method, signal: AbortSignal.timeout(10000),
    headers: { 'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; '),
      'X-CSRF-Token': cookies.get('commerceflow_csrf') ?? '',
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  for (const cookie of response.headers.getSetCookie()) {
    const [key, value] = cookie.split(';')[0].split('=')
    jar.set(key, value)
  }
  return response
}

let ready = false
for (let attempt = 0; attempt < 90; attempt++) {
  try {
    const response = await request('/auth/.well-known/jwks.json')
    if (response.ok) { ready = true; break }
  } catch { /* Services may still be starting. The bounded deadline fails below. */ }
  await delay(2000)
}
assert.ok(ready, 'Stack did not become ready within the deadline')
assert.equal((await fetch(base)).status, 200)
assert.equal((await request('/customers/me')).status, 401)
const credentials = { name: 'Foundation Smoke', email: `smoke-${randomUUID()}@example.test`, password: `Aa1-${randomUUID()}` }
let response = await request('/auth/register', { method: 'POST', body: credentials })
assert.equal(response.status, 200, 'Registration failed')
const registered = await response.json()
assert.ok(registered.accessToken)
const cookies = response.headers.getSetCookie()
assert.ok(cookies.some(value => value.includes('commerceflow_refresh=') && value.includes('HttpOnly')))
assert.ok(cookies.some(value => value.includes('commerceflow_csrf=') && value.includes('Path=/;')))
response = await request('/customers/me', { token: registered.accessToken })
assert.equal(response.status, 200)
assert.equal((await response.json()).email, credentials.email)
response = await request('/auth/me', { token: registered.accessToken })
assert.equal(response.status, 200)
assert.deepEqual((await response.json()).roles, ['CUSTOMER'])
const original = new Map(jar)
assert.equal((await request('/auth/refresh', { method: 'POST' })).status, 200)
const successor = new Map(jar)
assert.equal((await request('/auth/refresh', { method: 'POST', cookies: original })).status, 401)
assert.equal((await request('/auth/refresh', { method: 'POST', cookies: successor })).status, 401)
assert.equal((await request('/auth/login', { method: 'POST', body: { ...credentials, password: 'wrong' } })).status, 401)
assert.equal((await request('/auth/login', { method: 'POST', body: credentials })).status, 200)
const beforeLogout = new Map(jar)
assert.equal((await request('/auth/logout', { method: 'POST' })).status, 204)
assert.equal((await request('/auth/refresh', { method: 'POST', cookies: beforeLogout })).status, 401)
console.log('Foundation smoke passed: frontend → gateway → auth → customer → PostgreSQL')
