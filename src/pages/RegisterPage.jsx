import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';
import { User, Mail, Lock } from 'lucide-react';

const GOOGLE_ICON = (
  <svg width="18" height="18" viewBox="0 0 18 18" xmlns="http://www.w3.org/2000/svg">
    <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4"/>
    <path d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853"/>
    <path d="M3.964 10.706A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.706V4.962H.957A8.997 8.997 0 0 0 0 9c0 1.452.348 2.827.957 4.038l3.007-2.332Z" fill="#FBBC05"/>
    <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.962L3.964 7.294C4.672 5.166 6.656 3.58 9 3.58Z" fill="#EA4335"/>
  </svg>
);

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '', confirm: '' });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handle = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirm) { toast.error('Passwords do not match'); return; }
    if (form.password.length < 6) { toast.error('Password must be at least 6 characters'); return; }
    setLoading(true);
    try {
      const res = await authAPI.register({ username: form.username, email: form.email, password: form.password });
      login(res.data);
      toast.success('Account created! Welcome to CodeSphere');
      navigate('/problems');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed. Try a different username/email.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleRegister = () => {
    window.location.href = '/oauth2/authorization/google';
  };

  const strength = (pw) => {
    if (!pw) return null;
    if (pw.length < 6) return { label: 'Too short', color: 'var(--red)', w: '20%' };
    if (pw.length < 8) return { label: 'Weak', color: 'var(--yellow)', w: '40%' };
    if (!/[A-Z]/.test(pw) && !/\d/.test(pw)) return { label: 'Fair', color: 'var(--yellow)', w: '60%' };
    if (/[A-Z]/.test(pw) && /\d/.test(pw)) return { label: 'Strong', color: 'var(--green)', w: '100%' };
    return { label: 'Good', color: 'var(--green)', w: '80%' };
  };
  const s = strength(form.password);

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in">
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ fontSize: 40, marginBottom: 8 }}><span role="img" aria-label="rocket">&#128640;</span></div>
          <h1 className="auth-title">Create your account</h1>
          <p className="auth-subtitle">Join CodeSphere and start assessing talent</p>
        </div>

        {/* Google OAuth Button */}
        <button
          type="button"
          className="btn w-full"
          onClick={handleGoogleRegister}
          style={{
            justifyContent: 'center', padding: '12px',
            background: '#fff', color: '#3c4043', border: '1px solid #dadce0',
            fontWeight: 500, fontSize: 14, cursor: 'pointer',
            borderRadius: 'var(--radius-sm)', marginBottom: 20,
          }}
        >
          {GOOGLE_ICON}
          <span style={{ marginLeft: 8 }}>Continue with Google</span>
        </button>

        <div className="auth-divider">
          <span style={{ padding: '0 12px', position: 'relative', zIndex: 1, background: 'var(--bg-card)' }}>or</span>
          <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 1, background: 'var(--border)' }} />
        </div>

        <form className="auth-form" onSubmit={submit}>
          <div className="form-group">
            <label className="label">Username</label>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}><User size={16} /></span>
              <input
                className="input" id="reg-username" name="username"
                placeholder="Choose a username (3-20 chars)"
                value={form.username} onChange={handle}
                minLength={3} maxLength={20} required autoFocus
                style={{ paddingLeft: 36 }}
              />
            </div>
          </div>
          <div className="form-group">
            <label className="label">Email</label>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}><Mail size={16} /></span>
              <input
                className="input" id="reg-email" name="email" type="email"
                placeholder="your@email.com"
                value={form.email} onChange={handle} required
                style={{ paddingLeft: 36 }}
              />
            </div>
          </div>
          <div className="form-group">
            <label className="label">Password</label>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}><Lock size={16} /></span>
              <input
                className="input" id="reg-password" name="password" type="password"
                placeholder="At least 6 characters"
                value={form.password} onChange={handle} minLength={6} required
                style={{ paddingLeft: 36 }}
              />
            </div>
            {s && (
              <div style={{ marginTop: 6 }}>
                <div style={{ height: 4, background: 'var(--bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: s.w, background: s.color, borderRadius: 2, transition: 'width 0.3s, background 0.3s' }} />
                </div>
                <div style={{ fontSize: 12, color: s.color, marginTop: 4 }}>{s.label}</div>
              </div>
            )}
          </div>
          <div className="form-group">
            <label className="label">Confirm Password</label>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}><Lock size={16} /></span>
              <input
                className="input" id="reg-confirm" name="confirm" type="password"
                placeholder="Repeat your password"
                value={form.confirm} onChange={handle} required
                style={{ paddingLeft: 36 }}
              />
            </div>
          </div>

          <button
            type="submit" id="reg-submit"
            className="btn btn-primary w-full"
            style={{ justifyContent: 'center', padding: '12px' }}
            disabled={loading}
          >
            {loading ? <><div className="spinner" style={{ width: 16, height: 16 }} /> Creating account...</> : 'Create Account'}
          </button>
        </form>

        <div style={{ marginTop: 24, textAlign: 'center', fontSize: 14, color: 'var(--text-secondary)' }}>
          Already have an account?{' '}
          <Link to="/login" className="auth-link">Sign in →</Link>
        </div>
      </div>
    </div>
  );
}
