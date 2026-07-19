import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

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
      toast.success('Account created! Welcome to CodeSphere 🎉');
      navigate('/problems');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed. Try a different username/email.');
    } finally {
      setLoading(false);
    }
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
          <div style={{ fontSize: 40, marginBottom: 8 }}>🚀</div>
          <h1 className="auth-title">Create your account</h1>
          <p className="auth-subtitle">Join thousands of coders on CodeSphere</p>
        </div>

        <form className="auth-form" onSubmit={submit}>
          <div className="form-group">
            <label className="label">Username</label>
            <input
              className="input" id="reg-username" name="username"
              placeholder="Choose a username (3-20 chars)"
              value={form.username} onChange={handle}
              minLength={3} maxLength={20} required autoFocus
            />
          </div>
          <div className="form-group">
            <label className="label">Email</label>
            <input
              className="input" id="reg-email" name="email" type="email"
              placeholder="your@email.com"
              value={form.email} onChange={handle} required
            />
          </div>
          <div className="form-group">
            <label className="label">Password</label>
            <input
              className="input" id="reg-password" name="password" type="password"
              placeholder="At least 6 characters"
              value={form.password} onChange={handle} minLength={6} required
            />
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
            <input
              className="input" id="reg-confirm" name="confirm" type="password"
              placeholder="Repeat your password"
              value={form.confirm} onChange={handle} required
            />
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
