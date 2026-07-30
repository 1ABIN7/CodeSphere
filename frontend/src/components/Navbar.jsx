import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../api';

const ADMIN_NAV_ITEMS = [
  { to: '/admin', label: 'Overview' },
  { to: '/admin/assessments', label: 'Assessments' },
  { to: '/admin/questions', label: 'Question bank' },
  { to: '/admin/evaluations', label: 'Evaluation' },
  { to: '/admin/reports', label: 'Reports' },
  { to: '/admin/users', label: 'People' },
  { to: '/admin/ai-insights', label: 'AI insights' },
  { to: '/admin/security', label: 'Security' },
];

export default function Navbar() {
  const { user, logout, isLoggedIn, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const isEvaluator = user?.role === 'ROLE_EXAMINER';

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/');

  const handleLogout = async () => {
    try { await authAPI.logout(); } catch { /* Clear the local session even if the server is unavailable. */ }
    finally {
      logout();
      navigate('/');
    }
  };

  return (
      <nav className="navbar">
        <div className="navbar-inner">
          <Link to={isEvaluator ? "/admin/evaluations" : (isAdmin ? "/admin" : "/")} className="navbar-brand">
            <div className="brand-icon">⚡</div>
            CodeSphere
          </Link>

          <div className="navbar-nav">
            {isLoggedIn && !isAdmin && (
                <>
                  <Link to="/dashboard" className={`nav-link ${isActive('/dashboard') ? 'active' : ''}`}>Dashboard</Link>
                  <Link to="/coding-help" className={`nav-link ${isActive('/coding-help') ? 'active' : ''}`}>Coding Help</Link>
                </>
            )}
            {isLoggedIn && isEvaluator && (
              <Link to="/admin/evaluations" className={`nav-link ${isActive('/admin/evaluations') ? 'active' : ''}`}>
                My reviews
              </Link>
            )}
            {isLoggedIn && isAdmin && !isEvaluator && (
              ADMIN_NAV_ITEMS.map((item) => (
                <Link key={item.to} to={item.to} className={`nav-link ${(item.to === '/admin' ? location.pathname === '/admin' : isActive(item.to)) ? 'active' : ''}`}>
                  {item.label}
                </Link>
              ))
            )}
          </div>

          <div className="navbar-actions">
            {isLoggedIn ? (
                <>
              <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                👤 {user?.username}
              </span>
                  <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
                    Log Out
                  </button>
                </>
            ) : (
                <>
                  <Link to="/login" className="btn btn-ghost btn-sm">Log In</Link>
                  <Link to="/register" className="btn btn-primary btn-sm">Sign Up</Link>
                </>
            )}
          </div>
        </div>
      </nav>
  );
}
