export default function StatCard({ label, value, hint }) {
  return (
    <div className="stat-card">
      <div className="stat-value">{value ?? '-'}</div>
      <div className="stat-label">{label}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}
