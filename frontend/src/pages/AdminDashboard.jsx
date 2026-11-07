import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Button, EmptyState, ErrorAlert, LoadingBlock, Pagination, StatCard, SuccessAlert } from '../components/ui'
import useAsync from '../hooks/useAsync'
import { adminApi } from '../services/endpoints'
import { errorMessage, formatDate, formatTime, titleCase } from '../utils/format'

const COLORS = { PENDING: '#d97706', CONFIRMED: '#0f766e', COMPLETED: '#475569', CANCELLED: '#b91c1c' }
const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

function Overview() {
  const { loading, error, data, reload } = useAsync(() => Promise.all([adminApi.dashboard(), adminApi.analytics()]), [])
  if (loading) return <LoadingBlock />
  if (error) return <ErrorAlert message={error} onRetry={reload} />
  const [d, a] = data
  const statusData = Object.entries(d.appointmentsByStatus).map(([name, value]) => ({ name, value }))
  return (
    <>
      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3"><StatCard label="Registered patients" value={d.totalPatients} hint={`${d.newPatientsLast30Days} new in 30 days`} /></div>
        <div className="col-6 col-lg-3"><StatCard label="Appointments" value={d.totalAppointments} hint={`${d.pendingAppointments} pending`} /></div>
        <div className="col-6 col-lg-3"><StatCard label="Predictions" value={d.totalPredictions} hint={`${d.predictionsLast7Days} in last 7 days`} /></div>
        <div className="col-6 col-lg-3"><StatCard label="Active doctors" value={d.activeDoctors} /></div>
      </div>
      <div className="row g-3">
        <div className="col-12 col-lg-5"><div className="card h-100"><div className="card-body">
          <h2 className="h6">Appointments by status</h2>
          {d.totalAppointments === 0 ? <EmptyState title="No appointments yet" /> : (
            <div style={{ height: 260 }}><ResponsiveContainer><PieChart>
              <Pie data={statusData.filter((s) => s.value > 0)} dataKey="value" nameKey="name" innerRadius={50} outerRadius={90} label>
                {statusData.filter((s) => s.value > 0).map((s) => <Cell key={s.name} fill={COLORS[s.name]} />)}
              </Pie><Tooltip /><Legend />
            </PieChart></ResponsiveContainer></div>
          )}
        </div></div></div>
        <div className="col-12 col-lg-7"><div className="card h-100"><div className="card-body">
          <h2 className="h6">Prediction requests per day (30 days)</h2>
          <div style={{ height: 260 }}><ResponsiveContainer><LineChart data={a.predictionsPerDay}>
            <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tickFormatter={(v) => v.slice(5)} minTickGap={20} /><YAxis allowDecimals={false} /><Tooltip />
            <Line type="monotone" dataKey="count" name="Predictions" stroke="#0f766e" strokeWidth={2} dot={false} />
          </LineChart></ResponsiveContainer></div>
        </div></div></div>
      </div>
    </>
  )
}

function Analytics() {
  const { loading, error, data, reload } = useAsync(() => adminApi.analytics(), [])
  if (loading) return <LoadingBlock />
  if (error) return <ErrorAlert message={error} onRetry={reload} />
  const chart = (title, rows, color) => (
    <div className="card h-100"><div className="card-body">
      <h2 className="h6">{title}</h2>
      {rows.length === 0 ? <p className="text-muted small mb-0">Not enough data yet. Groups with fewer than {data.minGroupSize} distinct patients are hidden to protect privacy.</p> : (
        <div style={{ height: Math.max(220, rows.length * 34) }}><ResponsiveContainer><BarChart data={rows} layout="vertical" margin={{ left: 40 }}>
          <CartesianGrid strokeDasharray="3 3" /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="label" width={130} /><Tooltip />
          <Bar dataKey="count" fill={color} radius={[0, 4, 4, 0]} />
        </BarChart></ResponsiveContainer></div>
      )}
    </div></div>
  )
  return (
    <div className="row g-3">
      <div className="col-12 col-lg-6">{chart('Most selected symptoms (distinct patients)', data.topSymptoms, '#0f766e')}</div>
      <div className="col-12 col-lg-6">{chart('Most frequent predicted conditions', data.topPredictedConditions, '#155e75')}</div>
      <div className="col-12"><div className="card"><div className="card-body">
        <h2 className="h6">Model usage</h2>
        {data.modelUsage.length === 0 ? <p className="text-muted small mb-0">No predictions yet.</p> : (
          <table className="table mb-0"><thead><tr><th scope="col">Model</th><th scope="col">Version</th><th scope="col">Requests</th></tr></thead>
            <tbody>{data.modelUsage.map((m) => <tr key={m.modelName + m.modelVersion}><td>{m.modelName}</td><td>{m.modelVersion}</td><td>{m.count}</td></tr>)}</tbody></table>
        )}
        <p className="small text-muted mt-3 mb-0">Aggregates only. Individual patients’ symptoms and results are never shown here.</p>
      </div></div></div>
    </div>
  )
}

function AppointmentsAdmin() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [version, setVersion] = useState(0)
  const [msg, setMsg] = useState({ ok: '', err: '' })
  const { loading, error, data, reload } = useAsync(() => adminApi.appointments({ page, size: 10, status: status || undefined }), [page, status, version])

  const update = async (a, next) => {
    setMsg({ ok: '', err: '' })
    try { await adminApi.setStatus(a.id, next); setMsg({ ok: `Appointment #${a.id} is now ${titleCase(next)}.`, err: '' }); setVersion((v) => v + 1) }
    catch (e) { setMsg({ ok: '', err: errorMessage(e) }) }
  }

  return (
    <div>
      <div className="d-flex align-items-center gap-2 mb-3">
        <label htmlFor="status-filter" className="form-label mb-0">Status</label>
        <select id="status-filter" className="form-select w-auto" value={status} onChange={(e) => { setPage(0); setStatus(e.target.value) }}>
          <option value="">All</option>{Object.keys(COLORS).map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
        </select>
      </div>
      <SuccessAlert message={msg.ok} /><ErrorAlert message={msg.err} />
      {loading && <LoadingBlock />}
      <ErrorAlert message={error} onRetry={reload} />
      {data && (data.content.length === 0 ? <EmptyState title="No appointments" /> : (
        <>
          <div className="table-responsive card"><table className="table mb-0">
            <thead><tr><th scope="col">#</th><th scope="col">Patient</th><th scope="col">Doctor</th><th scope="col">When</th><th scope="col">Status</th><th scope="col">Change to</th></tr></thead>
            <tbody>{data.content.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td><td>{a.patientName}</td><td>{a.doctorName}<div className="small text-muted">{a.specialty}</div></td>
                <td>{formatDate(a.date)} {formatTime(a.time)}</td>
                <td><span className="badge" style={{ background: COLORS[a.status] }}>{titleCase(a.status)}</span></td>
                <td>{a.allowedTransitions.length === 0 ? <span className="text-muted small">Final</span> : (
                  <div className="d-flex gap-1 flex-wrap">{a.allowedTransitions.map((t) => (
                    <button key={t} type="button" className="btn btn-sm btn-outline-primary" onClick={() => update(a, t)} aria-label={`Set appointment ${a.id} to ${titleCase(t)}`}>{titleCase(t)}</button>))}</div>)}</td>
              </tr>))}</tbody>
          </table></div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      ))}
    </div>
  )
}

const EMPTY_DOCTOR = { name: '', specialty: '', availableDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], startTime: '09:00', endTime: '13:00', slotMinutes: 30, active: true }

function DoctorsAdmin() {
  const { loading, error, data, reload } = useAsync(() => adminApi.doctors(), [])
  const [editing, setEditing] = useState(null) // null | 'new' | doctor id
  const [form, setForm] = useState(EMPTY_DOCTOR)
  const [msg, setMsg] = useState({ ok: '', err: '' })
  const [busy, setBusy] = useState(false)

  const edit = (d) => { setEditing(d.id); setForm({ ...d, startTime: formatTime(d.startTime), endTime: formatTime(d.endTime) }); setMsg({ ok: '', err: '' }) }
  const toggleDay = (day) => setForm((f) => ({ ...f, availableDays: f.availableDays.includes(day) ? f.availableDays.filter((x) => x !== day) : [...f.availableDays, day] }))

  const save = async (e) => {
    e.preventDefault()
    setMsg({ ok: '', err: '' })
    if (form.availableDays.length === 0) return setMsg({ ok: '', err: 'Select at least one available day' })
    setBusy(true)
    const body = { name: form.name, specialty: form.specialty, availableDays: form.availableDays, startTime: form.startTime, endTime: form.endTime, slotMinutes: Number(form.slotMinutes), active: form.active }
    try {
      if (editing === 'new') await adminApi.createDoctor(body); else await adminApi.updateDoctor(editing, body)
      setMsg({ ok: 'Doctor saved.', err: '' }); setEditing(null); reload()
    } catch (err) { setMsg({ ok: '', err: errorMessage(err) }) } finally { setBusy(false) }
  }

  if (loading) return <LoadingBlock />
  if (error) return <ErrorAlert message={error} onRetry={reload} />
  return (
    <div>
      <SuccessAlert message={msg.ok} />
      {!editing && <ErrorAlert message={msg.err} />}
      <div className="d-flex justify-content-between mb-2"><h2 className="h5 mb-0">Doctors &amp; availability</h2>
        <button type="button" className="btn btn-primary btn-sm" onClick={() => { setEditing('new'); setForm(EMPTY_DOCTOR); setMsg({ ok: '', err: '' }) }}>Add doctor</button></div>
      <div className="table-responsive card mb-3"><table className="table mb-0">
        <thead><tr><th scope="col">Name</th><th scope="col">Specialty</th><th scope="col">Days</th><th scope="col">Hours</th><th scope="col">Status</th><th scope="col"><span className="visually-hidden">Edit</span></th></tr></thead>
        <tbody>{data.map((d) => (
          <tr key={d.id}><td>{d.name}</td><td>{d.specialty}</td><td className="small">{d.availableDays.map((x) => titleCase(x).slice(0, 3)).join(', ')}</td>
            <td>{formatTime(d.startTime)}–{formatTime(d.endTime)} ({d.slotMinutes} min)</td>
            <td><span className={`badge text-bg-${d.active ? 'success' : 'secondary'}`}>{d.active ? 'Active' : 'Inactive'}</span></td>
            <td><button type="button" className="btn btn-sm btn-outline-primary" onClick={() => edit(d)} aria-label={`Edit ${d.name}`}>Edit</button></td></tr>))}</tbody>
      </table></div>
      {editing && (
        <form className="card card-body" onSubmit={save}>
          <h3 className="h6">{editing === 'new' ? 'New doctor' : 'Edit doctor'}</h3>
          <div className="row g-3">
            <div className="col-12 col-md-6"><label htmlFor="d-name" className="form-label">Name</label><input id="d-name" className="form-control" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div className="col-12 col-md-6"><label htmlFor="d-spec" className="form-label">Specialty</label><input id="d-spec" className="form-control" required value={form.specialty} onChange={(e) => setForm({ ...form, specialty: e.target.value })} /></div>
            <div className="col-6 col-md-3"><label htmlFor="d-start" className="form-label">Start</label><input id="d-start" type="time" className="form-control" required value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></div>
            <div className="col-6 col-md-3"><label htmlFor="d-end" className="form-label">End</label><input id="d-end" type="time" className="form-control" required value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></div>
            <div className="col-6 col-md-3"><label htmlFor="d-slot" className="form-label">Slot (minutes)</label><input id="d-slot" type="number" min="10" max="120" step="5" className="form-control" required value={form.slotMinutes} onChange={(e) => setForm({ ...form, slotMinutes: e.target.value })} /></div>
            <div className="col-6 col-md-3 d-flex align-items-end"><div className="form-check"><input id="d-active" type="checkbox" className="form-check-input" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /><label htmlFor="d-active" className="form-check-label">Active (bookable)</label></div></div>
            <div className="col-12"><fieldset><legend className="form-label">Available days</legend>
              <div className="d-flex flex-wrap gap-3">{DAYS.map((day) => (<div className="form-check" key={day}><input id={`day-${day}`} type="checkbox" className="form-check-input" checked={form.availableDays.includes(day)} onChange={() => toggleDay(day)} /><label htmlFor={`day-${day}`} className="form-check-label">{titleCase(day)}</label></div>))}</div></fieldset></div>
          </div>
          <ErrorAlert message={msg.err} />
          <div className="d-flex gap-2 mt-3"><Button type="submit" className="btn btn-primary" loading={busy}>Save</Button><button type="button" className="btn btn-outline-secondary" onClick={() => setEditing(null)}>Cancel</button></div>
          <p className="small text-muted mt-2 mb-0">Changing availability does not cancel existing appointments; it only affects new bookings.</p>
        </form>
      )}
    </div>
  )
}

export default function AdminDashboard() {
  const [tab, setTab] = useState('overview')
  const tabs = [['overview', 'Overview'], ['analytics', 'Analytics'], ['appointments', 'Appointments'], ['doctors', 'Doctors']]
  return (
    <div>
      <h1 className="h3">Administration</h1>
      <ul className="nav nav-tabs mb-3" role="tablist">
        {tabs.map(([k, label]) => (
          <li className="nav-item" role="presentation" key={k}>
            <button type="button" role="tab" aria-selected={tab === k} className={`nav-link ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{label}</button>
          </li>))}
      </ul>
      {tab === 'overview' && <Overview />}
      {tab === 'analytics' && <Analytics />}
      {tab === 'appointments' && <AppointmentsAdmin />}
      {tab === 'doctors' && <DoctorsAdmin />}
    </div>
  )
}
