import { screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { render } from '@testing-library/react'
import { PredictionView } from '../pages/PredictionResult'

const prediction = {
  id: 1, createdAt: '2026-10-30T10:00:00Z', predictedClass: 'Fungal infection', modelName: 'logistic_regression',
  modelVersion: '1.0.0', scoreType: 'uncalibrated_model_score',
  symptoms: [{ name: 'itching', label: 'Itching' }, { name: 'skin_rash', label: 'Skin rash' }],
  results: [{ rank: 1, disease: 'Fungal infection', score: 0.61 }, { rank: 2, disease: 'Allergy', score: 0.21 }, { rank: 3, disease: 'Drug Reaction', score: 0.08 }],
  description: 'A fungal skin condition.', generalGuidance: ['See a clinician.'], urgentCareNotice: 'Seek urgent care if severe.',
  limitations: ['Educational only.'], explanation: { method: 'm', influentialSymptoms: [{ symptom: 'itching', label: 'Itching', influence: 0.3 }], caveat: 'Not medical causality.' },
  disclaimer: 'Not a medical diagnosis.',
}

describe('PredictionView', () => {
  it('renders the model output, alternatives, limits and disclaimer', () => {
    render(<MemoryRouter><PredictionView prediction={prediction} /></MemoryRouter>)
    expect(screen.getByTestId('top-prediction')).toHaveTextContent('Fungal infection')
    expect(screen.getByText(/Allergy/)).toBeInTheDocument()
    expect(screen.getByText('61%')).toBeInTheDocument()
    expect(screen.getByText(/the probability that you have a condition/i)).toBeInTheDocument()
    expect(screen.getByText('Itching, Skin rash.', { exact: false })).toBeInTheDocument()
    expect(screen.getByText('Not a medical diagnosis.', { exact: false })).toBeInTheDocument()
    expect(screen.getByText('Seek urgent care if severe.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /arrange a consultation/i })).toHaveAttribute('href', '/appointments')
  })
})
