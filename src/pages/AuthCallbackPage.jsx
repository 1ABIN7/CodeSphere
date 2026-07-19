import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');
    const username = searchParams.get('username');
    const role = searchParams.get('role');

    if (token) {
      localStorage.setItem('token', token);
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
      if (username && role) {
        localStorage.setItem('user', JSON.stringify({ username, role }));
      }
      navigate('/dashboard', { replace: true });
    } else {
      navigate('/login', { replace: true });
    }
  }, [searchParams, navigate]);

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in">
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto 16px' }} />
          <h1 className="auth-title">Signing you in...</h1>
          <p className="auth-subtitle">Please wait while we complete your login</p>
        </div>
      </div>
    </div>
  );
}
