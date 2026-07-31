import { useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authAPI } from '../api';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState(''); const [sent, setSent] = useState(false); const [loading, setLoading] = useState(false);
  const submit = async (event) => { event.preventDefault(); setLoading(true); try { await authAPI.forgotPassword({ email }); setSent(true); } catch (error) { toast.error(error.response?.data?.message || 'Unable to request a password reset.'); } finally { setLoading(false); } };
  return <div className="auth-page"><div className="card auth-card fade-in"><h1 className="auth-title">Reset your password</h1><p className="auth-subtitle">Enter your account email to receive reset instructions.</p>{sent ? <div className="feedback-box correct">If this email is registered, a reset token has been created. Check the backend mail/log configuration for the reset link.</div> : <form className="auth-form" onSubmit={submit}><label className="label">Email<input className="input" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} /></label><button className="btn btn-primary w-full" disabled={loading}>{loading ? 'Sending...' : 'Send reset instructions'}</button></form>}<div style={{ marginTop: 20, textAlign: 'center' }}><Link to="/login" className="auth-link">Back to sign in</Link></div></div></div>;
}
