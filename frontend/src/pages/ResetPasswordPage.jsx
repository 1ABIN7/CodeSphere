import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authAPI } from '../api';

export default function ResetPasswordPage() {
  const [params] = useSearchParams(); const navigate = useNavigate(); const [password, setPassword] = useState(''); const [loading, setLoading] = useState(false); const token = params.get('token') || '';
  const submit = async (event) => { event.preventDefault(); if (!token) return toast.error('This reset link is missing its token.'); setLoading(true); try { await authAPI.resetPassword({ token, newPassword: password }); toast.success('Password updated. You can sign in now.'); navigate('/login'); } catch (error) { toast.error(error.response?.data?.message || 'Unable to reset password.'); } finally { setLoading(false); } };
  return <div className="auth-page"><div className="card auth-card fade-in"><h1 className="auth-title">Choose a new password</h1><form className="auth-form" onSubmit={submit}><label className="label">New password<input className="input" minLength="8" type="password" required value={password} onChange={(event) => setPassword(event.target.value)} /></label><button className="btn btn-primary w-full" disabled={loading}>{loading ? 'Updating...' : 'Update password'}</button></form><div style={{ marginTop: 20, textAlign: 'center' }}><Link to="/login" className="auth-link">Back to sign in</Link></div></div></div>;
}
