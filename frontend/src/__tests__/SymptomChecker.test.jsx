import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import SymptomChecker from '../pages/SymptomChecker'
import { authApi, predictionApi } from '../services/endpoints'
import { renderApp } from './helpers'

vi.mock('../services/endpoints', () => ({
  authApi: { me: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() },
  predictionApi: { symptoms: vi.fn(), create: vi.fn(), get: vi.fn(), mine: vi.fn() },
}))

const catalog = {
  count: 3, minSymptoms: 2, maxSymptoms: 20,
  symptoms: [{ name: 'high_fever', label: 'High fever' }, { name: 'cough', label: 'Cough' }, { name: 'headache', label: 'Headache' }],
}

beforeEach(() => {
  vi.clearAllMocks()
  authApi.me.mockResolvedValue({ id: 1, fullName: 'Asha', role: 'PATIENT' })
  predictionApi.symptoms.mockResolvedValue(catalog)
})

const open = () => renderApp(<SymptomChecker />, { route: '/symptom-checker', path: '/symptom-checker' })

describe('SymptomChecker', () => {
  it('shows a loading state, then the symptoms', async () => {
    open()
    expect(screen.getByRole('status')).toHaveTextContent(/loading symptoms/i)
    expect(await screen.findByRole('button', { name: 'Cough' })).toBeInTheDocument()
  })

  it('shows an error with retry when the catalog cannot be loaded', async () => {
    predictionApi.symptoms.mockRejectedValueOnce({ response: { status: 503, data: { message: 'The prediction service is temporarily unavailable.' } } })
    open()
    expect(await screen.findByRole('alert')).toHaveTextContent('temporarily unavailable')
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByRole('button', { name: 'Cough' })).toBeInTheDocument()
  })

  it('filters by search, toggles selection and enforces the minimum', async () => {
    open()
    await userEvent.type(await screen.findByLabelText('Search symptoms'), 'hea')
    expect(screen.queryByRole('button', { name: 'Cough' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Headache' }))
    expect(screen.getByText(/Selected symptoms \(1\)/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /get prediction/i })).toBeDisabled()
    await userEvent.clear(screen.getByLabelText('Search symptoms'))
    await userEvent.click(screen.getByRole('button', { name: 'Cough' }))
    expect(screen.getByRole('button', { name: /get prediction/i })).toBeEnabled()
    await userEvent.click(screen.getByRole('button', { name: 'Remove Cough' }))
    expect(screen.getByText(/Selected symptoms \(1\)/)).toBeInTheDocument()
  })

  it('submits the selected symptom names and navigates to the result', async () => {
    predictionApi.create.mockResolvedValue({ id: 7 })
    open()
    await userEvent.click(await screen.findByRole('button', { name: 'High fever' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cough' }))
    await userEvent.click(screen.getByRole('button', { name: /get prediction/i }))
    expect(predictionApi.create).toHaveBeenCalledWith(['high_fever', 'cough'])
  })

  it('shows the server error if the prediction fails', async () => {
    predictionApi.create.mockRejectedValue({ response: { status: 503, data: { message: 'The prediction service is temporarily unavailable. Please try again shortly.' } } })
    open()
    await userEvent.click(await screen.findByRole('button', { name: 'High fever' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cough' }))
    await userEvent.click(screen.getByRole('button', { name: /get prediction/i }))
    expect(await screen.findByText(/prediction service is temporarily unavailable/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /get prediction/i })).toBeEnabled()
  })
})
