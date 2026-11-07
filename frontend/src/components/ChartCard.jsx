/**
 * Accessible chart wrapper: titled figure, text summary via aria-label on the graphic,
 * and a collapsible data table as the non-visual fallback.
 */
export default function ChartCard({ title, description, columns, rows, children, emptyMessage = 'No data available for this period.' }) {
  const hasData = rows && rows.length > 0;
  const summary = hasData ? `${title}. ${rows.length} data points. Full data is available in the table below.` : `${title}. ${emptyMessage}`;
  return (
    <figure className="chart-card">
      <figcaption className="chart-title">{title}</figcaption>
      {description && <p className="chart-desc">{description}</p>}
      {hasData ? (
        <>
          <div className="chart-graphic" role="img" aria-label={summary}>{children}</div>
          <details className="chart-details">
            <summary>View data as table</summary>
            <div className="table-responsive">
              <table className="table table-sm data-table">
                <caption className="visually-hidden">{title}</caption>
                <thead>
                  <tr>{columns.map((c) => <th key={c.key} scope="col">{c.label}</th>)}</tr>
                </thead>
                <tbody>
                  {rows.map((r, i) => (
                    <tr key={i}>{columns.map((c) => <td key={c.key}>{r[c.key]}</td>)}</tr>
                  ))}
                </tbody>
              </table>
            </div>
          </details>
        </>
      ) : (
        <p className="chart-empty">{emptyMessage}</p>
      )}
    </figure>
  );
}
