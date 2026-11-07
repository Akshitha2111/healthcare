import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Button, ErrorAlert, FieldError } from '../components/ui'
import { homePathFor, useAuth } from '../context/AuthContext'
import { errorMessage } from '../utils/format'
import { hasErrors, validateLogin } from '../utils/validation'

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homePathFor(user)} replace />

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setServerError('')
    const v = validateLogin(form)
    setErrors(v)
    if (hasErrors(v)) return
    setBusy(true)
    try {
      const u = await login({ email: form.email.trim(), password: form.password })
      const from = location.state?.from
      navigate(u.role === 'ADMIN' ? '/admin' : from || '/dashboard', { replace: true })
    } catch (err) {
      setServerError(err?.response?.status === 401 ? 'Invalid email or password.' : errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-5">
        <div className="card"><div className="card-body p-4">
          <h1 className="h3 mb-3">Log in</h1>
          <ErrorAlert message={serverError} />
          <form onSubmit={submit} noValidate>
            <div className="mb-3">
              <label htmlFor="email" className="form-label">Email</label>
              <input id="email" name="email" type="email" autoComplete="email" className={`form-control ${errors.email ? 'is-invalid' : ''}`} value={form.email} onChange={change} aria-invalid={!!errors.email} aria-describedby={errors.email ? 'email-err' : undefined} />
              <FieldError id="email-err" message={errors.email} />
            </div>
            <div className="mb-3">
              <label htmlFor="password" className="form-label">Password</label>
              <input id="password" name="password" type="password" autoComplete="current-password" className={`form-control ${errors.password ? 'is-invalid' : ''}`} value={form.password} onChange={change} aria-invalid={!!errors.password} aria-describedby={errors.password ? 'password-err' : undefined} />
              <FieldError id="password-err" message={errors.password} />
            </div>
            <Button type="submit" className="btn btn-primary w-100" loading={busy}>Log in</Button>
          </form>
          <p className="mt-3 mb-0 small">No account? <Link to="/register">Register</Link></p>
        </div></div>
      </div>
    </div>
  )
}
