export function LoadingBlock({ label = 'Loading…' }) {
  return (
    <div className="text-center py-5" role="status" aria-live="polite">
      <div className="spinner-border text-primary" aria-hidden="true" />
      <div className="mt-2 text-muted">{label}</div>
    </div>
  )
}

export function ErrorAlert({ message, onRetry }) {
  if (!message) return null
  return (
    <div className="alert alert-danger d-flex justify-content-between align-items-center" role="alert">
      <span>{message}</span>
      {onRetry && <button type="button" className="btn btn-sm btn-outline-danger ms-3" onClick={onRetry}>Retry</button>}
    </div>
  )
}

export function SuccessAlert({ message }) {
  if (!message) return null
  return <div className="alert alert-success" role="status">{message}</div>
}

export function EmptyState({ title, children }) {
  return (
    <div className="text-center text-muted py-5 border rounded-3 bg-white">
      <div className="fs-5 mb-1">{title}</div>
      <div>{children}</div>
    </div>
  )
}

export function StatCard({ label, value, hint }) {
  return (
    <div className="card stat-card h-100">
      <div className="card-body">
        <div className="text-muted small text-uppercase fw-semibold">{label}</div>
        <div className="display-6 fw-bold">{value ?? '—'}</div>
        {hint && <div className="small text-muted">{hint}</div>}
      </div>
    </div>
  )
}

export function Disclaimer({ children }) {
  return (
    <div className="disclaimer" role="note">
      <strong>Medical disclaimer.</strong>{' '}
      {children || 'This tool gives educational, preliminary information produced by a statistical model. It is not a medical diagnosis and does not replace a qualified healthcare professional. Never ignore professional advice because of something you read here.'}
    </div>
  )
}

export function UrgentNotice({ children }) {
  return (
    <div className="alert alert-warning" role="alert">
      <strong>When to seek urgent care.</strong>{' '}
      {children || 'Seek urgent medical care (or call your local emergency number) for severe or rapidly worsening symptoms, chest pain, difficulty breathing, confusion, fainting, sudden weakness or severe bleeding.'}
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  return (
    <nav aria-label="Pagination" className="d-flex justify-content-between align-items-center mt-3">
      <button type="button" className="btn btn-outline-secondary btn-sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span className="small text-muted">Page {page + 1} of {totalPages}</span>
      <button type="button" className="btn btn-outline-secondary btn-sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next</button>
    </nav>
  )
}

export function FieldError({ id, message }) {
  return message ? <div id={id} className="invalid-feedback d-block">{message}</div> : null
}

export function Button({ loading, children, ...props }) {
  return (
    <button {...props} disabled={props.disabled || loading}>
      {loading && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
      {children}
    </button>
  )
}
