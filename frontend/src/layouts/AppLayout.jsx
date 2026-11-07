import { Outlet } from 'react-router-dom'
import Navbar from '../components/Navbar'

export default function AppLayout() {
  return (
    <div className="d-flex flex-column min-vh-100">
      <a className="skip-link" href="#main">Skip to content</a>
      <Navbar />
      <main id="main" className="container py-4 flex-grow-1"><Outlet /></main>
      <footer className="app-footer py-3 text-center small">
        Educational project — not a medical device and not a substitute for professional medical advice.
      </footer>
    </div>
  )
}
