/** Visual toast stack. Lives in an aria-live region so messages are announced. */
export default function ToastRegion({ toasts, onDismiss }) {
  return (
    <div className="toast-region" role="status" aria-live="polite" aria-atomic="false">
      {toasts.map((t) => (
        <div key={t.id} className={`toast-item toast-${t.variant}`}>
          <span>{t.message}</span>
          <button type="button" className="toast-close" aria-label="Dismiss notification" onClick={() => onDismiss(t.id)}>
            &times;
          </button>
        </div>
      ))}
    </div>
  );
}
