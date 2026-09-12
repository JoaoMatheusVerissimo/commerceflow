import { Link } from 'react-router-dom'
import { money, type Product } from './api'

export function ProductCard({ product }: { product: Product }) {
  return <article className="product-card"><Link to={`/products/${product.slug}`}>
    <img src={product.images[0].url} alt={product.images[0].alt} loading="lazy" width="520" height="480" />
    <p className="eyebrow">{product.category.name}</p><h2>{product.name}</h2>
    <p className="product-price">A partir de {money(product.minPrice)}</p>
    <span className="muted">{product.variants.length} opções · Ver detalhes →</span></Link></article>
}
