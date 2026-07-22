import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authExtendedAPI } from '../api';
import toast from 'react-hot-toast';
import { CheckCircle } from 'lucide-react';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [form, setForm] = useState({ password: '', confirm: '' });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handle = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const valid = form.password.length >= 6 && form.password === form.confirm;

  const submit = async (e) => {
    e.preventDefault();
    if (!token) { toast.error('No reset token found'); return; }
    if (form.password.length < 6) { toast.error('Password must be at least 6 characters'); return; }
    if (form.password !== form.confirm) { toast.error('Passwords do not match'); return; }
    setLoading(true);
    try {
      // TODO: backend endpoint pending — see AuthController
      await authExtendedAPI.resetPassword({ token, newPassword: form.password });
      setSuccess(true);
      toast.success('Password has been reset!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Reset failed. The link may have expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in">
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ fontSize: 40, marginBottom: 8 }}>🔒</div>
          <h1 className="auth-title">Reset password</h1>
          <p className="auth-subtitle">Enter your new password below</p>
        </div>

        {success ? (
          <div style={{ textAlign: 'center', padding: '16px 0' }}>
            <div style={{
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              width: 56, height: 56, borderRadius: '50%', background: 'rgba(16,185,129,0.12)',
              marginBottom: 16,
            }}>
              <CheckCircle size={28} style={{ color: 'var(--green)' }} />
            </div>
            <p style={{ fontSize: 14, color: 'var(--text-secondary)', marginBottom: 16 }}>
              Your password has been reset successfully.
            </p>
            <Link to="/login" className="btn btn-primary" style={{ justifyContent: 'center', padding: '12px 24px' }}>
              Sign In
            </Link>
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit}>
            <div className="form-group">
              <label className="label">New Password</label>
              <input
                className="input"
                id="reset-password"
                name="password"
                type="password"
                placeholder="At least 6 characters"
                value={form.password}
                onChange={handle}
                minLength={6}
                required
                autoFocus
              />
              {form.password && form.password.length < 6 && (
                <div style={{ fontSize: 12, color: 'var(--red)', marginTop: 4 }}>
                  Must be at least 6 characters
                </div>
              )}
            </div>
            <div className="form-group">
              <label className="label">Confirm Password</label>
              <input
                className="input"
                id="reset-confirm"
                name="confirm"
                type="password"
                placeholder="Repeat your password"
                value={form.confirm}
                onChange={handle}
                required
              />
              {form.confirm && form.password !== form.confirm && (
                <div style={{ fontSize: 12, color: 'var(--red)', marginTop: 4 }}>
                  Passwords do not match
                </div>
              )}
            </div>

            <button
              type="submit"
              id="reset-submit"
              className="btn btn-primary w-full"
              style={{ justifyContent: 'center', padding: '12px' }}
              disabled={loading || !valid}
            >
              {loading ? <><div className="spinner" style={{ width: 16, height: 16 }} /> Resetting...</> : 'Reset Password'}
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
