import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError, catalogRequest } from '../catalog/api'
import { addItem, saveCart, type Command } from './api'

export function ProductActions({ productId, sku }: { productId: string; sku: string }) {
  const { accessToken } = useAuth()
  const [busy, setBusy] = useState(false)
  const [pending, setPending] = useState<Command>()
  const [feedback, setFeedback] = useState('')
  const [error, setError] = useState('')
  if (!accessToken) return <p><Link to="/login">Entre na sua conta</Link> para adicionar ao carrinho ou aos favoritos.</p>
  async function add() {
    setBusy(true); setError(''); setFeedback('')
    try {
      const request = pending ?? await addItem(accessToken!, { productId, sku, quantity: 1 })
      setPending(request)
      await saveCart(accessToken!, request)
      setPending(undefined); setFeedback('Produto adicionado ao carrinho.')
    } catch (reason) {
      if (reason instanceof ApiError && reason.status < 500) setPending(undefined)
      setError(reason instanceof Error ? reason.message : 'Falha ao adicionar.')
    } finally { setBusy(false) }
  }
  async function favorite() {
    setBusy(true); setError(''); setFeedback('')
    try {
      await catalogRequest(`/customers/me/favorites/${productId}`, accessToken, { method: 'PUT' })
      setFeedback('Produto salvo nos favoritos.')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha ao favoritar.') }
    finally { setBusy(false) }
  }
  return <div className="purchase-actions"><button className="button primary" disabled={busy} onClick={add}>{busy ? 'Aguarde…' : pending ? 'Repetir inclusão pendente' : 'Adicionar ao carrinho'}</button>
    <button className="button" disabled={busy || !!pending} onClick={favorite}>Salvar nos favoritos</button>
    {error && <p role="alert">{error}</p>}{feedback && <p role="status">{feedback} <Link to="/cart">Ver carrinho</Link> · <Link to="/favorites">Ver favoritos</Link></p>}
  </div>
}
