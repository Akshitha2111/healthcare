export default function EmptyState({ title, children, action }) {
  return (
    <div className="empty-state">
      <h3 className="empty-title">{title}</h3>
      {children && <p className="empty-text">{children}</p>}
      {action}
    </div>
  );
}
