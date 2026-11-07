import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import Register from '../pages/Register'
import { authApi } from '../services/endpoints'
import { renderApp } from './helpers'

vi.mock('../services/endpoints', () => ({ authApi: { me: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() } }))

beforeEach(() => {
  vi.clearAllMocks()
  authApi.me.mockRejectedValue({ response: { status: 401 } })
})

async function fill({ name = 'Asha Rao', email = 'asha@example.com', pw = 'Passw0rd1', confirm = 'Passw0rd1' } = {}) {
  await userEvent.type(await screen.findByLabelText('Full name'), name)
  await userEvent.type(screen.getByLabelText('Email'), email)
  await userEvent.type(screen.getByLabelText('Password'), pw)
  await userEvent.type(screen.getByLabelText('Confirm password'), confirm)
}

describe('Register page', () => {
  it('blocks mismatched passwords', async () => {
    renderApp(<Register />, { route: '/register', path: '/register' })
    await fill({ confirm: 'Different1' })
    await userEvent.click(screen.getByRole('button', { name: 'Register' }))
    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument()
    expect(authApi.register).not.toHaveBeenCalled()
  })

  it('registers and goes to the dashboard', async () => {
    authApi.register.mockResolvedValue({ user: { id: 1, fullName: 'Asha Rao', role: 'PATIENT' } })
    renderApp(<Register />, { route: '/register', path: '/register' })
    await fill()
    await userEvent.click(screen.getByRole('button', { name: 'Register' }))
    expect(await screen.findByText('PATIENT HOME')).toBeInTheDocument()
    expect(authApi.register).toHaveBeenCalledWith({ fullName: 'Asha Rao', email: 'asha@example.com', password: 'Passw0rd1' })
  })

  it('shows the server error for a duplicate email', async () => {
    authApi.register.mockRejectedValue({ response: { status: 409, data: { message: 'An account with this email already exists' } } })
    renderApp(<Register />, { route: '/register', path: '/register' })
    await fill()
    await userEvent.click(screen.getByRole('button', { name: 'Register' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('already exists')
  })
})
