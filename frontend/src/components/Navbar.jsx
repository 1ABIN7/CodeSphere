import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Bell } from 'lucide-react';

export default function Navbar() {
  const { user, logout, isLoggedIn, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/');

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const isEvaluator = user?.role === 'ROLE_EXAMINER';
  const isCandidate = user?.role === 'ROLE_CANDIDATE' || user?.role === 'ROLE_USER';

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <div className="brand-icon">⚡</div>
          CodeSphere
        </Link>

        <div className="navbar-nav">
          <Link to="/problems" className={`nav-link ${isActive('/problems') ? 'active' : ''}`}>
            Practice
          </Link>
          
          {isLoggedIn && isAdmin && !isEvaluator && (
            <>
              <Link to="/admin" className={`nav-link ${isActive('/admin') ? 'active' : ''}`}>
                Admin Dashboard
              </Link>
              <Link to="/admin/assessments/new" className={`nav-link ${isActive('/admin/assessments/new') ? 'active' : ''}`}>
                Create Assessment
              </Link>
            </>
          )}

          {isLoggedIn && isEvaluator && (
            <>
              <Link to="/evaluator" className={`nav-link ${isActive('/evaluator') ? 'active' : ''}`}>
                Evaluator Dashboard
              </Link>
            </>
          )}

          {isLoggedIn && (isCandidate || (!isAdmin && !isEvaluator)) && (
            <>
              <Link to="/assessments" className={`nav-link ${isActive('/assessments') ? 'active' : ''}`}>
                My Assessments
              </Link>
              <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
                Profile
              </Link>
            </>
          )}
        </div>

        <div className="navbar-actions">
          {isLoggedIn ? (
            <>
              <button className="btn btn-ghost btn-icon" title="Notifications">
                <Bell size={18} />
              </button>
              <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                👤 {user?.username || 'User'}
              </span>
              <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
                Log Out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">Login</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Get Started</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
