import { createContext, useContext, useMemo, useState, useCallback } from 'react';
import * as authApi from '../api/auth';
import { logout as logoutApi } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('ibns_token'));
  const [role, setRole] = useState(() => localStorage.getItem('ibns_role'));
  const [email, setEmail] = useState(() => localStorage.getItem('ibns_email'));

  const applySession = useCallback((authResponse) => {
    const { token: t, role: r, email: e } = authResponse;
    localStorage.setItem('ibns_token', t);
    localStorage.setItem('ibns_role', r);
    localStorage.setItem('ibns_email', e);
    setToken(t);
    setRole(r);
    setEmail(e);
  }, []);

  const clearSession = useCallback(() => {
    localStorage.removeItem('ibns_token');
    localStorage.removeItem('ibns_role');
    localStorage.removeItem('ibns_email');
    setToken(null);
    setRole(null);
    setEmail(null);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await logoutApi();
    } catch {
      // even if the API call fails, clear the local session
    }
    clearSession();
  }, [clearSession]);

  const value = useMemo(
    () => ({
      token,
      role,
      email,
      isAuthenticated: Boolean(token),
      applySession,
      clearSession,
      signOut,
      ...authApi,
    }),
    [token, role, email, applySession, clearSession, signOut]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
