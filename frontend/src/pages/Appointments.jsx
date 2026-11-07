import { useEffect, useState } from 'react'
import { Button, EmptyState, ErrorAlert, LoadingBlock, Pagination, SuccessAlert } from '../components/ui'
import useAsync from '../hooks/useAsync'
import { appointmentApi } from '../services/endpoints'
import { errorMessage, formatDate, formatTime, titleCase, todayIso } from '../utils/format'

const STATUS_BADGE = { PENDING: 'warning', CONFIRMED: 'success', COMPLETED: 'secondary', CANCELLED: 'danger' }

function BookingPanel({ onBooked }) {
  const doctors = useAsync(() => appointmentApi.doctors(), [])
  const [doctorId, setDoctorId] = useState('')
  const [date, setDate] = useState('')
  const [slots, setSlots] = useState(null)
  const [slot, setSlot] = useState('')
  const [slotError, setSlotError] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState({ ok: '', err: '' })

  useEffect(() => {
    setSlot(''); setSlots(null); setSlotError('')
    if (!doctorId || !date) return undefined
    let alive = true
    appointmentApi.slots(doctorId, date).then((s) => alive && setSlots(s)).catch((e) => alive && setSlotError(errorMessage(e)))
    return () => { alive = false }
  }, [doctorId, date])

  if (doctors.loading) return <LoadingBlock label="Loading doctors…" />
  if (doctors.error) return <ErrorAlert message={doctors.error} onRetry={doctors.reload} />
  if (doctors.data.length === 0) return <EmptyState title="No doctors are available right now">Please check back later.</EmptyState>
  const doctor = doctors.data.find((d) => String(d.id) === String(doctorId))

  const book = async (e) => {
    e.preventDefault()
    setMessage({ ok: '', err: '' })
    setBusy(true)
    try {
      await appointmentApi.book({ doctorId: Number(doctorId), date, time: slot })
      setMessage({ ok: `Appointment requested with ${doctor.name} on ${formatDate(date)} at ${slot}. It is pending confirmation.`, err: '' })
      setSlot('')
      setSlots(await appointmentApi.slots(doctorId, date))
      onBooked()
    } catch (err) {
      setMessage({ ok: '', err: errorMessage(err) })
      appointmentApi.slots(doctorId, date).then(setSlots).catch(() => {})
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={book}>
      <div className="row g-3 mb-3">
        {doctors.data.map((d) => (
          <div className="col-12 col-md-6 col-lg-4" key={d.id}>
            <label className={`card h-100 ${String(d.id) === String(doctorId) ? 'border-primary border-2' : ''}`} style={{ cursor: 'pointer' }}>
              <div className="card-body">
                <input type="radio" name="doctor" className="form-check-input me-2" value={d.id} checked={String(d.id) === String(doctorId)} onChange={(e) => setDoctorId(e.target.value)} />
                <span className="fw-semibold">{d.name}</span>
                <div className="text-muted small">{d.specialty}</div>
                <div className="small mt-2">{d.availableDays.map((x) => titleCase(x).slice(0, 3)).join(', ')} · {formatTime(d.startTime)}–{formatTime(d.endTime)} · {d.slotMinutes} min slots</div>
              </div>
            </label>
          </div>
        ))}
      </div>

      <div className="mb-3" style={{ maxWidth: 260 }}>
        <label htmlFor="appt-date" className="form-label">Date</label>
        <input id="appt-date" type="date" className="form-control" min={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} disabled={!doctorId} />
      </div>

      {slotError && <ErrorAlert message={slotError} />}
      {slots && (slots.length === 0 ? <p className="text-muted">This doctor is not available on that day.</p> : (
        <fieldset className="mb-3">
          <legend className="h6">Available times</legend>
          <div className="d-flex flex-wrap gap-2">
            {slots.map((s) => {
              const t = formatTime(s.time)
              return <button key={t} type="button" disabled={!s.available} aria-pressed={slot === t} className={`btn btn-sm slot-btn ${slot === t ? 'btn-primary' : 'btn-outline-primary'}`} onClick={() => setSlot(t)}>{t}</button>
            })}
          </div>
        </fieldset>
      ))}

      <SuccessAlert message={message.ok} />
      <ErrorAlert message={message.err} />
      <Button type="submit" className="btn btn-primary" loading={busy} disabled={!doctorId || !date || !slot}>Confirm booking</Button>
    </form>
  )
}

function MyAppointments({ refreshKey }) {
  const [page, setPage] = useState(0)
  const [version, setVersion] = useState(0)
  const [actionError, setActionError] = useState('')
  const { loading, error, data, reload } = useAsync(() => appointmentApi.mine({ page, size: 10 }), [page, refreshKey, version])

  const cancel = async (a) => {
    if (!window.confirm(`Cancel your appointment with ${a.doctorName} on ${formatDate(a.date)} at ${formatTime(a.time)}?`)) return
    setActionError('')
    try { await appointmentApi.cancel(a.id); setVersion((v) => v + 1) } catch (e) { setActionError(errorMessage(e)) }
  }

  if (loading) return <LoadingBlock />
  if (error) return <ErrorAlert message={error} onRetry={reload} />
  if (data.content.length === 0) return <EmptyState title="You have no appointments yet">Use the “Book” tab to schedule one.</EmptyState>
  return (
    <>
      <ErrorAlert message={actionError} />
      <div className="table-responsive card"><table className="table mb-0">
        <thead><tr><th scope="col">Date</th><th scope="col">Time</th><th scope="col">Doctor</th><th scope="col">Status</th><th scope="col"><span className="visually-hidden">Actions</span></th></tr></thead>
        <tbody>
          {data.content.map((a) => (
            <tr key={a.id}>
              <td>{formatDate(a.date)}</td><td>{formatTime(a.time)}</td>
              <td>{a.doctorName}<div className="small text-muted">{a.specialty}</div></td>
              <td><span className={`badge text-bg-${STATUS_BADGE[a.status]}`}>{titleCase(a.status)}</span></td>
              <td>{a.cancellable && <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => cancel(a)}>Cancel</button>}</td>
            </tr>
          ))}
        </tbody>
      </table></div>
      <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
    </>
  )
}

export default function Appointments() {
  const [tab, setTab] = useState('book')
  const [refreshKey, setRefreshKey] = useState(0)
  return (
    <div>
      <h1 className="h3">Appointments</h1>
      <ul className="nav nav-tabs mb-3" role="tablist">
        {[['book', 'Book'], ['mine', 'My appointments']].map(([k, label]) => (
          <li className="nav-item" role="presentation" key={k}>
            <button type="button" role="tab" aria-selected={tab === k} className={`nav-link ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{label}</button>
          </li>
        ))}
      </ul>
      {tab === 'book' ? <BookingPanel onBooked={() => setRefreshKey((k) => k + 1)} /> : <MyAppointments refreshKey={refreshKey} />}
    </div>
  )
}
