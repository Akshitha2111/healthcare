import { NavLink } from 'react-router-dom';

export default function SideNav({ links, label }) {
  return (
    <nav className="sidenav" aria-label={label}>
      <ul>
        {links.map((l) => (
          <li key={l.to}>
            <NavLink to={l.to} className={({ isActive }) => `sidenav-link ${isActive ? 'active' : ''}`}>{l.label}</NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
