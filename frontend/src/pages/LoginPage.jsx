import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

export default function LoginPage() {
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handle = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authAPI.login(form);
      login(res.data);
      toast.success(`Welcome back, ${res.data.username}! 🎉`);
      const isAdmin = ['ROLE_SUPER_ADMIN', 'ROLE_ORG_ADMIN', 'ROLE_EXAMINER'].includes(res.data.role);
      navigate(isAdmin ? '/admin' : '/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const continueWithGoogle = () => {
    window.location.assign('/oauth2/authorization/google');
  };

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in">
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ fontSize: 40, marginBottom: 8 }}>⚡</div>
          <h1 className="auth-title">Welcome back</h1>
          <p className="auth-subtitle">Sign in to your CodeSphere account</p>
        </div>

        <form className="auth-form" onSubmit={submit}>
          <div className="form-group">
            <label className="label">Username or Email</label>
            <input
              className="input"
              id="login-username"
              name="usernameOrEmail"
              placeholder="Enter your username or email"
              value={form.usernameOrEmail}
              onChange={handle}
              required
              autoFocus
            />
          </div>
          <div style={{ textAlign: 'right', marginTop: -8 }}><Link to="/forgot-password" className="auth-link" style={{ fontSize: 13 }}>Forgot password?</Link></div>
          <div className="form-group">
            <label className="label">Password</label>
            <input
              className="input"
              id="login-password"
              name="password"
              type="password"
              placeholder="Enter your password"
              value={form.password}
              onChange={handle}
              required
            />
          </div>

          <button
            type="submit"
            id="login-submit"
            className="btn btn-primary w-full"
            style={{ justifyContent: 'center', padding: '12px' }}
            disabled={loading}
          >
            {loading ? <><div className="spinner" style={{ width: 16, height: 16 }} /> Signing in...</> : 'Sign In'}
          </button>
        </form>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '20px 0' }}>
          <div style={{ height: 1, flex: 1, background: 'var(--border)' }} />
          <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>OR</span>
          <div style={{ height: 1, flex: 1, background: 'var(--border)' }} />
        </div>

        <button
          type="button"
          className="btn w-full"
          style={{
            justifyContent: 'center', padding: '12px', border: '1px solid var(--border)',
            background: '#fff', color: '#3c4043', fontWeight: 600,
          }}
          onClick={continueWithGoogle}
        >
          <svg aria-hidden="true" width="18" height="18" viewBox="0 0 18 18" focusable="false">
            <path fill="#EA4335" d="M17.64 9.205c0-.638-.057-1.251-.164-1.841H9v3.483h4.844a4.14 4.14 0 0 1-1.798 2.716v2.258h2.909c1.702-1.567 2.685-3.875 2.685-6.616Z" />
            <path fill="#4285F4" d="M9 18c2.43 0 4.467-.806 5.955-2.179l-2.909-2.258c-.806.54-1.837.859-3.046.859-2.344 0-4.328-1.584-5.037-3.71H.956v2.332A9 9 0 0 0 9 18Z" />
            <path fill="#FBBC05" d="M3.963 10.712A5.41 5.41 0 0 1 3.681 9c0-.594.102-1.172.282-1.712V4.956H.956A9 9 0 0 0 0 9c0 1.452.347 2.827.956 4.044l3.007-2.332Z" />
            <path fill="#34A853" d="M9 3.578c1.321 0 2.508.454 3.442 1.345l2.582-2.582C13.463.891 11.426 0 9 0A9 9 0 0 0 .956 4.956l3.007 2.332C4.672 5.162 6.656 3.578 9 3.578Z" />
          </svg>
          Continue with Google
        </button>

        <div style={{ marginTop: 24, textAlign: 'center', fontSize: 14, color: 'var(--text-secondary)' }}>
          Don't have an account?{' '}
          <Link to="/register" className="auth-link">Create one →</Link>
        </div>

        {/* Demo credentials hint */}
        <div style={{
          marginTop: 20, padding: '12px 16px', background: 'rgba(124,58,237,0.08)',
          border: '1px solid rgba(124,58,237,0.2)', borderRadius: 'var(--radius-sm)',
          fontSize: 13, color: 'var(--text-secondary)'
        }}>
          <strong style={{ color: 'var(--accent-light)' }}>Demo credentials:</strong><br />
          Admin: admin@demo.com / password123<br />
          Candidate: candidate@demo.com / password123
        </div>
      </div>
    </div>
  );
}
