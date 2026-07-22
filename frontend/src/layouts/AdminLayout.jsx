import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, FileQuestion, Calendar, BarChart3, ClipboardCheck, ScrollText, LogOut, Bell } from 'lucide-react';

const ADMIN_NAV = [
  { path: '/admin/dashboard', label: 'Dashboard', icon: <LayoutDashboard size={18} /> },
  { path: '/admin/assessments', label: 'Assessments', icon: <Calendar size={18} /> },
  { path: '/admin/questions', label: 'Question Bank', icon: <FileQuestion size={18} /> },
  { path: '/admin/evaluations', label: 'Evaluations', icon: <ClipboardCheck size={18} /> },
  { path: '/admin/reports', label: 'Reports', icon: <BarChart3 size={18} /> },
  { path: '/admin/audit-logs', label: 'Audit Logs', icon: <ScrollText size={18} /> },
  { path: '/admin/notifications', label: 'Notifications', icon: <Bell size={18} /> },
];

export default function AdminLayout() {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="app-layout">
      <div className="sidebar">
        <div className="sidebar-header">
          <Link to="/" className="brand-icon" style={{ textDecoration: 'none' }}>&#9889;</Link>
          <span className="sidebar-title">CodeSphere</span>
        </div>
        <div className="sidebar-role-badge">
          {user?.role?.replace('ROLE_', '')}
        </div>
        <nav className="sidebar-nav">
          {ADMIN_NAV.map((item) => {
            const isActive = window.location.pathname === item.path || window.location.pathname.startsWith(item.path + '/');
            return (
              <Link key={item.path} to={item.path} className={`sidebar-link ${isActive ? 'active' : ''}`}>
                <span className="sidebar-icon">{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div style={{ marginTop: 'auto', padding: '16px' }}>
          <button
            className="btn btn-ghost btn-sm w-full"
            onClick={() => navigate('/dashboard')}
            style={{ justifyContent: 'flex-start' }}
          >
            <LogOut size={16} /> Exit Admin
          </button>
        </div>
      </div>
      <div className="app-main">
        <header className="app-header">
          <div className="header-greeting">Admin Console</div>
        </header>
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
