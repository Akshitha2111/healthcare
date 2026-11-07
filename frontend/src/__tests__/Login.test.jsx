import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import Login from '../pages/Login'
import { authApi } from '../services/endpoints'
import { renderApp } from './helpers'

vi.mock('../services/endpoints', () => ({ authApi: { me: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() } }))

beforeEach(() => {
  vi.clearAllMocks()
  authApi.me.mockRejectedValue({ response: { status: 401 } })
})

describe('Login page', () => {
  it('shows validation errors and does not call the API', async () => {
    renderApp(<Login />, { route: '/login', path: '/login' })
    await userEvent.click(await screen.findByRole('button', { name: /log in/i }))
    expect(await screen.findByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('logs a patient in and redirects to the dashboard', async () => {
    authApi.login.mockResolvedValue({ user: { id: 1, fullName: 'Asha', role: 'PATIENT' } })
    renderApp(<Login />, { route: '/login', path: '/login' })
    await userEvent.type(await screen.findByLabelText('Email'), 'asha@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'Passw0rd1')
    await userEvent.click(screen.getByRole('button', { name: /log in/i }))
    expect(await screen.findByText('PATIENT HOME')).toBeInTheDocument()
    expect(authApi.login).toHaveBeenCalledWith({ email: 'asha@example.com', password: 'Passw0rd1' })
  })

  it('redirects an administrator to the admin area', async () => {
    authApi.login.mockResolvedValue({ user: { id: 2, fullName: 'Root', role: 'ADMIN' } })
    renderApp(<Login />, { route: '/login', path: '/login' })
    await userEvent.type(await screen.findByLabelText('Email'), 'admin@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'Passw0rd1')
    await userEvent.click(screen.getByRole('button', { name: /log in/i }))
    expect(await screen.findByText('ADMIN HOME')).toBeInTheDocument()
  })

  it('shows an error for wrong credentials', async () => {
    authApi.login.mockRejectedValue({ response: { status: 401, data: { message: 'Invalid email or password' } } })
    renderApp(<Login />, { route: '/login', path: '/login' })
    await userEvent.type(await screen.findByLabelText('Email'), 'asha@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: /log in/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
  })

  it('disables the button while the request is running', async () => {
    let resolve
    authApi.login.mockReturnValue(new Promise((r) => { resolve = r }))
    renderApp(<Login />, { route: '/login', path: '/login' })
    await userEvent.type(await screen.findByLabelText('Email'), 'a@b.co')
    await userEvent.type(screen.getByLabelText('Password'), 'x')
    await userEvent.click(screen.getByRole('button', { name: /log in/i }))
    expect(screen.getByRole('button', { name: /log in/i })).toBeDisabled()
    resolve({ user: { id: 1, fullName: 'A', role: 'PATIENT' } })
    await waitFor(() => expect(screen.getByText('PATIENT HOME')).toBeInTheDocument())
  })
})
