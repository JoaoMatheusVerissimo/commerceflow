import { render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { AccountProfile } from './AccountProfile'

afterEach(() => vi.unstubAllGlobals())

it('loads the actual profile with the bearer token', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('{"name":"Ana Demo","email":"ana@example.test"}'))
  vi.stubGlobal('fetch', fetchMock)
  render(<AccountProfile accessToken="access" />)
  expect(screen.getByRole('status')).toHaveTextContent('Carregando')
  expect(await screen.findByText('Ana Demo')).toBeInTheDocument()
  expect(fetchMock).toHaveBeenCalledWith('/api/v1/customers/me', expect.objectContaining({
    headers: { Authorization: 'Bearer access' },
  }))
})

it('shows permission denial without displaying fictitious profile data', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 403 })))
  render(<AccountProfile accessToken="access" />)
  expect(await screen.findByRole('alert')).toHaveTextContent('Você não tem permissão')
  expect(screen.queryByText('Sua conta está ativa')).not.toBeInTheDocument()
})
