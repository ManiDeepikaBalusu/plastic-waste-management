import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { complaintsApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import { AlertCircle, Calendar, MapPin, Plus, Search, Eye, Camera } from 'lucide-react';

export default function MyComplaintsPage() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    async function fetchComplaints() {
      try {
        setLoading(true);
        setError(null);
        const data = await complaintsApi.getMy();
        setComplaints(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.message || 'Failed to load your complaints');
      } finally {
        setLoading(false);
      }
    }
    fetchComplaints();
  }, []);

  const filtered = complaints.filter((c) => {
    const matchesSearch =
      c.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.addressText?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.wasteType?.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;

    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'ACTIVE') {
      return ['SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS'].includes(c.status);
    }
    if (statusFilter === 'RESOLVED') return c.status === 'RESOLVED';
    if (statusFilter === 'CLOSED') return ['REJECTED', 'CANCELLED'].includes(c.status);

    return c.status === statusFilter;
  });

  return (
    <div className="main-content">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.4rem' }}>My Reported Complaints</h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Track the live progress of plastic waste sites you reported.
          </p>
        </div>
        <Link
          to="/citizen/report"
          className="service-btn"
          style={{
            background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))',
            color: '#000',
            fontWeight: 800
          }}
        >
          <Plus size={18} />
          Report New Waste
        </Link>
      </div>

      {/* Filter and Search Bar */}
      <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {[
            { label: 'All', value: 'ALL' },
            { label: 'Active', value: 'ACTIVE' },
            { label: 'Resolved', value: 'RESOLVED' },
            { label: 'Closed', value: 'CLOSED' }
          ].map(tab => (
            <button
              key={tab.value}
              onClick={() => setStatusFilter(tab.value)}
              style={{
                padding: '0.45rem 1rem',
                borderRadius: 'var(--radius-sm)',
                border: statusFilter === tab.value ? '1px solid var(--primary)' : '1px solid var(--border-subtle)',
                background: statusFilter === tab.value ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                color: statusFilter === tab.value ? '#34d399' : 'var(--text-muted)',
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
            placeholder="Search by title or area..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
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

      {/* Loading State */}
      {loading && (
        <div style={{ textAlign: 'center', padding: '4rem 1rem', color: 'var(--text-muted)' }}>
          <div className="badge-dot" style={{ width: '16px', height: '16px', margin: '0 auto 1rem' }} />
          Loading your complaints...
        </div>
      )}

      {/* Error State */}
      {error && (
        <div style={{
          background: 'rgba(244, 63, 94, 0.12)',
          border: '1px solid rgba(244, 63, 94, 0.35)',
          color: '#f43f5e',
          padding: '1.25rem',
          borderRadius: 'var(--radius-sm)',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem',
          marginBottom: '2rem'
        }}>
          <AlertCircle size={24} />
          <div>
            <strong>Error loading complaints:</strong> {error}
          </div>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && filtered.length === 0 && (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📦</div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.5rem' }}>No Reports Found</h2>
          <p style={{ color: 'var(--text-muted)', maxWidth: '440px', margin: '0 auto 1.5rem', fontSize: '0.95rem' }}>
            {complaints.length === 0
              ? "You haven't filed any plastic waste reports yet. Notice an accumulation in your neighborhood?"
              : "No reports match your current filter criteria."}
          </p>
          <Link to="/citizen/report" className="service-btn" style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', fontWeight: 700 }}>
            <Plus size={16} /> File First Report
          </Link>
        </div>
      )}

      {/* Complaints Grid */}
      {!loading && !error && filtered.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
          {filtered.map((item) => {
            const firstImg = item.images && item.images.length > 0 ? item.images[0].imageUrl : null;
            const formattedDate = item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Recent';

            return (
              <div key={item.id} className="glass-panel" style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                {/* Photo Header */}
                <div style={{ height: '180px', width: '100%', background: 'rgba(0,0,0,0.4)', position: 'relative' }}>
                  {firstImg ? (
                    <img src={firstImg} alt={item.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-dim)', flexDirection: 'column', gap: '0.5rem' }}>
                      <Camera size={32} />
                      <span style={{ fontSize: '0.8rem' }}>No Photo Attached</span>
                    </div>
                  )}
                  <div style={{ position: 'absolute', top: '12px', right: '12px' }}>
                    <StatusBadge status={item.status} size="sm" />
                  </div>
                  {item.images && item.images.length > 1 && (
                    <div style={{ position: 'absolute', bottom: '10px', right: '10px', background: 'rgba(0,0,0,0.7)', padding: '2px 8px', borderRadius: '4px', fontSize: '11px', color: '#fff' }}>
                      +{item.images.length - 1} photos
                    </div>
                  )}
                </div>

                {/* Card Content */}
                <div style={{ padding: '1.25rem', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.5rem', lineHeight: 1.3 }}>
                      {item.title}
                    </h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '1rem', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {item.description}
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-dim)', marginBottom: '1.25rem' }}>
                      {item.addressText && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <MapPin size={14} color="var(--primary)" />
                          <span style={{ textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>{item.addressText}</span>
                        </div>
                      )}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Calendar size={14} />
                        <span>Filed on {formattedDate}</span>
                      </div>
                    </div>
                  </div>

                  <Link
                    to={`/citizen/complaints/${item.id}`}
                    className="service-btn"
                    style={{ width: '100%', justifyContent: 'center', marginTop: 'auto' }}
                  >
                    <Eye size={16} />
                    View Details & Audit Trail
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
