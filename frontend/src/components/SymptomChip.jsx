/** A real checkbox styled as a chip: keyboard operable (Tab, Space) and screen-reader friendly. */
export default function SymptomChip({ id, label, checked, onChange, redFlag = false, disabled = false }) {
  const inputId = `symptom-${id}`;
  return (
    <span className={`chip ${checked ? 'chip-checked' : ''} ${redFlag ? 'chip-redflag' : ''} ${disabled ? 'chip-disabled' : ''}`}>
      <input
        id={inputId}
        type="checkbox"
        className="chip-input"
        checked={checked}
        disabled={disabled}
        onChange={() => onChange(id)}
      />
      <label htmlFor={inputId} className="chip-label">
        {label}
        {redFlag && <span className="chip-flag"> (red flag)</span>}
      </label>
    </span>
  );
}
