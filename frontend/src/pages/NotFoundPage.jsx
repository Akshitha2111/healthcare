import { Link } from 'react-router-dom';
import useDocumentTitle from '../hooks/useDocumentTitle';

export default function NotFoundPage() {
  useDocumentTitle('Page not found');
  return (
    <main id="main-content" className="page-center text-center">
      <h1>404 - Page not found</h1>
      <p className="text-muted-ink">The page you are looking for does not exist or has moved.</p>
      <Link to="/" className="btn btn-primary">Back to home</Link>
    </main>
  );
}
