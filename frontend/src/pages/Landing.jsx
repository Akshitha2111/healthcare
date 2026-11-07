import { Link } from 'react-router-dom'
import { Disclaimer } from '../components/ui'
import { homePathFor, useAuth } from '../context/AuthContext'

const features = [
  ['Symptom-based predictions', 'Pick your symptoms and get ranked model predictions from a trained machine-learning classifier.'],
  ['Transparent results', 'See which symptoms influenced the result and read plainly what the model cannot know.'],
  ['Private history', 'Your previous checks are stored in your own account and visible only to you.'],
  ['Book a professional', 'Arrange an appointment with a healthcare professional when you want a real evaluation.'],
]
const steps = [
  ['Select symptoms', 'Choose at least two symptoms from the supported list.'],
  ['Model analysis', 'A Flask service scores the symptoms with the trained model.'],
  ['Review guidance', 'Read the possible matches, limitations and general guidance.'],
  ['Talk to a professional', 'Use the result as a starting point for a consultation, not as a diagnosis.'],
]

export default function Landing() {
  const { user } = useAuth()
  return (
    <div>
      <section className="hero p-4 p-md-5 mb-4">
        <h1 className="display-5 fw-bold">AI-Powered Smart Healthcare Diagnosis Assistant</h1>
        <p className="lead text-muted col-lg-8">Preliminary, educational health information from a machine-learning model, with prediction history and appointment booking in one place.</p>
        <div className="d-flex flex-wrap gap-2">
          {user ? (
            <Link className="btn btn-primary btn-lg" to={homePathFor(user)}>Go to your dashboard</Link>
          ) : (
            <>
              <Link className="btn btn-primary btn-lg" to="/register">Create an account</Link>
              <Link className="btn btn-outline-primary btn-lg" to="/login">Log in</Link>
            </>
          )}
        </div>
      </section>

      <section aria-labelledby="features" className="mb-5">
        <h2 id="features" className="h3 mb-3">Features</h2>
        <div className="row g-3">
          {features.map(([t, d]) => (
            <div className="col-12 col-md-6 col-lg-3" key={t}>
              <div className="card h-100"><div className="card-body"><h3 className="h6 fw-bold">{t}</h3><p className="mb-0 text-muted small">{d}</p></div></div>
            </div>
          ))}
        </div>
      </section>

      <section aria-labelledby="how" className="mb-5">
        <h2 id="how" className="h3 mb-3">How it works</h2>
        <div className="row g-3">
          {steps.map(([t, d], i) => (
            <div className="col-12 col-md-6 col-lg-3" key={t}>
              <div className="d-flex gap-3"><span className="step-num">{i + 1}</span><div><div className="fw-semibold">{t}</div><div className="text-muted small">{d}</div></div></div>
            </div>
          ))}
        </div>
      </section>

      <Disclaimer />
    </div>
  )
}
