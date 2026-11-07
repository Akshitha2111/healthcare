import { formatScore } from '../utils/format';

/** Horizontal bar for a raw, uncalibrated model score in 0..1. */
export default function ScoreBar({ label, score, rank, highlight = false }) {
  const pct = Math.max(0, Math.min(1, Number(score) || 0)) * 100;
  return (
    <div className={`score-row ${highlight ? 'score-top' : ''}`}>
      <div className="score-head">
        <span className="score-name">{rank ? `${rank}. ` : ''}{label}</span>
        <span className="score-value">model score {formatScore(score)}</span>
      </div>
      <div
        className="score-track"
        role="meter"
        aria-label={`Model score for ${label}`}
        aria-valuemin={0}
        aria-valuemax={1}
        aria-valuenow={Number(score) || 0}
        aria-valuetext={`model score ${formatScore(score)}`}
      >
        <div className="score-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
