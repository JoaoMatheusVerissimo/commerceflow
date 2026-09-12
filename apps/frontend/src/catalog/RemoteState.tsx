export function RemoteState({ error, retry }: { error?: Error; retry: () => void }) {
  return error ? <div className="notice" role="alert"><p>{error.message}</p><button onClick={retry}>Tentar novamente</button></div>
    : <div className="catalog-skeleton" role="status" aria-label="Carregando catálogo"><span /><span /><span /><p>Carregando…</p></div>
}
