import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authExtendedAPI } from '../api';
import { Mail } from 'lucide-react';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      // TODO: backend endpoint pending — see AuthController
      await authExtendedAPI.forgotPassword({ email });
      setSent(true);
    } catch {
      setSent(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in">
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ fontSize: 40, marginBottom: 8 }}>🔑</div>
          <h1 className="auth-title">Forgot password?</h1>
          <p className="auth-subtitle">Enter your email and we'll send you a reset link</p>
        </div>

        {sent ? (
          <div style={{ textAlign: 'center', padding: '16px 0' }}>
            <div style={{
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              width: 56, height: 56, borderRadius: '50%', background: 'rgba(16,185,129,0.12)',
              marginBottom: 16,
            }}>
              <Mail size={28} style={{ color: 'var(--green)' }} />
            </div>
            <p style={{ fontSize: 14, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              If an account with that email exists, we've sent reset instructions.
            </p>
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit}>
            <div className="form-group">
              <label className="label">Email</label>
              <input
                className="input"
                id="forgot-email"
                name="email"
                type="email"
                placeholder="your@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
              />
            </div>

            <button
              type="submit"
              id="forgot-submit"
              className="btn btn-primary w-full"
              style={{ justifyContent: 'center', padding: '12px' }}
              disabled={loading}
            >
              {loading ? <><div className="spinner" style={{ width: 16, height: 16 }} /> Sending...</> : 'Send Reset Link'}
            </button>
          </form>
        )}

        <div style={{ marginTop: 24, textAlign: 'center', fontSize: 14, color: 'var(--text-secondary)' }}>
          <Link to="/login" className="auth-link">← Back to Sign In</Link>
        </div>
      </div>
    </div>
  );
}
