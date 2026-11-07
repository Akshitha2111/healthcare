/** Label + control + error text wiring (aria-describedby / aria-invalid). */
export default function FormField({ id, label, error, hint, children, required = false }) {
  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label">
        {label}
        {required && <span aria-hidden="true" className="req"> *</span>}
      </label>
      {children}
      {hint && !error && <div id={`${id}-hint`} className="form-text">{hint}</div>}
      {error && <div id={`${id}-error`} className="field-error" role="alert">{error}</div>}
    </div>
  );
}

export function describedBy(id, error, hint) {
  if (error) return `${id}-error`;
  if (hint) return `${id}-hint`;
  return undefined;
}
