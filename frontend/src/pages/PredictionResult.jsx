import { Link, useParams } from 'react-router-dom'
import { Disclaimer, ErrorAlert, LoadingBlock, UrgentNotice } from '../components/ui'
import useAsync from '../hooks/useAsync'
import { predictionApi } from '../services/endpoints'
import { formatDateTime, percent } from '../utils/format'

export function PredictionView({ prediction: p }) {
  const [top, ...others] = p.results
  return (
    <div>
      <h1 className="h3">Prediction result</h1>
      <p className="text-muted">{formatDateTime(p.createdAt)} · model {p.modelName} v{p.modelVersion}</p>
      <UrgentNotice>{p.urgentCareNotice}</UrgentNotice>

      <div className="card mb-3"><div className="card-body">
        <div className="text-muted small text-uppercase fw-semibold">Most likely match according to the model</div>
        <div className="display-6 fw-bold" data-testid="top-prediction">{p.predictedClass}</div>
        {top && <div className="mt-2" style={{ maxWidth: 420 }}>
          <div className="d-flex justify-content-between small"><span>Model score</span><span>{percent(top.score)}</span></div>
          <div className="score-bar" role="img" aria-label={`Model score ${percent(top.score)}`}><div style={{ width: percent(top.score) }} /></div>
        </div>}
        {p.description && <p className="mt-3 mb-0">{p.description}</p>}
      </div></div>

      {others.length > 0 && (
        <div className="card mb-3"><div className="card-body">
          <h2 className="h5">Alternative model predictions</h2>
          <ul className="list-group list-group-flush">
            {others.map((r) => (
              <li key={r.rank} className="list-group-item px-0 d-flex justify-content-between"><span>{r.rank}. {r.disease}</span><span className="text-muted">{percent(r.score)}</span></li>
            ))}
          </ul>
        </div></div>
      )}

      <div className="card mb-3"><div className="card-body">
        <h2 className="h5">How to read this</h2>
        <ul className="mb-0">
          <li><strong>Symptoms you submitted:</strong> {p.symptoms.map((s) => s.label).join(', ')}.</li>
          <li><strong>Scores</strong> are relative values produced by the model ({p.scoreType === 'uncalibrated_model_score' ? 'uncalibrated' : p.scoreType}). They are <em>not</em> the probability that you have a condition.</li>
          {p.explanation?.influentialSymptoms?.length > 0 && (
            <li><strong>What the model reacted to most:</strong> {p.explanation.influentialSymptoms.slice(0, 3).map((i) => i.label).join(', ')}. {p.explanation.caveat}</li>
          )}
        </ul>
        {p.limitations?.length > 0 && (<>
          <h3 className="h6 mt-3">Limitations</h3>
          <ul className="mb-0 small">{p.limitations.map((l) => <li key={l}>{l}</li>)}</ul>
        </>)}
      </div></div>

      {p.generalGuidance?.length > 0 && (
        <div className="card mb-3"><div className="card-body">
          <h2 className="h5">General guidance</h2>
          <ul className="mb-0">{p.generalGuidance.map((g) => <li key={g}>{g}</li>)}</ul>
        </div></div>
      )}

      <Disclaimer>{p.disclaimer}</Disclaimer>
      <div className="d-flex flex-wrap gap-2 mt-3">
        <Link className="btn btn-primary" to="/appointments">Arrange a consultation</Link>
        <Link className="btn btn-outline-primary" to="/history">View history</Link>
        <Link className="btn btn-outline-secondary" to="/symptom-checker">New check</Link>
      </div>
    </div>
  )
}

export default function PredictionResult() {
  const { id } = useParams()
  const { loading, error, data, reload } = useAsync(() => predictionApi.get(id), [id])
  if (loading) return <LoadingBlock label="Loading result…" />
  if (error) return <ErrorAlert message={error} onRetry={reload} />
  return <PredictionView prediction={data} />
}
