import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Bell,
  LayoutDashboard,
  ClipboardCheck,
  BarChart3,
  Shield,
  User,
  BookOpen,
} from 'lucide-react';

export default function Navbar() {
  const { user, logout, isLoggedIn } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/');

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const role = user?.role;
  const isSuperAdmin = role === 'ROLE_SUPER_ADMIN';
  const isOrgAdmin = role === 'ROLE_ORG_ADMIN';
  const isInstructor = role === 'ROLE_INSTRUCTOR';
  const isEvaluator = role === 'ROLE_EXAMINER';
  const isCandidate = role === 'ROLE_CANDIDATE' || role === 'ROLE_USER';

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <div className="brand-icon">⚡</div>
          CodeSphere
        </Link>

        <div className="navbar-nav">
          {/* Logged out: show Practice */}
          {!isLoggedIn && (
            <Link to="/problems" className={`nav-link ${isActive('/problems') ? 'active' : ''}`}>
              <BookOpen size={14} /> Practice
            </Link>
          )}

          {/* Candidate */}
          {isLoggedIn && isCandidate && (
            <>
              <Link to="/dashboard" className={`nav-link ${isActive('/dashboard') ? 'active' : ''}`}>
                <LayoutDashboard size={14} /> Dashboard
              </Link>
              <Link to="/assessments" className={`nav-link ${isActive('/assessments') ? 'active' : ''}`}>
                <ClipboardCheck size={14} /> Assessments
              </Link>
              <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
                <User size={14} /> Profile
              </Link>
            </>
          )}

          {/* Examiner / Evaluator */}
          {isLoggedIn && isEvaluator && (
            <>
              <Link to="/evaluator" className={`nav-link ${isActive('/evaluator') ? 'active' : ''}`}>
                <BarChart3 size={14} /> Evaluator
              </Link>
              <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
                <User size={14} /> Profile
              </Link>
            </>
          )}

          {/* Admin (super, org) */}
          {isLoggedIn && (isSuperAdmin || isOrgAdmin) && (
            <>
              <Link to="/admin" className={`nav-link ${isActive('/admin') ? 'active' : ''}`}>
                <Shield size={14} /> Admin
              </Link>
              <Link to="/assessments" className={`nav-link ${isActive('/assessments') ? 'active' : ''}`}>
                <ClipboardCheck size={14} /> Assessments
              </Link>
              <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
                <User size={14} /> Profile
              </Link>
            </>
          )}

          {/* Instructor */}
          {isLoggedIn && isInstructor && (
            <>
              <Link to="/admin" className={`nav-link ${isActive('/admin') ? 'active' : ''}`}>
                <Shield size={14} /> Dashboard
              </Link>
              <Link to="/assessments" className={`nav-link ${isActive('/assessments') ? 'active' : ''}`}>
                <ClipboardCheck size={14} /> Assessments
              </Link>
              <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
                <User size={14} /> Profile
              </Link>
            </>
          )}
        </div>

        <div className="navbar-actions">
          {isLoggedIn ? (
            <>
              {/* TODO: wire up notification count from backend */}
              <button className="btn btn-ghost btn-icon" title="Notifications">
                <Bell size={18} />
              </button>
              <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                {user?.username || 'User'}
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
