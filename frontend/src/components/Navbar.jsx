import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { homePathFor, useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const close = () => setOpen(false)

  const handleLogout = async () => {
    close()
    await logout()
    navigate('/')
  }

  const patientLinks = [['/dashboard', 'Dashboard'], ['/symptom-checker', 'Symptom checker'], ['/history', 'History'], ['/appointments', 'Appointments'], ['/profile', 'Profile']]
  const links = user?.role === 'ADMIN' ? [['/admin', 'Admin dashboard']] : user ? patientLinks : []

  return (
    <nav className="navbar navbar-expand-lg navbar-dark app-nav" aria-label="Main navigation">
      <div className="container">
        <Link className="navbar-brand fw-bold" to={user ? homePathFor(user) : '/'} onClick={close}>
          <span aria-hidden="true">✚</span> Smart Health Assistant
        </Link>
        <button className="navbar-toggler" type="button" aria-controls="nav" aria-expanded={open} aria-label="Toggle navigation" onClick={() => setOpen(!open)}>
          <span className="navbar-toggler-icon" />
        </button>
        <div id="nav" className={`collapse navbar-collapse ${open ? 'show' : ''}`}>
          <ul className="navbar-nav me-auto">
            {links.map(([to, label]) => (
              <li className="nav-item" key={to}><NavLink className="nav-link" to={to} onClick={close}>{label}</NavLink></li>
            ))}
          </ul>
          <div className="d-flex gap-2 align-items-center">
            {user ? (
              <>
                <span className="text-white-50 small d-none d-lg-inline">{user.fullName}</span>
                <button type="button" className="btn btn-outline-light btn-sm" onClick={handleLogout}>Log out</button>
              </>
            ) : (
              <>
                <Link className="btn btn-outline-light btn-sm" to="/login" onClick={close}>Log in</Link>
                <Link className="btn btn-light btn-sm" to="/register" onClick={close}>Register</Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}
