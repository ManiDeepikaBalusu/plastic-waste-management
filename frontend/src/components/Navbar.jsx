import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Menu, X, LogOut, User, Shield, Compass, CheckCircle2 } from 'lucide-react';

export default function Navbar() {
  const { user, role, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    setMobileMenuOpen(false);
    navigate('/', { replace: true });
    await logout();
  };

  const getHomeTarget = () => {
    if (!user) return '/';
    if (role === 'ADMIN') return '/admin';
    if (role === 'VOLUNTEER') return '/volunteer';
    return '/citizen/complaints';
  };

  const isActive = (path) => location.pathname === path;

  return (
    <header className="header">
      <div className="header-container">
        {/* Brand */}
        <Link to={getHomeTarget()} style={{ textDecoration: 'none' }} className="brand">
          <div className="brand-icon">🌱</div>
          <div>
            <div className="brand-title">EcoClean Network</div>
            <div className="brand-subtitle">Plastic Waste & Volunteer Dispatch</div>
          </div>
        </Link>

        {/* Desktop Nav Links */}
        <nav style={{ display: 'none', alignItems: 'center', gap: '1.25rem' }} className="desktop-nav">
          {!user && (
            <Link
              to="/"
              className={`nav-link ${isActive('/') ? 'active' : ''}`}
            >
              Home
            </Link>
          )}

          {/* Citizen Links: visible only to authenticated CITIZEN */}
          {user && role === 'CITIZEN' && (
            <>
              <Link
                to="/citizen/report"
                className={`nav-link ${isActive('/citizen/report') ? 'active' : ''}`}
              >
                Report Waste
              </Link>
              <Link
                to="/citizen/complaints"
                className={`nav-link ${isActive('/citizen/complaints') ? 'active' : ''}`}
              >
                My Complaints
              </Link>
            </>
          )}

          {/* Admin Links: visible only to authenticated ADMIN */}
          {user && role === 'ADMIN' && (
            <>
              <Link
                to="/admin"
                className={`nav-link ${isActive('/admin') ? 'active' : ''}`}
              >
                Dashboard
              </Link>
              <Link
                to="/admin/complaints"
                className={`nav-link ${isActive('/admin/complaints') ? 'active' : ''}`}
              >
                Complaints
              </Link>
              <Link
                to="/admin/map"
                className={`nav-link ${isActive('/admin/map') ? 'active' : ''}`}
              >
                Waste Map
              </Link>
              <Link
                to="/admin/volunteers"
                className={`nav-link ${isActive('/admin/volunteers') ? 'active' : ''}`}
              >
                Volunteers
              </Link>
            </>
          )}

          {/* Volunteer Links: visible only to authenticated VOLUNTEER */}
          {user && role === 'VOLUNTEER' && (
            <>
              <Link
                to="/volunteer"
                className={`nav-link ${isActive('/volunteer') ? 'active' : ''}`}
              >
                Workstation
              </Link>
              <Link
                to="/volunteer/tasks"
                className={`nav-link ${isActive('/volunteer/tasks') ? 'active' : ''}`}
              >
                Assigned Tasks
              </Link>
            </>
          )}

          {/* User Auth Info / Controls */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginLeft: '1rem' }}>
            {user ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div className="user-pill">
                  <User size={14} />
                  <span>{user.fullName || user.email}</span>
                  <span className="role-tag">{user.role}</span>
                </div>
                <button
                  onClick={handleLogout}
                  className="btn-icon"
                  title="Sign Out"
                >
                  <LogOut size={16} />
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Link to="/login" className="btn-secondary-sm">
                  Sign In
                </Link>
                <Link to="/register" className="btn-primary-sm">
                  Register
                </Link>
              </div>
            )}
          </div>
        </nav>

        {/* Mobile menu trigger */}
        <button
          className="mobile-trigger"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label="Toggle Navigation"
        >
          {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="mobile-drawer glass-panel">
          {!user && (
            <Link to="/" onClick={() => setMobileMenuOpen(false)}>Home</Link>
          )}
          {user && role === 'CITIZEN' && (
            <>
              <Link to="/citizen/report" onClick={() => setMobileMenuOpen(false)}>Report Waste</Link>
              <Link to="/citizen/complaints" onClick={() => setMobileMenuOpen(false)}>My Complaints</Link>
            </>
          )}
          {user && role === 'ADMIN' && (
            <>
              <Link to="/admin" onClick={() => setMobileMenuOpen(false)}>Admin Dashboard</Link>
              <Link to="/admin/complaints" onClick={() => setMobileMenuOpen(false)}>Manage Complaints</Link>
              <Link to="/admin/map" onClick={() => setMobileMenuOpen(false)}>Interactive Map</Link>
              <Link to="/admin/volunteers" onClick={() => setMobileMenuOpen(false)}>Volunteer Dispatch</Link>
            </>
          )}
          {user && role === 'VOLUNTEER' && (
            <>
              <Link to="/volunteer" onClick={() => setMobileMenuOpen(false)}>Volunteer Dashboard</Link>
              <Link to="/volunteer/tasks" onClick={() => setMobileMenuOpen(false)}>Assigned Cleanups</Link>
            </>
          )}
          <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '0.75rem', marginTop: '0.5rem' }}>
            {user ? (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>{user.email}</span>
                <button onClick={handleLogout} className="btn-secondary-sm">Sign Out</button>
              </div>
            ) : (
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Link to="/login" className="btn-secondary-sm" style={{ flex: 1, textAlign: 'center' }} onClick={() => setMobileMenuOpen(false)}>Sign In</Link>
                <Link to="/register" className="btn-primary-sm" style={{ flex: 1, textAlign: 'center' }} onClick={() => setMobileMenuOpen(false)}>Register</Link>
              </div>
            )}
          </div>
        </div>
      )}

      <style>{`
        .desktop-nav {
          display: flex !important;
        }
        .nav-link {
          color: var(--text-muted);
          text-decoration: none;
          font-weight: 600;
          font-size: 0.9rem;
          padding: 0.4rem 0.75rem;
          border-radius: var(--radius-sm);
          transition: all 0.2s ease;
        }
        .nav-link:hover, .nav-link.active {
          color: var(--text-main);
          background: rgba(255, 255, 255, 0.05);
        }
        .nav-link.active {
          color: #34d399;
          border-bottom: 2px solid var(--primary);
        }
        .user-pill {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          background: rgba(255, 255, 255, 0.04);
          border: 1px solid var(--border-subtle);
          padding: 0.35rem 0.75rem;
          border-radius: var(--radius-full);
          font-size: 0.85rem;
        }
        .role-tag {
          font-size: 0.7rem;
          font-weight: 700;
          padding: 0.1rem 0.4rem;
          border-radius: var(--radius-full);
          background: rgba(16, 185, 129, 0.2);
          color: #10b981;
          letter-spacing: 0.05em;
        }
        .btn-icon {
          background: rgba(255, 255, 255, 0.05);
          border: 1px solid var(--border-subtle);
          color: var(--text-muted);
          padding: 0.45rem;
          border-radius: var(--radius-sm);
          cursor: pointer;
          transition: all 0.2s;
        }
        .btn-icon:hover {
          color: #f43f5e;
          border-color: rgba(244, 63, 94, 0.4);
          background: rgba(244, 63, 94, 0.1);
        }
        .btn-primary-sm {
          background: linear-gradient(135deg, var(--primary), var(--accent-cyan));
          color: #000;
          text-decoration: none;
          padding: 0.45rem 1rem;
          border-radius: var(--radius-sm);
          font-weight: 700;
          font-size: 0.85rem;
          box-shadow: 0 0 10px var(--primary-glow);
          transition: all 0.2s;
        }
        .btn-primary-sm:hover {
          filter: brightness(1.1);
          transform: translateY(-1px);
        }
        .btn-secondary-sm {
          background: rgba(255, 255, 255, 0.05);
          border: 1px solid var(--border-subtle);
          color: var(--text-main);
          text-decoration: none;
          padding: 0.45rem 0.9rem;
          border-radius: var(--radius-sm);
          font-weight: 600;
          font-size: 0.85rem;
          transition: all 0.2s;
        }
        .btn-secondary-sm:hover {
          background: rgba(255, 255, 255, 0.1);
        }
        .mobile-trigger {
          display: none;
          background: transparent;
          border: none;
          color: var(--text-main);
          cursor: pointer;
        }
        .mobile-drawer {
          display: none;
          flex-direction: column;
          gap: 0.75rem;
          padding: 1rem;
          margin-top: 1rem;
        }
        .mobile-drawer a {
          color: var(--text-main);
          text-decoration: none;
          font-weight: 600;
          padding: 0.4rem 0;
        }
        @media (max-width: 860px) {
          .desktop-nav {
            display: none !important;
          }
          .mobile-trigger {
            display: block;
          }
          .mobile-drawer {
            display: flex;
          }
        }
      `}</style>
    </header>
  );
}
