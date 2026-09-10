import { afterEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './api'

afterEach(() => { vi.unstubAllGlobals(); document.cookie = 'commerceflow_csrf=; Max-Age=0; Path=/' })

describe('session API', () => {
  it('shares concurrent refresh requests and sends the CSRF header', async () => {
    document.cookie = 'commerceflow_csrf=csrf-value; Path=/'
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ accessToken: 'access', expiresIn: 900 })))
    vi.stubGlobal('fetch', fetchMock)
    const first = authApi.refresh()
    const second = authApi.refresh()
    expect(first).toBe(second)
    expect((await first).accessToken).toBe('access')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/refresh', expect.objectContaining({
      credentials: 'include', headers: expect.objectContaining({ 'X-CSRF-Token': 'csrf-value' }),
    }))
  })

  it('reports a failed logout instead of pretending it succeeded', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 503 })))
    await expect(authApi.logout()).rejects.toThrow('Não foi possível encerrar')
  })

  it('reports login rejection', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"message":"Invalid credentials"}', { status: 401 })))
    await expect(authApi.login({ email: 'a@example.test', password: 'incorrect' })).rejects.toThrow('Invalid credentials')
  })
})
