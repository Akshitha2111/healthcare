import { useMemo, useState } from 'react'

/** Searchable multi-select built from accessible toggle buttons (keyboard friendly, aria-pressed). */
export default function SymptomSelector({ symptoms, selected, onToggle, max = 20 }) {
  const [query, setQuery] = useState('')
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return q ? symptoms.filter((s) => s.label.toLowerCase().includes(q)) : symptoms
  }, [symptoms, query])
  const atLimit = selected.length >= max

  return (
    <div>
      <label htmlFor="symptom-search" className="form-label fw-semibold">Search symptoms</label>
      <input id="symptom-search" type="search" className="form-control mb-3" placeholder="e.g. fever, cough, headache" value={query} onChange={(e) => setQuery(e.target.value)} autoComplete="off" />
      <div className="symptom-grid" role="group" aria-label="Available symptoms">
        {filtered.map((s) => {
          const on = selected.includes(s.name)
          return (
            <button key={s.name} type="button" className={`chip ${on ? 'chip-on' : ''}`} aria-pressed={on} disabled={!on && atLimit} onClick={() => onToggle(s.name)}>
              {on ? '✓ ' : ''}{s.label}
            </button>
          )
        })}
        {filtered.length === 0 && <div className="text-muted">No symptoms match “{query}”.</div>}
      </div>
    </div>
  )
}
