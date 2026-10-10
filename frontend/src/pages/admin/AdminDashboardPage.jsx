import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { complaintsApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import {
  ShieldAlert,
  CheckCircle,
  Clock,
  MapPin,
  Users,
  ArrowRight,
  TrendingUp,
  AlertTriangle,
  FileText
} from 'lucide-react';

export default function AdminDashboardPage() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        setError(null);
        const data = await complaintsApi.getAll();
        setComplaints(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.message || 'Failed to fetch complaints analytics');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const counts = {
    total: complaints.length,
    submitted: complaints.filter(c => c.status === 'SUBMITTED').length,
    underReview: complaints.filter(c => c.status === 'UNDER_REVIEW').length,
    verified: complaints.filter(c => c.status === 'VERIFIED').length,
    resolved: complaints.filter(c => c.status === 'RESOLVED').length,
    rejected: complaints.filter(c => c.status === 'REJECTED').length
  };

  const pendingTriage = complaints.filter(c => ['SUBMITTED', 'UNDER_REVIEW'].includes(c.status));

  return (
    <div className="main-content">
      <div style={{ marginBottom: '2.5rem' }}>
        <div className="badge">
          <span className="badge-dot" style={{ background: '#38bdf8', boxShadow: '0 0 8px #38bdf8' }} />
          Administrative Control Center
        </div>
        <h1 style={{ fontSize: '2.25rem', fontWeight: 800, marginBottom: '0.5rem' }}>
          Operations & Cleanup Dashboard
        </h1>
        <p style={{ color: 'var(--text-muted)' }}>
          Monitor community submissions, triage reports, and manage volunteer assignments.
        </p>
      </div>

      {error && (
        <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', color: '#f43f5e', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '2rem' }}>
          ⚠️ {error}
        </div>
      )}

      {/* KPI Status Metrics Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1.25rem', marginBottom: '2.5rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)' }}>Total Reports</span>
            <FileText size={18} color="var(--primary)" />
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800 }}>{counts.total}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Lifetime citizen reports</span>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', borderColor: 'rgba(56, 189, 248, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#38bdf8' }}>New / Submitted</span>
            <Clock size={18} color="#38bdf8" />
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: '#38bdf8' }}>{counts.submitted}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Awaiting review</span>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', borderColor: 'rgba(251, 191, 36, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#fbbf24' }}>Under Review</span>
            <AlertTriangle size={18} color="#fbbf24" />
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: '#fbbf24' }}>{counts.underReview}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>In triage process</span>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', borderColor: 'rgba(52, 211, 153, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#34d399' }}>Verified</span>
            <CheckCircle size={18} color="#34d399" />
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: '#34d399' }}>{counts.verified}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Ready for dispatch</span>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', borderColor: 'rgba(16, 185, 129, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#10b981' }}>Resolved</span>
            <TrendingUp size={18} color="#10b981" />
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: '#10b981' }}>{counts.resolved}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Cleaned & confirmed</span>
        </div>
      </div>

      {/* Quick Action Navigation Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem', marginBottom: '3rem' }}>
        <Link to="/admin/complaints" className="glass-panel" style={{ padding: '1.5rem', textDecoration: 'none', color: 'inherit', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.35rem' }}>Manage Complaints</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Filter, search and triage citizen submissions</p>
          </div>
          <ArrowRight size={20} color="var(--primary)" />
        </Link>

        <Link to="/admin/map" className="glass-panel" style={{ padding: '1.5rem', textDecoration: 'none', color: 'inherit', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.35rem' }}>Geospatial Waste Map</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Visualize hotspot clusters and cleanups</p>
          </div>
          <MapPin size={20} color="var(--accent-cyan)" />
        </Link>

        <Link to="/admin/volunteers" className="glass-panel" style={{ padding: '1.5rem', textDecoration: 'none', color: 'inherit', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.35rem' }}>Volunteer Dispatch</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Locate nearby volunteers and assign cleanups</p>
          </div>
          <Users size={20} color="var(--accent-amber)" />
        </Link>
      </div>

      {/* Triage Queue: Pending Reports */}
      <div className="glass-panel" style={{ padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: 700 }}>Priority Triage Queue</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>Submissions requiring verification or triage.</p>
          </div>
          <Link to="/admin/complaints" className="service-btn" style={{ fontSize: '0.85rem' }}>
            View All ({complaints.length})
          </Link>
        </div>

        {pendingTriage.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-muted)' }}>
            🎉 All submissions are reviewed! No pending complaints in queue.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {pendingTriage.slice(0, 6).map((item) => (
              <div
                key={item.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '1rem',
                  borderRadius: 'var(--radius-sm)',
                  background: 'rgba(255, 255, 255, 0.02)',
                  border: '1px solid var(--border-subtle)',
                  flexWrap: 'wrap',
                  gap: '1rem'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <StatusBadge status={item.status} size="sm" />
                  <div>
                    <h4 style={{ fontSize: '0.95rem', fontWeight: 700 }}>{item.title}</h4>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                      {item.addressText || `${item.latitude}, ${item.longitude}`}
                    </span>
                  </div>
                </div>

                <Link
                  to={`/admin/complaints/${item.id}`}
                  className="service-btn"
                  style={{ fontSize: '0.8rem', padding: '0.4rem 0.8rem' }}
                >
                  Review Photo & Triage <ArrowRight size={14} />
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
