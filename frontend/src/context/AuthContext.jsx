import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user')); } catch { return null; }
  });
  const [token, setToken] = useState(() => localStorage.getItem('token'));

  const login = (authResponse) => {
    const u = { username: authResponse.username, role: authResponse.role };
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('refreshToken', authResponse.refreshToken || '');
    localStorage.setItem('user', JSON.stringify(u));
    setToken(authResponse.token);
    setUser(u);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const isAdmin = user?.role === 'ROLE_SUPER_ADMIN' || user?.role === 'ROLE_ORG_ADMIN' || user?.role === 'ROLE_EXAMINER';

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAdmin, isLoggedIn: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
