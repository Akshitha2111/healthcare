import { STATUS_LABELS } from '../utils/constants';

export default function StatusBadge({ status }) {
  const key = String(status || '').toLowerCase().replace(/_/g, '-');
  return <span className={`status-badge status-${key}`}>{STATUS_LABELS[status] || status}</span>;
}
