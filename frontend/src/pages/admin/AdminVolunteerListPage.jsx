import React, { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Users, Shield, UserCheck, AlertCircle, CheckCircle, Search } from 'lucide-react';

export default function AdminVolunteerListPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [actionMsg, setActionMsg] = useState('');

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.get('/admin/users');
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch users list');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);


  const filtered = users.filter((u) => {
    const matchesSearch =
      u.email?.toLowerCase().includes(search.toLowerCase()) ||
      u.fullName?.toLowerCase().includes(search.toLowerCase());

    if (!matchesSearch) return false;
    if (roleFilter === 'ALL') return true;
    return u.role === roleFilter;
  });

  return (
    <div className="main-content">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.4rem' }}>User & Volunteer Directory</h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Overview of registered citizens, active volunteers, and administrators.
          </p>
        </div>
      </div>

      {actionMsg && (
        <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#34d399', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
          ✓ {actionMsg}
        </div>
      )}

      {error && (
        <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', color: '#f43f5e', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {/* Filter and Search */}
      <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {[
            { label: 'All Users', value: 'ALL' },
            { label: 'Volunteers', value: 'VOLUNTEER' },
            { label: 'Citizens', value: 'CITIZEN' },
            { label: 'Admins', value: 'ADMIN' }
          ].map((tab) => (
            <button
              key={tab.value}
              onClick={() => setRoleFilter(tab.value)}
              style={{
                padding: '0.45rem 0.85rem',
                borderRadius: 'var(--radius-sm)',
                border: roleFilter === tab.value ? '1px solid var(--primary)' : '1px solid var(--border-subtle)',
                background: roleFilter === tab.value ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                color: roleFilter === tab.value ? '#34d399' : 'var(--text-muted)',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer'
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '260px' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dim)' }} />
          <input
            type="text"
            placeholder="Search by name or email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '0.5rem 1rem 0.5rem 2.25rem',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(255, 255, 255, 0.04)',
              border: '1px solid var(--border-subtle)',
              color: 'var(--text-main)',
              fontSize: '0.88rem'
            }}
          />
        </div>
      </div>

      {loading && (
        <div style={{ textAlign: 'center', padding: '4rem 1rem', color: 'var(--text-muted)' }}>
          <div className="badge-dot" style={{ width: '16px', height: '16px', margin: '0 auto 1rem' }} />
          Loading user directory...
        </div>
      )}

      {!loading && (
        <div className="glass-panel" style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'rgba(255, 255, 255, 0.02)' }}>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>User Profile</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Role</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Phone</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Registered</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-dim)' }}>
                    No users matching criteria.
                  </td>
                </tr>
              ) : (
                filtered.map((u) => (
                  <tr key={u.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                    <td style={{ padding: '1rem' }}>
                      <div style={{ fontWeight: 700, color: 'var(--text-main)' }}>{u.fullName || 'No Name'}</div>
                      <div style={{ fontSize: '0.82rem', color: 'var(--text-dim)' }}>{u.email}</div>
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span
                        className="badge-port"
                        style={{
                          fontSize: '0.75rem',
                          color: u.role === 'ADMIN' ? '#38bdf8' : u.role === 'VOLUNTEER' ? '#34d399' : '#f59e0b',
                          borderColor: u.role === 'ADMIN' ? 'rgba(56, 189, 248, 0.3)' : u.role === 'VOLUNTEER' ? 'rgba(52, 211, 153, 0.3)' : 'rgba(245, 158, 11, 0.3)'
                        }}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td style={{ padding: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {u.phone || '—'}
                    </td>
                    <td style={{ padding: '1rem', fontSize: '0.82rem', color: 'var(--text-dim)' }}>
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Active'}
                    </td>
                    <td style={{ padding: '1rem', textAlign: 'right', fontSize: '0.82rem', color: 'var(--text-dim)' }}>
                      Verified Profile
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
