import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, json: vi.fn() }))
  })

  it('redirects an unauthenticated visitor after session bootstrap', async () => {
    render(<MemoryRouter initialEntries={['/app']}><AuthProvider><Routes>
      <Route path="/app" element={<ProtectedRoute><p>Área privada</p></ProtectedRoute>} />
      <Route path="/login" element={<p>Faça login</p>} />
    </Routes></AuthProvider></MemoryRouter>)
    expect(await screen.findByText('Faça login')).toBeInTheDocument()
    expect(screen.queryByText('Área privada')).not.toBeInTheDocument()
  })
})
