import { Link, Outlet } from 'react-router-dom';
import SkipLink from '../components/SkipLink';
import { useAuth } from '../context/AuthContext';
import { homeFor } from '../utils/routes';

export default function PublicLayout() {
  const { user } = useAuth();
  return (
    <div className="app-shell">
      <SkipLink />
      <header className="topbar">
        <Link to="/" className="brand" aria-label="Healthcare Diagnosis Assistant home">
          <span className="brand-mark" aria-hidden="true">+</span>
          <span className="brand-text">Health Assistant</span>
        </Link>
        <nav className="topbar-user" aria-label="Public">
          {user ? (
            <Link className="btn btn-primary btn-sm" to={homeFor(user.role)}>Go to dashboard</Link>
          ) : (
            <>
              <Link className="btn btn-outline-secondary btn-sm" to="/login">Log in</Link>
              <Link className="btn btn-primary btn-sm" to="/register">Create account</Link>
            </>
          )}
        </nav>
      </header>
      <main id="main-content" tabIndex={-1} className="public-main">
        <Outlet />
      </main>
      <footer className="site-footer">
        <p>Educational tool only. Not a medical diagnosis. In an emergency contact your local emergency services.</p>
      </footer>
    </div>
  );
}
