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
      navigate('/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
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
