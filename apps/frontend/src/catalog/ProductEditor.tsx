import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { catalogRequest, type Category, type Product, type ProductImage } from './api'

type EditableVariant = { sku: string; color: string; size: string; price: string; promotionalPrice: string | null }
const emptyVariant = (): EditableVariant => ({ sku: '', color: '', size: '', price: '', promotionalPrice: null })

export function ProductEditor({ product, categories, token }: { product?: Product; categories: Category[]; token: string }) {
  const navigate = useNavigate()
  const [variants, setVariants] = useState<EditableVariant[]>(product?.variants ?? [emptyVariant()])
  const [images, setImages] = useState<ProductImage[]>(product?.images ?? [{ url: '', alt: '' }])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  function variantField(index: number, field: keyof EditableVariant, value: string) {
    setVariants(rows => rows.map((row, i) => i === index ? { ...row, [field]: field === 'promotionalPrice' && value === '' ? null : value } : row))
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    const form = new FormData(event.currentTarget)
    const body = { name: form.get('name'), slug: form.get('slug'), categoryId: form.get('categoryId'), description: form.get('description'), status: form.get('status'),
      version: product?.version, images, variants: variants.map(({ sku, color, size, price, promotionalPrice }) => ({ sku, color, size, price, promotionalPrice })) }
    try {
      await catalogRequest(product ? `/admin/products/${product.id}` : '/admin/products', token, { method: product ? 'PUT' : 'POST', body: JSON.stringify(body) })
      navigate('/admin/products', { state: { saved: true } })
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível salvar.') }
    finally { setBusy(false) }
  }
  return <form className="editor" onSubmit={submit}><h1>{product ? 'Editar produto' : 'Novo produto'}</h1>
    <div className="editor-grid"><label>Nome<input name="name" required maxLength={160} defaultValue={product?.name} /></label><label>Slug<input name="slug" required pattern="[a-z0-9]+(-[a-z0-9]+)*" maxLength={180} defaultValue={product?.slug} /></label>
      <label>Categoria<select name="categoryId" required defaultValue={product?.category.id ?? categories[0]?.id}>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
      <label>Status<select name="status" defaultValue={product?.status ?? 'DRAFT'}><option value="DRAFT">Rascunho</option><option value="ACTIVE">Ativo</option><option value="ARCHIVED">Arquivado</option></select></label></div>
    <label>Descrição<textarea name="description" rows={4} required maxLength={5000} defaultValue={product?.description} /></label>
    <fieldset><legend>Variações e preços em BRL</legend>{variants.map((v, index) => <div className="editor-row" key={index}>{(['sku', 'color', 'size', 'price', 'promotionalPrice'] as const).map((field, f) => <label key={field}>{['SKU', 'Cor', 'Tamanho', 'Preço', 'Promocional'][f]}<input aria-label={`${field} ${index + 1}`} required={field !== 'promotionalPrice'} type={f >= 3 ? 'number' : 'text'} min={f >= 3 ? '0.01' : undefined} step={f >= 3 ? '0.01' : undefined} pattern={field === 'sku' ? '[A-Z0-9]+(-[A-Z0-9]+)*' : undefined} value={v[field] ?? ''} onChange={e => variantField(index, field, e.target.value)} /></label>)}<button type="button" disabled={variants.length === 1} onClick={() => setVariants(rows => rows.filter((_, i) => i !== index))}>Remover variação {index + 1}</button></div>)}<button type="button" disabled={variants.length >= 50} onClick={() => setVariants([...variants, emptyVariant()])}>+ Adicionar variação</button></fieldset>
    <fieldset><legend>Imagens (a primeira é a capa)</legend><p className="muted">URL HTTPS ou ilustração local: /catalog-images/boot.svg, hat.svg ou belt.svg.</p>{images.map((img, index) => <div className="editor-row image-row" key={index}><label>URL<input required value={img.url} maxLength={1000} onChange={e => setImages(rows => rows.map((row, i) => i === index ? { ...row, url: e.target.value } : row))} /></label><label>Descrição acessível<input required maxLength={160} value={img.alt} onChange={e => setImages(rows => rows.map((row, i) => i === index ? { ...row, alt: e.target.value } : row))} /></label><button type="button" disabled={images.length === 1} onClick={() => setImages(rows => rows.filter((_, i) => i !== index))}>Remover imagem {index + 1}</button></div>)}<button type="button" disabled={images.length >= 12} onClick={() => setImages([...images, { url: '', alt: '' }])}>+ Adicionar imagem</button></fieldset>
    {error && <p className="notice" role="alert">{error}</p>}<div className="actions"><button className="button primary" disabled={busy || categories.length === 0}>{busy ? 'Salvando…' : 'Salvar produto'}</button><Link to="/admin/products">Cancelar</Link></div>
  </form>
}
