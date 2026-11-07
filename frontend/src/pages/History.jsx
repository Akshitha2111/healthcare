import { useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState, ErrorAlert, LoadingBlock, Pagination } from '../components/ui'
import useAsync from '../hooks/useAsync'
import { predictionApi } from '../services/endpoints'
import { formatDateTime, percent } from '../utils/format'

export default function History() {
  const [filters, setFilters] = useState({ q: '', from: '', to: '', sort: 'desc' })
  const [applied, setApplied] = useState(filters)
  const [page, setPage] = useState(0)
  const { loading, error, data, reload } = useAsync(
    () => predictionApi.mine({ page, size: 10, q: applied.q || undefined, from: applied.from || undefined, to: applied.to || undefined, sort: applied.sort }),
    [page, applied],
  )
  const change = (e) => setFilters({ ...filters, [e.target.name]: e.target.value })
  const apply = (e) => { e.preventDefault(); setPage(0); setApplied(filters) }
  const reset = () => { const f = { q: '', from: '', to: '', sort: 'desc' }; setFilters(f); setApplied(f); setPage(0) }

  return (
    <div>
      <h1 className="h3">Prediction history</h1>
      <form className="card card-body mb-3" onSubmit={apply}>
        <div className="row g-2 align-items-end">
          <div className="col-12 col-md-4"><label htmlFor="q" className="form-label small mb-1">Search result</label><input id="q" name="q" className="form-control" value={filters.q} onChange={change} placeholder="e.g. allergy" /></div>
          <div className="col-6 col-md-2"><label htmlFor="from" className="form-label small mb-1">From</label><input id="from" name="from" type="date" className="form-control" value={filters.from} onChange={change} /></div>
          <div className="col-6 col-md-2"><label htmlFor="to" className="form-label small mb-1">To</label><input id="to" name="to" type="date" className="form-control" value={filters.to} onChange={change} /></div>
          <div className="col-6 col-md-2"><label htmlFor="sort" className="form-label small mb-1">Order</label>
            <select id="sort" name="sort" className="form-select" value={filters.sort} onChange={change}><option value="desc">Newest first</option><option value="asc">Oldest first</option></select></div>
          <div className="col-6 col-md-2 d-flex gap-2"><button className="btn btn-primary flex-grow-1" type="submit">Filter</button><button className="btn btn-outline-secondary" type="button" onClick={reset}>Reset</button></div>
        </div>
      </form>

      {loading && <LoadingBlock />}
      <ErrorAlert message={error} onRetry={reload} />
      {!loading && !error && data && (data.content.length === 0 ? (
        <EmptyState title="No predictions found"><Link to="/symptom-checker">Run a symptom check</Link></EmptyState>
      ) : (
        <>
          <div className="table-responsive card"><table className="table table-hover mb-0">
            <thead><tr><th scope="col">Date</th><th scope="col">Top result</th><th scope="col">Symptoms</th><th scope="col">Score</th><th scope="col"><span className="visually-hidden">Details</span></th></tr></thead>
            <tbody>
              {data.content.map((p) => (
                <tr key={p.id}>
                  <td>{formatDateTime(p.createdAt)}</td>
                  <td className="fw-semibold">{p.predictedClass}</td>
                  <td className="small text-muted">{p.symptoms.map((s) => s.replace(/_/g, ' ')).join(', ')}</td>
                  <td>{p.topScore != null ? percent(p.topScore) : '—'}</td>
                  <td><Link to={`/predictions/${p.id}`} className="btn btn-sm btn-outline-primary">Details</Link></td>
                </tr>
              ))}
            </tbody>
          </table></div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      ))}
    </div>
  )
}
