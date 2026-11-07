import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { EmptyState, ErrorAlert, LoadingBlock, StatCard } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import useAsync from '../hooks/useAsync'
import { appointmentApi, patientApi, predictionApi } from '../services/endpoints'
import { formatDateTime, formatTime, formatDate, percent } from '../utils/format'

export default function PatientDashboard() {
  const { user } = useAuth()
  const { loading, error, data, reload } = useAsync(
    () => Promise.all([
      patientApi.summary(),
      predictionApi.mine({ page: 0, size: 5 }),
      appointmentApi.mine({ page: 0, size: 5, upcoming: true }),
    ]),
    [],
  )

  if (loading) return <LoadingBlock />
  if (error) return <ErrorAlert message={error} onRetry={reload} />
  const [summary, recent, upcoming] = data

  return (
    <div>
      <h1 className="h3">Welcome, {user.fullName.split(' ')[0]}</h1>
      <p className="text-muted">Your personal overview. Only you can see this information.</p>

      <div className="d-flex flex-wrap gap-2 mb-4">
        <Link className="btn btn-primary" to="/symptom-checker">Check symptoms</Link>
        <Link className="btn btn-outline-primary" to="/appointments">Book an appointment</Link>
        <Link className="btn btn-outline-primary" to="/history">View history</Link>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3"><StatCard label="Predictions" value={summary.totalPredictions} /></div>
        <div className="col-6 col-lg-3"><StatCard label="Upcoming appointments" value={summary.upcomingAppointments} /></div>
      </div>

      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <div className="card h-100"><div className="card-body">
            <h2 className="h5">Recent predictions</h2>
            {recent.content.length === 0 ? (
              <EmptyState title="No predictions yet"><Link to="/symptom-checker">Run your first symptom check</Link></EmptyState>
            ) : (
              <ul className="list-group list-group-flush">
                {recent.content.map((p) => (
                  <li key={p.id} className="list-group-item px-0 d-flex justify-content-between align-items-center">
                    <div>
                      <Link to={`/predictions/${p.id}`} className="fw-semibold">{p.predictedClass}</Link>
                      <div className="small text-muted">{formatDateTime(p.createdAt)}</div>
                    </div>
                    {p.topScore != null && <span className="badge text-bg-light" title="Relative model score, not a probability">score {percent(p.topScore)}</span>}
                  </li>
                ))}
              </ul>
            )}
          </div></div>
        </div>
        <div className="col-12 col-lg-6">
          <div className="card h-100"><div className="card-body">
            <h2 className="h5">Upcoming appointments</h2>
            {upcoming.content.length === 0 ? (
              <EmptyState title="Nothing scheduled"><Link to="/appointments">Book an appointment</Link></EmptyState>
            ) : (
              <ul className="list-group list-group-flush">
                {upcoming.content.map((a) => (
                  <li key={a.id} className="list-group-item px-0">
                    <div className="fw-semibold">{a.doctorName} <span className="text-muted fw-normal">· {a.specialty}</span></div>
                    <div className="small text-muted">{formatDate(a.date)} at {formatTime(a.time)} · {a.status}</div>
                  </li>
                ))}
              </ul>
            )}
          </div></div>
        </div>
        <div className="col-12">
          <div className="card"><div className="card-body">
            <h2 className="h5">Your activity (last 6 months)</h2>
            <div style={{ width: '100%', height: 220 }}>
              <ResponsiveContainer>
                <BarChart data={summary.predictionsByMonth}>
                  <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="month" /><YAxis allowDecimals={false} /><Tooltip />
                  <Bar dataKey="count" name="Predictions" fill="#0f766e" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div></div>
        </div>
      </div>
    </div>
  )
}
