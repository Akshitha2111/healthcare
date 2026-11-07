import { Outlet } from 'react-router-dom';
import Navbar from '../components/Navbar';
import SideNav from '../components/SideNav';
import SkipLink from '../components/SkipLink';

const LINKS = [
  { to: '/app/dashboard', label: 'Dashboard' },
  { to: '/app/symptom-checker', label: 'Symptom checker' },
  { to: '/app/history', label: 'History' },
  { to: '/app/appointments', label: 'Appointments' },
  { to: '/app/profile', label: 'Profile' },
];

export default function PatientLayout() {
  return (
    <div className="app-shell">
      <SkipLink />
      <Navbar />
      <div className="area">
        <SideNav links={LINKS} label="Patient navigation" />
        <main id="main-content" tabIndex={-1} className="area-main"><Outlet /></main>
      </div>
    </div>
  );
}
