export type Category = { id: string; name: string; slug: string; version: number }
export type Variant = { sku: string; color: string; size: string; price: string; promotionalPrice: string | null; effectivePrice: string }
export type ProductImage = { url: string; alt: string }
export type Product = { id: string; category: Category; name: string; slug: string; description: string; status: string; minPrice: string; currency: string; version: number; variants: Variant[]; images: ProductImage[] }
export type Review = { id: string; productId: string; rating: number; comment: string; status: string; createdAt: string; version: number }
export type Page<T> = { items: T[]; page: number; size: number; totalElements: number; totalPages: number }

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

export async function catalogRequest<T>(path: string, token?: string | null, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`/api/v1${path}`, { ...init, headers: {
    'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...init.headers,
  } })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const messages: Record<number, string> = {
      400: 'Confira os campos e filtros informados.', 401: 'Sua sessão expirou. Entre novamente.',
      403: 'Você não tem permissão para esta operação.', 404: 'Não encontramos este produto ou categoria.',
      409: 'Este dado já existe ou foi alterado. Recarregue antes de tentar novamente.',
    }
    throw new ApiError(response.status, messages[response.status] ?? body.message ?? 'Não foi possível concluir a operação.')
  }
  return response.json() as Promise<T>
}

export const money = (value: string) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value))
