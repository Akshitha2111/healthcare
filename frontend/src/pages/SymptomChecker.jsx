import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Disclaimer, ErrorAlert, LoadingBlock, UrgentNotice } from '../components/ui'
import SymptomSelector from '../components/SymptomSelector'
import { predictionApi } from '../services/endpoints'
import { errorMessage } from '../utils/format'
import { validateSymptomSelection } from '../utils/validation'

export default function SymptomChecker() {
  const navigate = useNavigate()
  const [catalog, setCatalog] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [selected, setSelected] = useState([])
  const [submitError, setSubmitError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoadError('')
    predictionApi.symptoms().then(setCatalog).catch((e) => setLoadError(errorMessage(e)))
  }
  useEffect(load, [])

  if (loadError) return <ErrorAlert message={loadError} onRetry={load} />
  if (!catalog) return <LoadingBlock label="Loading symptoms…" />

  const labelOf = Object.fromEntries(catalog.symptoms.map((s) => [s.name, s.label]))
  const toggle = (name) => setSelected((cur) => (cur.includes(name) ? cur.filter((n) => n !== name) : [...cur, name]))
  const validation = validateSymptomSelection(selected, catalog.minSymptoms, catalog.maxSymptoms)

  const submit = async (e) => {
    e.preventDefault()
    setSubmitError('')
    if (validation) return setSubmitError(validation)
    setBusy(true)
    try {
      const result = await predictionApi.create(selected)
      navigate(`/predictions/${result.id}`)
    } catch (err) {
      setSubmitError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <div>
      <h1 className="h3">Symptom checker</h1>
      <p className="text-muted">Select the symptoms you are experiencing ({catalog.minSymptoms}–{catalog.maxSymptoms}). The model only knows the {catalog.count} symptoms listed.</p>
      <UrgentNotice />
      <form onSubmit={submit}>
        <div className="row g-4">
          <div className="col-12 col-lg-8"><div className="card"><div className="card-body">
            <SymptomSelector symptoms={catalog.symptoms} selected={selected} onToggle={toggle} max={catalog.maxSymptoms} />
          </div></div></div>
          <div className="col-12 col-lg-4"><div className="card"><div className="card-body">
            <h2 className="h6">Selected symptoms ({selected.length})</h2>
            {selected.length === 0 ? <p className="text-muted small">Nothing selected yet.</p> : (
              <ul className="list-unstyled d-flex flex-wrap gap-2" aria-label="Selected symptoms">
                {selected.map((n) => (
                  <li key={n}>
                    <button type="button" className="chip chip-on" onClick={() => toggle(n)} aria-label={`Remove ${labelOf[n]}`}>{labelOf[n]} ✕</button>
                  </li>
                ))}
              </ul>
            )}
            {selected.length > 0 && validation && <div className="text-warning-emphasis small mb-2">{validation}</div>}
            <ErrorAlert message={submitError} />
            <Button type="submit" className="btn btn-primary w-100" loading={busy} disabled={!!validation}>
              {busy ? 'Analysing…' : 'Get prediction'}
            </Button>
            {selected.length > 0 && <button type="button" className="btn btn-link btn-sm w-100 mt-2" onClick={() => setSelected([])}>Clear all</button>}
          </div></div></div>
        </div>
      </form>
      <div className="mt-4"><Disclaimer /></div>
    </div>
  )
}
