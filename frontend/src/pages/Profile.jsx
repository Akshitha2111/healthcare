import { useEffect, useState } from 'react'
import { Button, ErrorAlert, FieldError, LoadingBlock, SuccessAlert } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import useAsync from '../hooks/useAsync'
import { patientApi } from '../services/endpoints'
import { errorMessage, fieldErrors, formatDate } from '../utils/format'
import { hasErrors, validateProfile } from '../utils/validation'

export default function Profile() {
  const { setUser } = useAuth()
  const { loading, error, data, reload } = useAsync(() => patientApi.profile(), [])
  const [form, setForm] = useState({ fullName: '', phone: '', dateOfBirth: '', gender: '' })
  const [errors, setErrors] = useState({})
  const [status, setStatus] = useState({ ok: '', err: '' })
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (data) setForm({ fullName: data.fullName || '', phone: data.phone || '', dateOfBirth: data.dateOfBirth || '', gender: data.gender || '' })
  }, [data])

  if (loading) return <LoadingBlock />
  if (error) return <ErrorAlert message={error} onRetry={reload} />

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })
  const submit = async (e) => {
    e.preventDefault()
    setStatus({ ok: '', err: '' })
    const v = validateProfile(form)
    setErrors(v)
    if (hasErrors(v)) return
    setBusy(true)
    try {
      const updated = await patientApi.updateProfile({
        fullName: form.fullName.trim(), phone: form.phone.trim() || null,
        dateOfBirth: form.dateOfBirth || null, gender: form.gender || null,
      })
      setUser((u) => ({ ...u, fullName: updated.fullName }))
      setStatus({ ok: 'Profile updated.', err: '' })
    } catch (err) {
      setErrors(fieldErrors(err))
      setStatus({ ok: '', err: errorMessage(err) })
    } finally {
      setBusy(false)
    }
  }

  const input = (name, label, type = 'text', extra = {}) => (
    <div className="mb-3">
      <label htmlFor={name} className="form-label">{label}</label>
      <input id={name} name={name} type={type} className={`form-control ${errors[name] ? 'is-invalid' : ''}`} value={form[name]} onChange={change} aria-invalid={!!errors[name]} {...extra} />
      <FieldError message={errors[name]} />
    </div>
  )

  return (
    <div className="row"><div className="col-12 col-lg-7">
      <h1 className="h3">Your profile</h1>
      <p className="text-muted">We only store what the service needs. Optional fields can be left empty.</p>
      <form className="card card-body" onSubmit={submit} noValidate>
        <div className="mb-3"><label htmlFor="email" className="form-label">Email</label><input id="email" className="form-control" value={data.email} readOnly aria-describedby="email-help" /><div id="email-help" className="form-text">Email cannot be changed.</div></div>
        {input('fullName', 'Full name', 'text', { autoComplete: 'name' })}
        {input('phone', 'Phone (optional)', 'tel', { autoComplete: 'tel' })}
        {input('dateOfBirth', 'Date of birth (optional)', 'date')}
        <div className="mb-3">
          <label htmlFor="gender" className="form-label">Gender (optional)</label>
          <select id="gender" name="gender" className="form-select" value={form.gender} onChange={change}>
            <option value="">Not specified</option><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other</option><option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
          </select>
        </div>
        <SuccessAlert message={status.ok} />
        <ErrorAlert message={status.err} />
        <Button type="submit" className="btn btn-primary align-self-start" loading={busy}>Save changes</Button>
        <div className="small text-muted mt-3">Member since {formatDate(data.memberSince)}</div>
      </form>
    </div></div>
  )
}
