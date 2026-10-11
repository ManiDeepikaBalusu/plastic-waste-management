import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const token = localStorage.getItem('token');
      const savedUser = localStorage.getItem('user');
      return token && savedUser ? JSON.parse(savedUser) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(() => {
    const token = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    return !!token && !savedUser;
  });
  const [error, setError] = useState(null);

  const checkAuth = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }

    try {
      setError(null);
      const profile = await authApi.getMe();
      if (profile && profile.id) {
        setUser(profile);
        localStorage.setItem('user', JSON.stringify(profile));
      } else {
        throw new Error('Invalid profile session');
      }
    } catch {
      // Not logged in or expired token
      localStorage.removeItem('token');
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user');
      sessionStorage.clear();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const login = async (email, password) => {
    try {
      setError(null);
      const res = await authApi.login({ email, password });
      if (res.token) {
        localStorage.setItem('token', res.token);
        localStorage.setItem('jwt_token', res.token);
      }
      if (res.user) {
        localStorage.setItem('user', JSON.stringify(res.user));
        setUser(res.user);
      }
      return res.user;
    } catch (err) {
      setError(err.message || 'Login failed');
      throw err;
    }
  };

  const register = async (payload) => {
    try {
      setError(null);
      const res = await authApi.register(payload);
      return res;
    } catch (err) {
      setError(err.message || 'Registration failed');
      throw err;
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (err) {
      console.warn('Logout endpoint error:', err);
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user');
      sessionStorage.clear();
      setUser(null);
      setError(null);
    }
  };

  const value = {
    user,
    role: user?.role ? user.role.toUpperCase() : null,
    isAuthenticated: !!user,
    loading,
    error,
    login,
    register,
    logout,
    checkAuth
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
