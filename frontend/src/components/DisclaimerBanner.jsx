import { DISCLAIMER_TEXT } from '../utils/constants';

export default function DisclaimerBanner({ extra, compact = false }) {
  return (
    <aside className={`disclaimer ${compact ? 'disclaimer-compact' : ''}`} aria-label="Medical disclaimer">
      <strong>Medical disclaimer. </strong>
      <span>{DISCLAIMER_TEXT}</span>
      {extra && <span className="d-block mt-1">{extra}</span>}
    </aside>
  );
}
