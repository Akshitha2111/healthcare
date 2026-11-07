import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { vi } from 'vitest'
import ProtectedRoute from '../components/ProtectedRoute'
import { AuthProvider } from '../context/AuthContext'
import { authApi } from '../services/endpoints'

vi.mock('../services/endpoints', () => ({ authApi: { me: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() } }))

function setup(route) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<div>LOGIN PAGE</div>} />
          <Route path="/dashboard" element={<div>PATIENT HOME</div>} />
          <Route path="/admin" element={<div>ADMIN HOME</div>} />
          <Route element={<ProtectedRoute roles={['PATIENT']} />}><Route path="/history" element={<div>HISTORY</div>} /></Route>
          <Route element={<ProtectedRoute roles={['ADMIN']} />}><Route path="/admin-only" element={<div>SECRET ADMIN</div>} /></Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  it('redirects anonymous users to the login page', async () => {
    authApi.me.mockRejectedValue({ response: { status: 401 } })
    setup('/history')
    expect(await screen.findByText('LOGIN PAGE')).toBeInTheDocument()
  })

  it('lets a patient into patient pages', async () => {
    authApi.me.mockResolvedValue({ id: 1, role: 'PATIENT', fullName: 'A' })
    setup('/history')
    expect(await screen.findByText('HISTORY')).toBeInTheDocument()
  })

  it('keeps a patient out of admin pages', async () => {
    authApi.me.mockResolvedValue({ id: 1, role: 'PATIENT', fullName: 'A' })
    setup('/admin-only')
    expect(await screen.findByText('PATIENT HOME')).toBeInTheDocument()
    expect(screen.queryByText('SECRET ADMIN')).not.toBeInTheDocument()
  })

  it('keeps an admin out of patient pages and allows admin pages', async () => {
    authApi.me.mockResolvedValue({ id: 2, role: 'ADMIN', fullName: 'Root' })
    setup('/history')
    expect(await screen.findByText('ADMIN HOME')).toBeInTheDocument()
  })
})
