import { Link } from 'react-router-dom';
import DisclaimerBanner from '../components/DisclaimerBanner';
import useDocumentTitle from '../hooks/useDocumentTitle';

const FEATURES = [
  { title: 'Symptom checker', text: 'Choose the symptoms you have and see which conditions a machine-learning model finds most similar.' },
  { title: 'Plain-language explanation', text: 'See which of your symptoms matched, what is typical for the top match and the limits of the model.' },
  { title: 'Book appointments', text: 'Pick a doctor, a day and an available time slot, then manage or cancel your bookings.' },
  { title: 'Your history', text: 'Search and review earlier checks so you can share them with a clinician.' },
];

const STEPS = [
  { n: 1, title: 'Create an account', text: 'Register in under a minute with your name and email.' },
  { n: 2, title: 'Select your symptoms', text: 'Search the list and pick at least two symptoms you are experiencing.' },
  { n: 3, title: 'Review and book', text: 'Read the explanation and guidance, then book a doctor if you want a professional opinion.' },
];

export default function LandingPage() {
  useDocumentTitle('Welcome');
  return (
    <>
      <section className="hero" aria-labelledby="hero-title">
        <div className="hero-inner">
          <h1 id="hero-title">Understand your symptoms. Talk to a doctor with confidence.</h1>
          <p className="hero-lead">An educational symptom checker and appointment assistant. It suggests what your symptoms resemble, it never replaces a clinician.</p>
          <div className="hero-cta">
            <Link to="/register" className="btn btn-primary btn-lg">Create free account</Link>
            <Link to="/login" className="btn btn-outline-secondary btn-lg">Log in</Link>
          </div>
        </div>
      </section>

      <div className="container-narrow">
        <DisclaimerBanner />
      </div>

      <section className="section container-narrow" aria-labelledby="features-title">
        <h2 id="features-title">What you can do</h2>
        <div className="row g-3">
          {FEATURES.map((f) => (
            <div className="col-12 col-md-6" key={f.title}>
              <div className="card-surface h-100">
                <h3 className="h5">{f.title}</h3>
                <p className="mb-0">{f.text}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="section container-narrow" aria-labelledby="how-title">
        <h2 id="how-title">How it works</h2>
        <ol className="steps">
          {STEPS.map((s) => (
            <li key={s.n} className="card-surface step">
              <span className="step-num" aria-hidden="true">{s.n}</span>
              <div>
                <h3 className="h5">{s.title}</h3>
                <p className="mb-0">{s.text}</p>
              </div>
            </li>
          ))}
        </ol>
        <div className="text-center mt-4">
          <Link to="/register" className="btn btn-primary">Get started</Link>
        </div>
      </section>
    </>
  );
}
