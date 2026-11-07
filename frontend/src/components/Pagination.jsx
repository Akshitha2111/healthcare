/** `page` is 0-based as in the API. */
export default function Pagination({ page, totalPages, onPageChange, totalElements }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <nav className="pager" aria-label="Pagination">
      <button type="button" className="btn btn-outline-secondary btn-sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>
      <span className="pager-info" aria-live="polite">
        Page {page + 1} of {totalPages}
        {typeof totalElements === 'number' ? ` (${totalElements} total)` : ''}
      </span>
      <button type="button" className="btn btn-outline-secondary btn-sm" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>
        Next
      </button>
    </nav>
  );
}
