import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout, isLoggedIn, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/');

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
      <nav className="navbar">
        <div className="navbar-inner">
          <Link to="/" className="navbar-brand">
            <div className="brand-icon">⚡</div>
            CodeSphere
          </Link>

          <div className="navbar-nav">
            <Link to="/problems" className={`nav-link ${isActive('/problems') ? 'active' : ''}`}>
              Problems
            </Link>
            {isLoggedIn && (
                <Link to="/dashboard" className={`nav-link ${isActive('/dashboard') ? 'active' : ''}`}>
                  Dashboard
                </Link>
            )}
            {isLoggedIn && isAdmin && (
                <Link to="/admin" className={`nav-link ${isActive('/admin') ? 'active' : ''}`}>
                  Admin
                </Link>
            )}
            <Link to="/submissions" className={`nav-link ${isActive('/submissions') ? 'active' : ''}`}>
              Submissions
            </Link>
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