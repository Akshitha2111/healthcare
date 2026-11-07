import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Button, ErrorAlert, FieldError } from '../components/ui'
import { homePathFor, useAuth } from '../context/AuthContext'
import { errorMessage, fieldErrors } from '../utils/format'
import { hasErrors, validateRegister } from '../utils/validation'

export default function Register() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homePathFor(user)} replace />

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setServerError('')
    const v = validateRegister(form)
    setErrors(v)
    if (hasErrors(v)) return
    setBusy(true)
    try {
      await register({ fullName: form.fullName.trim(), email: form.email.trim(), password: form.password })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setErrors(fieldErrors(err))
      setServerError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const field = (name, label, type = 'text', autoComplete) => (
    <div className="mb-3">
      <label htmlFor={name} className="form-label">{label}</label>
      <input id={name} name={name} type={type} autoComplete={autoComplete} className={`form-control ${errors[name] ? 'is-invalid' : ''}`} value={form[name]} onChange={change} aria-invalid={!!errors[name]} aria-describedby={errors[name] ? `${name}-err` : undefined} />
      <FieldError id={`${name}-err`} message={errors[name]} />
    </div>
  )

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-5">
        <div className="card"><div className="card-body p-4">
          <h1 className="h3 mb-3">Create your account</h1>
          <ErrorAlert message={serverError} />
          <form onSubmit={submit} noValidate>
            {field('fullName', 'Full name', 'text', 'name')}
            {field('email', 'Email', 'email', 'email')}
            {field('password', 'Password', 'password', 'new-password')}
            <div className="form-text mb-3 mt-n2">At least 8 characters with a letter and a number.</div>
            {field('confirmPassword', 'Confirm password', 'password', 'new-password')}
            <Button type="submit" className="btn btn-primary w-100" loading={busy}>Register</Button>
          </form>
          <p className="mt-3 mb-0 small">Already registered? <Link to="/login">Log in</Link></p>
        </div></div>
      </div>
    </div>
  )
}
