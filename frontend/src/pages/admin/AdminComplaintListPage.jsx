import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { complaintsApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import { Search, Filter, Eye, AlertCircle, ArrowUpDown, ChevronLeft, ChevronRight } from 'lucide-react';

export default function AdminComplaintListPage() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 8;

  useEffect(() => {
    async function fetchList() {
      try {
        setLoading(true);
        setError(null);
        const data = await complaintsApi.getAll(statusFilter === 'ALL' ? null : statusFilter);
        setComplaints(Array.isArray(data) ? data : []);
        setCurrentPage(1);
      } catch (err) {
        setError(err.message || 'Failed to load complaints');
      } finally {
        setLoading(false);
      }
    }
    fetchList();
  }, [statusFilter]);

  const filtered = complaints.filter(c => {
    return (
      c.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.addressText?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.wasteType?.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  const totalPages = Math.ceil(filtered.length / pageSize) || 1;
  const paginated = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <div className="main-content">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.4rem' }}>Complaint Management</h1>
          <p style={{ color: 'var(--text-muted)' }}>Review and manage all citizen-submitted waste reports.</p>
        </div>
        <Link to="/admin/map" className="service-btn">
          View on Interactive Map
        </Link>
      </div>

      {/* Filter and Search Controls */}
      <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {[
            { label: 'All', value: 'ALL' },
            { label: 'Submitted', value: 'SUBMITTED' },
            { label: 'Under Review', value: 'UNDER_REVIEW' },
            { label: 'Verified', value: 'VERIFIED' },
            { label: 'Resolved', value: 'RESOLVED' },
            { label: 'Rejected', value: 'REJECTED' }
          ].map(tab => (
            <button
              key={tab.value}
              onClick={() => setStatusFilter(tab.value)}
              style={{
                padding: '0.45rem 0.85rem',
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
            placeholder="Search reports..."
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

      {loading && (
        <div style={{ textAlign: 'center', padding: '4rem 1rem', color: 'var(--text-muted)' }}>
          <div className="badge-dot" style={{ width: '16px', height: '16px', margin: '0 auto 1rem' }} />
          Loading reports list...
        </div>
      )}

      {error && (
        <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', color: '#f43f5e', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '2rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {/* Reports Table */}
      {!loading && !error && (
        <div className="glass-panel" style={{ overflowX: 'auto', marginBottom: '1.5rem' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'rgba(255, 255, 255, 0.02)' }}>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Title & Description</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Waste Type</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Location</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600 }}>Photos</th>
                <th style={{ padding: '1rem', color: 'var(--text-muted)', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginated.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-dim)' }}>
                    No matching reports found.
                  </td>
                </tr>
              ) : (
                paginated.map((row) => (
                  <tr key={row.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                    <td style={{ padding: '1rem', maxWidth: '280px' }}>
                      <div style={{ fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.2rem' }}>{row.title}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {row.description}
                      </div>
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span className="badge-port" style={{ fontSize: '0.75rem', padding: '2px 6px', borderRadius: '4px' }}>
                        {row.wasteType}
                      </span>
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <StatusBadge status={row.status} size="sm" />
                    </td>
                    <td style={{ padding: '1rem', fontSize: '0.82rem', color: 'var(--text-muted)', maxWidth: '200px' }}>
                      <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {row.addressText || `${row.latitude}, ${row.longitude}`}
                      </div>
                    </td>
                    <td style={{ padding: '1rem', fontSize: '0.85rem', color: 'var(--text-dim)' }}>
                      📸 {row.images?.length || 0}
                    </td>
                    <td style={{ padding: '1rem', textAlign: 'right' }}>
                      <Link
                        to={`/admin/complaints/${row.id}`}
                        className="service-btn"
                        style={{ fontSize: '0.8rem', padding: '0.35rem 0.75rem' }}
                      >
                        <Eye size={14} /> Review
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '1rem', marginTop: '1.5rem' }}>
          <button
            onClick={() => setCurrentPage(p => Math.max(p - 1, 1))}
            disabled={currentPage === 1}
            className="service-btn"
            style={{ padding: '0.4rem 0.8rem', opacity: currentPage === 1 ? 0.5 : 1 }}
          >
            <ChevronLeft size={16} /> Previous
          </button>
          <span style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
            Page {currentPage} of {totalPages}
          </span>
          <button
            onClick={() => setCurrentPage(p => Math.min(p + 1, totalPages))}
            disabled={currentPage === totalPages}
            className="service-btn"
            style={{ padding: '0.4rem 0.8rem', opacity: currentPage === totalPages ? 0.5 : 1 }}
          >
            Next <ChevronRight size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
