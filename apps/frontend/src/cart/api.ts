import { catalogRequest } from '../catalog/api'

export type CartItem = { productId: string; sku: string; quantity: number }
export type Cart = { version: number; items: CartItem[]; coupon: string | null }
export type Quote = { cartVersion: number; items: (CartItem & { name: string; slug: string; unitPrice: string; total: string })[]; subtotal: string; discount: string; total: string; currency: string; quotedAt: string; expiresAt: string }
export type Command = { key: string; cart: Cart }
export const command = (cart: Cart): Command => ({ key: crypto.randomUUID(), cart })
export const saveCart = (token: string, pending: Command) => catalogRequest<Cart>('/cart', token, {
  method: 'PUT', headers: { 'Idempotency-Key': pending.key }, body: JSON.stringify(pending.cart),
})
export async function addItem(token: string, item: CartItem) {
  const cart = await catalogRequest<Cart>('/cart', token)
  const existing = cart.items.find(i => i.sku === item.sku)
  const quantity = (existing?.quantity ?? 0) + item.quantity
  if (quantity > 99) throw new Error('O limite é 99 unidades por variação.')
  const items = existing ? cart.items.map(i => i.sku === item.sku ? { ...i, quantity } : i) : [...cart.items, item]
  if (items.length > 50) throw new Error('O limite é 50 variações por carrinho.')
  return command({ ...cart, items })
}
