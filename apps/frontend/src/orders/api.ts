import { catalogRequest } from '../catalog/api'

export type OrderItem = { productId: string; sku: string; name: string; slug: string; quantity: number; unitPrice: string; total: string }
export type Order = { id: string; customerId: string; status: string; items: OrderItem[]; subtotal: string; discount: string; shipping: string; total: string; currency: string; coupon: string | null; reservationId: string | null; failureCode: string | null; createdAt: string; updatedAt: string; version: number }
export type OrderPage = { items: Order[]; page: number; size: number; totalElements: number; totalPages: number }
export type CheckoutCommand = { key: string; cartVersion: number }
export const checkoutCommand = (cartVersion: number): CheckoutCommand => ({ key: crypto.randomUUID(), cartVersion })
export const createOrder = (token: string, command: CheckoutCommand) => catalogRequest<Order>('/checkout', token, {
  method: 'POST', headers: { 'Idempotency-Key': command.key }, body: JSON.stringify({ cartVersion: command.cartVersion }),
})

export const orderStatus: Record<string, string> = {
  CREATED: 'Aguardando reserva', PAYMENT_PENDING: 'Estoque reservado · aguardando pagamento',
  PAID: 'Pago', PROCESSING: 'Em preparação', SHIPPED: 'Enviado', DELIVERED: 'Entregue',
  CANCELLED: 'Cancelado', REFUNDED: 'Reembolsado',
}
