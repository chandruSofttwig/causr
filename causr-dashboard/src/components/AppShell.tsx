import { NavLink, Outlet } from 'react-router-dom';
import logoUrl from '../assets/logo.png';
import { AnomaliesIcon, DashboardIcon, LogsIcon, TracesIcon } from './icons';

const links = [
  { to: '/', label: 'Dashboard', end: true, Icon: DashboardIcon },
  { to: '/logs', label: 'Logs', Icon: LogsIcon },
  { to: '/anomalies', label: 'Anomalies', Icon: AnomaliesIcon },
  { to: '/traces', label: 'Traces', end: false, Icon: TracesIcon },
] as const;


/** logo.png — 1536×1024 (3:2); width slightly wider than strict aspect for sidebar legibility */
const LOGO_WIDTH = 169;
const LOGO_HEIGHT = 64;

export function AppShell() {
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <img
            src={logoUrl}
            alt="Causr — Observability, Intelligence. Clarity."
            className="sidebar-logo"
            width={LOGO_WIDTH}
            height={LOGO_HEIGHT}
          />
        </div>
        <ul className="sidebar-nav">
          {links.map((link) => (
            <li key={link.to}>
              <NavLink to={link.to} end={'end' in link ? link.end : undefined}>
                <link.Icon className="nav-icon" />
                <span>{link.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
