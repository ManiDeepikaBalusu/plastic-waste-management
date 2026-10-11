import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, complaintsApi, assignmentsApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import { MapPin, Calendar, Eye, AlertCircle, ArrowRight, CheckCircle2 } from 'lucide-react';

export default function VolunteerTaskListPage() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');

  useEffect(() => {
    async function loadTasks() {
      try {
        setLoading(true);
        setError(null);
        const data = await assignmentsApi.getMyTasks();
        setTasks(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.message || 'Failed to fetch assigned tasks');
      } finally {
        setLoading(false);
      }
    }
    loadTasks();
  }, []);

  const filtered = tasks.filter(t => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'ACTIVE') return ['PENDING', 'ACCEPTED', 'IN_PROGRESS'].includes(t.status);
    if (statusFilter === 'RESOLVED') return ['COMPLETED', 'RESOLVED'].includes(t.status);
    return t.status === statusFilter;
  });

  return (
    <div className="main-content">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.4rem' }}>Assigned Cleanup Tasks</h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Review plastic waste dispatches assigned to your volunteer profile.
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '2rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        {[
          { label: 'All Tasks', value: 'ALL' },
          { label: 'Active Missions', value: 'ACTIVE' },
          { label: 'Pending Acceptance', value: 'PENDING' },
          { label: 'In Progress', value: 'IN_PROGRESS' },
          { label: 'Completed', value: 'RESOLVED' }
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

      {loading && (
        <div style={{ textAlign: 'center', padding: '4rem 1rem', color: 'var(--text-muted)' }}>
          <div className="badge-dot" style={{ width: '16px', height: '16px', margin: '0 auto 1rem' }} />
          Loading assigned tasks...
        </div>
      )}

      {error && (
        <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', color: '#f43f5e', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '2rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {!loading && filtered.length === 0 && (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🎉</div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.5rem' }}>No Tasks in this Filter</h2>
          <p style={{ color: 'var(--text-muted)', maxWidth: '440px', margin: '0 auto 1.5rem', fontSize: '0.95rem' }}>
            There are currently no cleanup tasks matching your selection.
          </p>
        </div>
      )}

      {!loading && filtered.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
          {filtered.map((task) => {
            const comp = task.complaint || {};
            const img = comp.images?.find((i) => i.imageType === 'INITIAL_PROOF')?.imageUrl
              || (task.initialProofUrls && task.initialProofUrls.length > 0 ? task.initialProofUrls[0] : null)
              || (comp.images && comp.images.length > 0 ? comp.images[0].imageUrl : null);

            return (
              <div key={task.id} className="glass-panel" style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                <div style={{ height: '160px', width: '100%', background: 'rgba(0,0,0,0.5)', position: 'relative' }}>
                  {img ? (
                    <img src={img} alt="Waste preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-dim)', fontSize: '0.85rem' }}>
                      📸 Photos Available in Task
                    </div>
                  )}
                  <div style={{ position: 'absolute', top: '12px', right: '12px' }}>
                    <StatusBadge status={task.status} size="sm" />
                  </div>
                </div>

                <div style={{ padding: '1.25rem', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.4rem' }}>
                      {comp.title || 'Plastic Waste Mission'}
                    </h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '1rem', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {comp.description || 'Details specified on assignment.'}
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-dim)', marginBottom: '1.25rem' }}>
                      {comp.addressText && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <MapPin size={14} color="var(--primary)" />
                          <span>{comp.addressText}</span>
                        </div>
                      )}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Calendar size={14} />
                        <span>Assigned {task.assignedAt ? new Date(task.assignedAt).toLocaleDateString() : 'Recently'}</span>
                      </div>
                    </div>
                  </div>

                  <Link
                    to={`/volunteer/tasks/${task.id}`}
                    className="service-btn"
                    style={{ width: '100%', justifyContent: 'center', marginTop: 'auto' }}
                  >
                    View Mission & Update Progress <ArrowRight size={16} />
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
