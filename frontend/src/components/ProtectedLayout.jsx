import React from 'react';
import { Navigate, Outlet, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert } from 'lucide-react';

export default function ProtectedLayout({ allowedRoles = [] }) {
  const { user, loading, role } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', gap: '1rem' }}>
        <div className="badge-dot" style={{ width: '20px', height: '20px' }}></div>
        <div style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>Authenticating user session...</div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(role)) {
    const defaultDashboard = role === 'ADMIN' ? '/admin' : role === 'VOLUNTEER' ? '/volunteer/tasks' : '/citizen/complaints';

    return (
      <div style={{ maxWidth: '600px', margin: '4rem auto', padding: '2rem' }} className="glass-panel">
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', color: '#f43f5e', marginBottom: '1rem' }}>
          <ShieldAlert size={32} />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Access Restricted</h2>
        </div>
        <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
          Your current account role is <strong style={{ color: 'var(--primary)' }}>{role}</strong>, but this page requires one of: {allowedRoles.join(', ')}.
        </p>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Link to={defaultDashboard} className="service-btn">
            Go to My Dashboard
          </Link>
          <Link to="/" className="service-btn" style={{ background: 'rgba(255, 255, 255, 0.05)', borderColor: 'var(--border-subtle)' }}>
            Go to Home Page
          </Link>
        </div>
      </div>
    );
  }

  return <Outlet />;
}
