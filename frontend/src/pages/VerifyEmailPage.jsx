import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authAPI } from '../api';

export default function VerifyEmailPage() {
  const [params] = useSearchParams(); const [status, setStatus] = useState('loading');
  useEffect(() => { const token = params.get('token'); if (!token) { setStatus('error'); return; } authAPI.verifyEmail(token).then(() => setStatus('success')).catch(() => setStatus('error')); }, [params]);
  return <div className="auth-page"><div className="card auth-card fade-in"><h1 className="auth-title">{status === 'loading' ? 'Verifying email…' : status === 'success' ? 'Email verified' : 'Verification failed'}</h1><p className="auth-subtitle">{status === 'success' ? 'Your email address is now verified.' : status === 'error' ? 'The verification link is invalid or expired.' : 'Please wait a moment.'}</p>{status !== 'loading' && <Link to="/login" className="btn btn-primary">Go to sign in</Link>}</div></div>;
}
