import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authExtendedAPI } from '../api';
import { CheckCircle, XCircle } from 'lucide-react';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    if (!token) { setStatus('error'); return; }
    let cancelled = false;
    (async () => {
      try {
        // TODO: backend endpoint pending — see AuthController
        await authExtendedAPI.verifyEmail(token);
        if (!cancelled) setStatus('success');
      } catch {
        if (!cancelled) setStatus('error');
      }
    })();
    return () => { cancelled = true; };
  }, [token]);

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in">
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          {status === 'loading' && (
            <>
              <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto 16px' }} />
              <h1 className="auth-title">Verifying your email...</h1>
              <p className="auth-subtitle">This will only take a moment</p>
            </>
          )}

          {status === 'success' && (
            <>
              <div style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 56, height: 56, borderRadius: '50%', background: 'rgba(16,185,129,0.12)',
                marginBottom: 16,
              }}>
                <CheckCircle size={28} style={{ color: 'var(--green)' }} />
              </div>
              <h1 className="auth-title">Email verified!</h1>
              <p className="auth-subtitle" style={{ marginBottom: 24 }}>
                Your email has been verified successfully.
              </p>
              <Link to="/login" className="btn btn-primary" style={{ justifyContent: 'center', padding: '12px 24px' }}>
                Sign In
              </Link>
            </>
          )}

          {status === 'error' && (
            <>
              <div style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 56, height: 56, borderRadius: '50%', background: 'rgba(239,68,68,0.12)',
                marginBottom: 16,
              }}>
                <XCircle size={28} style={{ color: 'var(--red)' }} />
              </div>
              <h1 className="auth-title">Verification failed</h1>
              <p className="auth-subtitle" style={{ marginBottom: 24 }}>
                Verification failed or expired. Please request a new link.
              </p>
              <Link to="/login" className="btn btn-primary" style={{ justifyContent: 'center', padding: '12px 24px' }}>
                Back to Sign In
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
