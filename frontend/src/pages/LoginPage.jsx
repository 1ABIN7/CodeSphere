import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';
import { Mail, Lock, User } from 'lucide-react';

const GOOGLE_ICON = (
  <svg width="18" height="18" viewBox="0 0 18 18" xmlns="http://www.w3.org/2000/svg">
    <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4"/>
    <path d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853"/>
    <path d="M3.964 10.706A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.706V4.962H.957A8.997 8.997 0 0 0 0 9c0 1.452.348 2.827.957 4.038l3.007-2.332Z" fill="#FBBC05"/>
    <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.962L3.964 7.294C4.672 5.166 6.656 3.58 9 3.58Z" fill="#EA4335"/>
  </svg>
);

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
      toast.success(`Welcome back, ${res.data.username}!`);
      navigate(res.data.role === 'ROLE_ADMIN' ? '/admin/dashboard' : '/assessments');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = '/oauth2/authorization/google';
  };

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in" style={{ maxWidth: 460, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ fontSize: 42, marginBottom: 8 }}>⚡</div>
          <h1 className="auth-title" style={{ fontSize: 26, fontWeight: 700, marginBottom: 6 }}>Welcome back</h1>
          <p className="auth-subtitle" style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
            Sign in to your CodeSphere account
          </p>
        </div>

        {/* Google OAuth Button */}
        <button
          type="button"
          className="btn w-full"
          onClick={handleGoogleLogin}
          style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            padding: '11px', background: '#fff', color: '#3c4043',
            border: '1px solid #dadce0', fontWeight: 500, fontSize: 14,
            cursor: 'pointer', borderRadius: 'var(--radius-sm, 6px)', marginBottom: 18,
          }}
        >
          {GOOGLE_ICON}
          <span style={{ marginLeft: 8 }}>Continue with Google</span>
        </button>

        <div className="auth-divider" style={{ position: 'relative', textAlign: 'center', margin: '16px 0 20px' }}>
          <span style={{ padding: '0 12px', position: 'relative', zIndex: 1, background: 'var(--bg-card)', color: 'var(--text-muted)', fontSize: 13 }}>or sign in with email</span>
          <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 1, background: 'var(--border)' }} />
        </div>

        <form className="auth-form" onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="form-group">
            <label className="label" style={{ fontSize: 13, fontWeight: 500 }}>Username or Email</label>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}><User size={16} /></span>
              <input
                className="input"
                id="login-username"
                name="usernameOrEmail"
                placeholder="alex_morgan or alex@example.com"
                value={form.usernameOrEmail}
                onChange={handle}
                required
                autoFocus
                style={{ paddingLeft: 38, width: '100%' }}
              />
            </div>
          </div>

          <div className="form-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
              <label className="label" style={{ fontSize: 13, fontWeight: 500, margin: 0 }}>Password</label>
              <Link to="/forgot-password" className="auth-link" style={{ fontSize: 12, color: '#a78bfa' }}>Forgot password?</Link>
            </div>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}><Lock size={16} /></span>
              <input
                className="input"
                id="login-password"
                name="password"
                type="password"
                placeholder="Enter your password"
                value={form.password}
                onChange={handle}
                required
                style={{ paddingLeft: 38, width: '100%' }}
              />
            </div>
          </div>

          <button
            type="submit"
            id="login-submit"
            className="btn btn-primary w-full"
            style={{
              justifyContent: 'center', padding: '12px', marginTop: 8,
              fontSize: 15, fontWeight: 600, borderRadius: 'var(--radius-sm, 6px)'
            }}
            disabled={loading}
          >
            {loading ? <><div className="spinner" style={{ width: 16, height: 16 }} /> Signing in...</> : 'Sign In'}
          </button>
        </form>

        <div style={{ marginTop: 24, textAlign: 'center', fontSize: 14, color: 'var(--text-secondary)' }}>
          Don't have an account?{' '}
          <Link to="/register" className="auth-link" style={{ fontWeight: 600, color: '#a78bfa' }}>Create one →</Link>
        </div>

        {/* Demo credentials hint */}
        <div style={{
          marginTop: 20, padding: '12px 16px', background: 'rgba(124,58,237,0.1)',
          border: '1px solid rgba(139,92,246,0.25)', borderRadius: 'var(--radius-sm, 6px)',
          fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.6
        }}>
          <strong style={{ color: '#a78bfa', display: 'block', marginBottom: 4 }}>Demo credentials:</strong>
          <div>Admin: <code style={{ color: '#e2e8f0', background: 'rgba(0,0,0,0.3)', padding: '1px 5px', borderRadius: 3 }}>admin@demo.com</code> / <code style={{ color: '#e2e8f0', background: 'rgba(0,0,0,0.3)', padding: '1px 5px', borderRadius: 3 }}>password123</code></div>
          <div>Candidate: <code style={{ color: '#e2e8f0', background: 'rgba(0,0,0,0.3)', padding: '1px 5px', borderRadius: 3 }}>candidate@demo.com</code> / <code style={{ color: '#e2e8f0', background: 'rgba(0,0,0,0.3)', padding: '1px 5px', borderRadius: 3 }}>password123</code></div>
        </div>
      </div>
    </div>
  );
}

