import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    const token = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');
    const username = searchParams.get('username');
    const role = searchParams.get('role');

    if (!token || !username || !role) {
      navigate('/login', { replace: true });
      return;
    }

    login({ token, refreshToken, username, role });
    navigate('/dashboard', { replace: true });
  }, [login, navigate, searchParams]);

  return (
    <div className="auth-page">
      <div className="card auth-card fade-in">
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto 16px' }} />
          <h1 className="auth-title">Signing you in...</h1>
          <p className="auth-subtitle">Please wait while we complete your Google login.</p>
        </div>
      </div>
    </div>
  );
}
