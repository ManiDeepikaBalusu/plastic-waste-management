import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api, complaintsApi, assignmentsApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import MapView from '../../components/MapView';
import InAppCameraCapture from '../../components/InAppCameraCapture';
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
  ArrowRight,
  ShieldCheck,
  AlertTriangle
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

  // Resolution photo upload & GPS state
  const [resolutionFile, setResolutionFile] = useState(null);
  const [resolutionPreview, setResolutionPreview] = useState(null);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [deviceMetadata, setDeviceMetadata] = useState(null);
  const [showCamera, setShowCamera] = useState(false);

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

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setDeviceMetadata({
            deviceLatitude: pos.coords.latitude,
            deviceLongitude: pos.coords.longitude,
            deviceCapturedAt: new Date(pos.timestamp || Date.now()).toISOString(),
            captureSource: 'GALLERY_UPLOAD'
          });
        },
        () => {
          setDeviceMetadata({ captureSource: 'GALLERY_UPLOAD' });
        },
        { enableHighAccuracy: true, timeout: 5000, maximumAge: 60000 }
      );
    } else {
      setDeviceMetadata({ captureSource: 'GALLERY_UPLOAD' });
    }
  };

  const handlePhotoCaptured = (captured) => {
    setResolutionFile(captured.file);
    setResolutionPreview(captured.previewUrl);
    setDeviceMetadata({
      deviceLatitude: captured.deviceLatitude,
      deviceLongitude: captured.deviceLongitude,
      deviceCapturedAt: captured.deviceCapturedAt,
      captureSource: captured.captureSource
    });
    setShowCamera(false);
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
      const updated = await assignmentsApi.resolveWithProof(
        id,
        resolutionNotes,
        [resolutionFile],
        deviceMetadata || { captureSource: 'GALLERY_UPLOAD' }
      );
      setTask(updated);
      setResolutionFile(null);
      setResolutionPreview(null);
      if (updated.verificationStatus === 'NEEDS_REVIEW') {
        setActionSuccess('Cleanup proof submitted! Your submission has been flagged for Administrator verification.');
      } else {
        setActionSuccess('🎉 Outstanding work! Cleanup resolved and GPS verification passed.');
      }
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

            {task?.verificationStatus === 'NEEDS_REVIEW' && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#fbbf24', fontWeight: 700 }}>
                <Clock size={20} /> Cleanup Proof Under Admin Review
              </div>
            )}

            {task?.verificationStatus === 'REJECTED_BY_ADMIN' && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#f43f5e', fontWeight: 700 }}>
                <XCircle size={20} /> Proof Rejected — Re-submit Valid Photo Below
              </div>
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
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--primary)', margin: 0 }}>
                  2. Resolution Proof Photo (Uploaded by Volunteer)
                </h4>
                {deviceMetadata?.deviceLatitude && (
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    padding: '0.15rem 0.5rem',
                    borderRadius: '12px',
                    background: 'rgba(16, 185, 129, 0.15)',
                    border: '1px solid rgba(16, 185, 129, 0.3)',
                    color: '#34d399',
                    fontSize: '0.72rem',
                    fontWeight: 600
                  }}>
                    <ShieldCheck size={12} />
                    GPS Attached: {Number(deviceMetadata.deviceLatitude).toFixed(4)}, {Number(deviceMetadata.deviceLongitude).toFixed(4)}
                  </div>
                )}
              </div>

              {task?.status === 'COMPLETED' || task?.verificationStatus === 'NEEDS_REVIEW' || task?.verificationStatus === 'APPROVED_BY_ADMIN' || (task?.verificationStatus !== 'REJECTED_BY_ADMIN' && task?.resolutionProofUrls && task.resolutionProofUrls.length > 0) ? (
                <div>
                  <div style={{ height: '220px', borderRadius: 'var(--radius-sm)', overflow: 'hidden', border: '1px solid rgba(16, 185, 129, 0.4)', background: '#020617', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    {task.resolutionProofUrls && task.resolutionProofUrls[0] ? (
                      <img src={task.resolutionProofUrls[0]} alt="Resolution Proof" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    ) : resolutionPreview ? (
                      <img src={resolutionPreview} alt="Resolution Proof" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
                        <Sparkles size={32} color="#10b981" />
                        <span style={{ fontWeight: 700, color: '#34d399' }}>Verified Site Cleared</span>
                      </div>
                    )}
                  </div>

                  {/* Verification Status Banner */}
                  <div style={{ marginTop: '0.75rem' }}>
                    {task.verificationStatus === 'NEEDS_REVIEW' && (
                      <div style={{ background: 'rgba(245, 158, 11, 0.12)', border: '1px solid rgba(245, 158, 11, 0.35)', color: '#fbbf24', padding: '0.75rem', borderRadius: 'var(--radius-sm)', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <AlertTriangle size={16} />
                        <div>
                          <strong>Awaiting Admin Review:</strong> {task.verificationNotes || 'Proof photo held for administrator location review.'}
                        </div>
                      </div>
                    )}
                    {task.verificationStatus === 'VERIFIED' && (
                      <div style={{ background: 'rgba(16, 185, 129, 0.12)', border: '1px solid rgba(16, 185, 129, 0.35)', color: '#34d399', padding: '0.75rem', borderRadius: 'var(--radius-sm)', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <ShieldCheck size={16} />
                        <div>
                          <strong>GPS Verified:</strong> Cleanup photo verified within site distance tolerance.
                        </div>
                      </div>
                    )}
                    {task.verificationStatus === 'APPROVED_BY_ADMIN' && (
                      <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#34d399', padding: '0.75rem', borderRadius: 'var(--radius-sm)', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <CheckCircle size={16} />
                        <div>
                          <strong>Admin Approved:</strong> {task.verificationNotes || 'Manually verified and confirmed by Administrator.'}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <form onSubmit={handleCompleteTask} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {task?.verificationStatus === 'REJECTED_BY_ADMIN' && (
                    <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', color: '#f43f5e', padding: '0.75rem', borderRadius: 'var(--radius-sm)', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <XCircle size={16} />
                      <div>
                        <strong>Verification Rejected:</strong> {task.verificationNotes || 'Administrator rejected verification proof.'} Please re-submit valid on-site photo.
                      </div>
                    </div>
                  )}
                  {resolutionPreview ? (
                    <div style={{ position: 'relative', height: '160px', borderRadius: 'var(--radius-sm)', overflow: 'hidden', border: '1px solid var(--border-subtle)' }}>
                      <img src={resolutionPreview} alt="After preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      <div style={{ position: 'absolute', bottom: '8px', right: '8px', display: 'flex', gap: '0.5rem' }}>
                        <button
                          type="button"
                          onClick={() => setShowCamera(true)}
                          style={{ background: 'rgba(0,0,0,0.75)', color: '#fff', border: 'none', padding: '4px 8px', borderRadius: '4px', fontSize: '11px', cursor: 'pointer' }}
                        >
                          Retake Camera
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                      <button
                        type="button"
                        onClick={() => setShowCamera(true)}
                        style={{
                          height: '140px',
                          borderRadius: 'var(--radius-sm)',
                          border: '1px solid rgba(14, 165, 233, 0.4)',
                          background: 'rgba(14, 165, 233, 0.08)',
                          color: '#38bdf8',
                          cursor: 'pointer',
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '0.5rem',
                          padding: '0.75rem',
                          textAlign: 'center',
                          fontWeight: 600,
                          fontSize: '0.85rem'
                        }}
                      >
                        <Camera size={26} />
                        <span>Live Camera Proof</span>
                        <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', fontWeight: 400 }}>Geotagged at site</span>
                      </button>

                      <div>
                        <input
                          type="file"
                          accept="image/jpeg,image/png,image/webp"
                          id="resolution-upload"
                          onChange={handleFileSelect}
                          style={{ display: 'none' }}
                        />
                        <label
                          htmlFor="resolution-upload"
                          style={{
                            height: '140px',
                            borderRadius: 'var(--radius-sm)',
                            border: '1px dashed rgba(16, 185, 129, 0.4)',
                            background: 'rgba(16, 185, 129, 0.04)',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: '0.5rem',
                            cursor: 'pointer',
                            color: '#34d399',
                            fontSize: '0.85rem',
                            padding: '0.75rem',
                            textAlign: 'center',
                            fontWeight: 600,
                            boxSizing: 'border-box'
                          }}
                        >
                          <Upload size={24} />
                          <span>Choose File</span>
                          <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', fontWeight: 400 }}>JPG, PNG, WEBP</span>
                        </label>
                      </div>
                    </div>
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

        {showCamera && (
          <InAppCameraCapture
            onPhotoCaptured={handlePhotoCaptured}
            onClose={() => setShowCamera(false)}
          />
        )}
      </div>
    </div>
  );
}
