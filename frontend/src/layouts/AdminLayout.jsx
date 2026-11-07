import { Outlet } from 'react-router-dom';
import Navbar from '../components/Navbar';
import SideNav from '../components/SideNav';
import SkipLink from '../components/SkipLink';

const LINKS = [
  { to: '/admin/dashboard', label: 'Dashboard' },
  { to: '/admin/appointments', label: 'Appointments' },
  { to: '/admin/doctors', label: 'Doctors' },
  { to: '/admin/analytics', label: 'Analytics' },
];

export default function AdminLayout() {
  return (
    <div className="app-shell">
      <SkipLink />
      <Navbar />
      <div className="area">
        <SideNav links={LINKS} label="Admin navigation" />
        <main id="main-content" tabIndex={-1} className="area-main"><Outlet /></main>
      </div>
    </div>
  );
}
