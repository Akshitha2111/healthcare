import { useCallback, useEffect, useRef, useState } from 'react'
import { errorMessage } from '../utils/format'

/** Runs an async loader on mount (and when deps change); exposes loading / error / data / reload. */
export default function useAsync(loader, deps = []) {
  const [state, setState] = useState({ loading: true, error: '', data: null })
  const alive = useRef(true)
  const run = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: '' }))
    return loader()
      .then((data) => alive.current && setState({ loading: false, error: '', data }))
      .catch((e) => alive.current && setState({ loading: false, error: errorMessage(e), data: null }))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
  useEffect(() => {
    alive.current = true
    run()
    return () => { alive.current = false }
  }, [run])
  return { ...state, reload: run }
}
