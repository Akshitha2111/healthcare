import { render } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'

export function renderApp(ui, { route = '/', path = '*' } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>
        <Routes>
          <Route path={path} element={ui} />
          <Route path="/login" element={<div>LOGIN PAGE</div>} />
          <Route path="/dashboard" element={<div>PATIENT HOME</div>} />
          <Route path="/admin" element={<div>ADMIN HOME</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}
