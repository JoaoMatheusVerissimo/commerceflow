import { useEffect, useState } from 'react'
import { catalogRequest } from './api'

export function useRemote<T>(path: string, token?: string | null) {
  const [attempt, setAttempt] = useState(0)
  const key = `${path}:${token ?? ''}:${attempt}`
  const [state, setState] = useState<{ key: string; data?: T; error?: Error }>({ key: '' })
  useEffect(() => {
    const controller = new AbortController()
    catalogRequest<T>(path, token, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setState({ key, data }) })
      .catch((error: Error) => { if (!controller.signal.aborted) setState({ key, error }) })
    return () => controller.abort()
  }, [path, token, key])
  return { data: state.key === key ? state.data : undefined, error: state.key === key ? state.error : undefined,
    loading: state.key !== key, reload: () => setAttempt(value => value + 1) }
}
