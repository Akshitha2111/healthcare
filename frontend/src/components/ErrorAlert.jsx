/** Accepts a normalised API error (or a string) and renders an aria-live alert. */
export default function ErrorAlert({ error, message, onRetry, className = '' }) {
  const text = message || (error && error.message) || (typeof error === 'string' ? error : null);
  if (!text) return <div className={className} role="alert" aria-live="assertive" />;
  return (
    <div className={`alert-box alert-error ${className}`} role="alert" aria-live="assertive">
      <span>{text}</span>
      {onRetry && (
        <button type="button" className="btn btn-sm btn-outline-danger ms-3" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
