import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api, complaintsApi, assignmentsApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import MapView from '../../components/MapView';
import {
  ArrowLeft,
  CheckCircle,
  XCircle,
  Clock,
  MapPin,
  Camera,
  Upload,
  AlertCircle,
  Sparkles,
  ArrowRight
} from 'lucide-react';

export default function VolunteerTaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [task, setTask] = useState(null);
  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionSuccess, setActionSuccess] = useState('');

  // Resolution photo upload state
  const [resolutionFile, setResolutionFile] = useState(null);
  const [resolutionPreview, setResolutionPreview] = useState(null);
  const [resolutionNotes, setResolutionNotes] = useState('');

  useEffect(() => {
    async function loadTaskDetails() {
      try {
        setLoading(true);
        setError(null);
        const taskRes = await assignmentsApi.getById(id);
        setTask(taskRes);
        if (taskRes.complaintId) {
          const compData = await complaintsApi.getById(taskRes.complaintId).catch(() => null);
          setComplaint(compData);
        }
      } catch (err) {
        setError(err.message || 'Failed to load task details');
      } finally {
        setLoading(false);
      }
    }
    loadTaskDetails();
  }, [id]);

  const handleAccept = async () => {
    try {
      setActionLoading(true);
      setActionSuccess('');
      const updated = await assignmentsApi.accept(id);
      setTask(updated);
      setActionSuccess('Task accepted! You can now mobilize to the cleanup site.');
    } catch (err) {
      setError(err.message || 'Failed to accept task');
    } finally {
      setActionLoading(false);
    }
  };

  const handleStart = async () => {
    try {
      setActionLoading(true);
      setActionSuccess('');
      const updated = await assignmentsApi.start(id);
      setTask(updated);
      setActionSuccess('Status updated to In Progress. Site cleanup underway.');
    } catch (err) {
      setError(err.message || 'Failed to start task');
    } finally {
      setActionLoading(false);
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 10 * 1024 * 1024) {
      setError('Proof photo must be under 10MB.');
      return;
    }

    setResolutionFile(file);
    setResolutionPreview(URL.createObjectURL(file));
  };

  const handleCompleteTask = async (e) => {
    e.preventDefault();
    if (!resolutionFile) {
      setError('Please attach an after-cleanup photo proving the site was cleared.');
      return;
    }

    try {
      setActionLoading(true);
      setError(null);
      const updated = await assignmentsApi.resolveWithProof(id, resolutionNotes, [resolutionFile]);
      setTask(updated);
      setActionSuccess('🎉 Outstanding work! Cleanup resolved and before/after verification recorded.');
      if (updated.complaintId) {
        const compData = await complaintsApi.getById(updated.complaintId).catch(() => null);
        setComplaint(compData);
      }
    } catch (err) {
      setError(err.message || 'Failed to submit resolution proof.');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '6rem 1rem' }}>
        <div className="badge-dot" style={{ width: '20px', height: '20px', margin: '0 auto 1.5rem' }} />
        <div style={{ color: 'var(--text-muted)' }}>Loading mission details...</div>
      </div>
    );
  }

  if (error && !complaint) {
    return (
      <div className="main-content" style={{ maxWidth: '640px', margin: '3rem auto' }}>
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center' }}>
          <AlertCircle size={40} color="#f43f5e" style={{ margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.5rem' }}>Task Not Found</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>{error}</p>
          <button onClick={() => navigate('/volunteer/tasks')} className="service-btn">
            <ArrowLeft size={16} /> Back to Tasks
          </button>
        </div>
      </div>
    );
  }

  const mapCenter = [
    parseFloat(complaint?.latitude) || 12.9716,
    parseFloat(complaint?.longitude) || 77.5946
  ];

  const marker = {
    id: complaint?.id || id,
    lat: mapCenter[0],
    lng: mapCenter[1],
    title: complaint?.title || 'Mission Site',
    status: task?.status || 'ASSIGNED'
  };

  return (
    <div className="main-content">
      <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
        {/* Navigation Bar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
          <button onClick={() => navigate('/volunteer/tasks')} className="service-btn">
            <ArrowLeft size={16} /> Back to My Tasks
          </button>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-dim)' }}>Task Status:</span>
            <StatusBadge status={task?.status} size="lg" />
          </div>
        </div>

        {actionSuccess && (
          <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#34d399', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
            ✓ {actionSuccess}
          </div>
        )}

        {error && (
          <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', color: '#f43f5e', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
            ⚠️ {error}
          </div>
        )}

        {/* Task Workflow Controls */}
        <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', borderLeft: '4px solid var(--accent-cyan)' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Volunteer Action Steps</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Progress the mission from dispatch to certified resolution.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            {task?.status === 'PENDING' && (
              <>
                <button
                  onClick={handleAccept}
                  disabled={actionLoading}
                  className="service-btn"
                  style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', fontWeight: 700 }}
                >
                  <CheckCircle size={16} /> Accept Task
                </button>
                <button
                  onClick={() => navigate('/volunteer/tasks')}
                  disabled={actionLoading}
                  className="service-btn"
                  style={{ background: 'rgba(244, 63, 94, 0.12)', borderColor: 'rgba(244, 63, 94, 0.4)', color: '#f43f5e' }}
                >
                  <XCircle size={16} /> Decline
                </button>
              </>
            )}

            {task?.status === 'ACCEPTED' && (
              <button
                onClick={handleStart}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: '#f59e0b', color: '#000', fontWeight: 700 }}
              >
                <Clock size={16} /> Start Cleanup (Mark In Progress)
              </button>
            )}

            {task?.status === 'COMPLETED' && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#34d399', fontWeight: 700 }}>
                <CheckCircle size={20} /> Mission Verified & Completed!
              </div>
            )}
          </div>
        </div>

        {/* Overview & Map Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '2rem', marginBottom: '2rem' }}>
          {/* Details Card */}
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: '0.5rem' }}>
              {complaint?.title}
            </h2>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '1.25rem' }}>
              <MapPin size={16} color="var(--primary)" />
              <span>{complaint?.addressText || 'Geocoded Coordinates'}</span>
            </div>

            <div style={{ marginBottom: '1.25rem' }}>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                Site Overview & Target Waste
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <span className="badge-port">{complaint?.wasteType || 'PLASTIC_MIXED'}</span>
                <span className="badge-port" style={{ color: 'var(--accent-amber)', borderColor: 'rgba(245, 158, 11, 0.3)' }}>
                  {complaint?.estimatedSize || 'MEDIUM_PILE'}
                </span>
              </div>
              <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '1rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                {complaint?.description}
              </div>
            </div>
          </div>

          {/* Map Location */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <MapPin size={18} color="var(--accent-cyan)" />
              Site Navigation
            </h3>
            <MapView
              height="260px"
              center={mapCenter}
              zoom={14}
              markers={[marker]}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.75rem', fontSize: '0.82rem', color: 'var(--text-dim)' }}>
              <span>Latitude: {complaint?.latitude}</span>
              <span>Longitude: {complaint?.longitude}</span>
            </div>
          </div>
        </div>

        {/* Before vs After Photo Proof Section */}
        <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem' }}>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Camera size={20} color="var(--primary)" />
            Before & After Verification Proof
          </h3>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '2rem' }}>
            {/* Before Photo */}
            <div>
              <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
                1. Initial Proof Photo (Reported by Citizen)
              </h4>
              <div style={{ height: '220px', borderRadius: 'var(--radius-sm)', overflow: 'hidden', border: '1px solid var(--border-subtle)', background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                {complaint?.images && complaint.images.length > 0 ? (
                  <img src={complaint.images[0].imageUrl} alt="Before" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                ) : (
                  <span style={{ color: 'var(--text-dim)', fontSize: '0.85rem' }}>No initial photo recorded</span>
                )}
              </div>
            </div>

            {/* After Photo Upload */}
            <div>
              <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '0.5rem' }}>
                2. Resolution Proof Photo (Uploaded by Volunteer)
              </h4>

              {task?.status === 'COMPLETED' ? (
                <div style={{ height: '220px', borderRadius: 'var(--radius-sm)', overflow: 'hidden', border: '1px solid rgba(16, 185, 129, 0.4)', background: 'rgba(16, 185, 129, 0.05)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                  <Sparkles size={32} color="#10b981" />
                  <span style={{ fontWeight: 700, color: '#34d399' }}>Verified Site Cleared</span>
                </div>
              ) : (
                <form onSubmit={handleCompleteTask} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    id="resolution-upload"
                    onChange={handleFileSelect}
                    style={{ display: 'none' }}
                  />

                  {resolutionPreview ? (
                    <div style={{ position: 'relative', height: '150px', borderRadius: 'var(--radius-sm)', overflow: 'hidden', border: '1px solid var(--border-subtle)' }}>
                      <img src={resolutionPreview} alt="After preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      <label htmlFor="resolution-upload" style={{ position: 'absolute', bottom: '8px', right: '8px', background: 'rgba(0,0,0,0.7)', color: '#fff', padding: '4px 8px', borderRadius: '4px', fontSize: '11px', cursor: 'pointer' }}>
                        Change Photo
                      </label>
                    </div>
                  ) : (
                    <label
                      htmlFor="resolution-upload"
                      style={{
                        height: '150px',
                        borderRadius: 'var(--radius-sm)',
                        border: '2px dashed rgba(16, 185, 129, 0.4)',
                        background: 'rgba(16, 185, 129, 0.05)',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '0.5rem',
                        cursor: 'pointer',
                        color: '#34d399',
                        fontSize: '0.88rem'
                      }}
                    >
                      <Upload size={24} />
                      <span>Attach After-Cleanup Photo</span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>JPG, PNG, WEBP (Max 10MB)</span>
                    </label>
                  )}

                  <input
                    type="text"
                    placeholder="Optional remarks (e.g., 3 bags of PET collected and sent for recycling)"
                    value={resolutionNotes}
                    onChange={(e) => setResolutionNotes(e.target.value)}
                    style={{
                      padding: '0.65rem 0.85rem',
                      borderRadius: 'var(--radius-sm)',
                      background: 'rgba(255, 255, 255, 0.04)',
                      border: '1px solid var(--border-subtle)',
                      color: 'var(--text-main)',
                      fontSize: '0.85rem'
                    }}
                  />

                  <button
                    type="submit"
                    disabled={actionLoading || !resolutionFile}
                    className="service-btn"
                    style={{
                      background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))',
                      color: '#000',
                      fontWeight: 800,
                      padding: '0.75rem',
                      fontSize: '0.92rem',
                      opacity: (!resolutionFile || actionLoading) ? 0.6 : 1,
                      cursor: (!resolutionFile || actionLoading) ? 'not-allowed' : 'pointer'
                    }}
                  >
                    {actionLoading ? 'Uploading Proof & Resolving...' : 'Upload Proof & Resolve Task'}
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
