import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api, complaintsApi, volunteersApi, assignmentsApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import MapView from '../../components/MapView';
import {
  ArrowLeft,
  CheckCircle,
  AlertTriangle,
  XCircle,
  Clock,
  MapPin,
  Calendar,
  AlertCircle,
  ShieldCheck,
  Send
} from 'lucide-react';

export default function AdminComplaintReviewPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState('');

  // Reject modal state
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [rejectError, setRejectError] = useState('');

  // Assign modal state
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [volunteersList, setVolunteersList] = useState([]);
  const [selectedVolunteerId, setSelectedVolunteerId] = useState('');
  const [assignNotes, setAssignNotes] = useState('');
  const [assignError, setAssignError] = useState('');
  const [loadingVolunteers, setLoadingVolunteers] = useState(false);

  const [lightboxImg, setLightboxImg] = useState(null);

  const fetchDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await complaintsApi.getById(id);
      setComplaint(data);
    } catch (err) {
      setError(err.message || 'Failed to load complaint for review');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetails();
  }, [id]);

  const handleMarkUnderReview = async () => {
    try {
      setActionLoading(true);
      setFeedbackMsg('');
      await complaintsApi.review(id, 'Admin opened investigation triage.');
      setFeedbackMsg('Report moved to UNDER_REVIEW.');
      fetchDetails();
    } catch (err) {
      setError(err.message || 'Action failed.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleVerify = async () => {
    try {
      setActionLoading(true);
      setFeedbackMsg('');
      await complaintsApi.verify(id, 'Site confirmed valid plastic accumulation.');
      setFeedbackMsg('Report successfully VERIFIED and queued for volunteer dispatch.');
      fetchDetails();
    } catch (err) {
      setError(err.message || 'Action failed.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async (e) => {
    e.preventDefault();
    if (!rejectionReason.trim()) {
      setRejectError('A mandatory rejection reason is required.');
      return;
    }

    try {
      setActionLoading(true);
      setRejectError('');
      await complaintsApi.reject(id, rejectionReason.trim());
      setShowRejectModal(false);
      setFeedbackMsg('Complaint marked as REJECTED with recorded reason.');
      fetchDetails();
    } catch (err) {
      setRejectError(err.message || 'Failed to reject complaint.');
    } finally {
      setActionLoading(false);
    }
  };

  const openAssignModal = async () => {
    setShowAssignModal(true);
    setAssignError('');
    setLoadingVolunteers(true);
    try {
      let list = await api.get('/volunteer/available').catch(() => []);
      if (!Array.isArray(list) || list.length === 0) {
        const users = await api.get('/admin/users').catch(() => []);
        if (Array.isArray(users)) {
          list = users
            .filter(u => u && u.role === 'VOLUNTEER')
            .map(u => ({
              userId: u.id,
              fullName: u.fullName,
              email: u.email,
              phone: u.phone,
              isAvailable: true
            }));
        } else {
          list = [];
        }
      }
      setVolunteersList(list);
      if (list.length > 0) {
        const firstId = list[0].userId || list[0].id;
        setSelectedVolunteerId(firstId);
      }
    } catch (err) {
      setAssignError(err.message || 'Failed to load volunteer roster.');
    } finally {
      setLoadingVolunteers(false);
    }
  };

  const handleAssign = async (e) => {
    e.preventDefault();
    if (!selectedVolunteerId) {
      setAssignError('Please select an available volunteer.');
      return;
    }

    try {
      setActionLoading(true);
      setAssignError('');
      await api.post('/assignments', {
        complaintId: complaint.id,
        volunteerId: selectedVolunteerId,
        notes: assignNotes.trim() || undefined
      });
      setShowAssignModal(false);
      setFeedbackMsg('Task successfully assigned to volunteer and moved to ASSIGNED!');
      fetchDetails();
    } catch (err) {
      setAssignError(err.message || 'Failed to dispatch assignment.');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '6rem 1rem' }}>
        <div className="badge-dot" style={{ width: '20px', height: '20px', margin: '0 auto 1.5rem' }} />
        <div style={{ color: 'var(--text-muted)' }}>Loading review console...</div>
      </div>
    );
  }

  if (error || !complaint) {
    return (
      <div className="main-content" style={{ maxWidth: '640px', margin: '3rem auto' }}>
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center' }}>
          <AlertCircle size={40} color="#f43f5e" style={{ margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.5rem' }}>Review Failed</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>{error || 'Complaint not found.'}</p>
          <button onClick={() => navigate('/admin/complaints')} className="service-btn">
            <ArrowLeft size={16} /> Back to Complaints
          </button>
        </div>
      </div>
    );
  }

  const mapCenter = [
    parseFloat(complaint.latitude) || 12.9716,
    parseFloat(complaint.longitude) || 77.5946
  ];

  const marker = {
    id: complaint.id,
    lat: mapCenter[0],
    lng: mapCenter[1],
    title: complaint.title,
    status: complaint.status
  };

  return (
    <div className="main-content">
      <div style={{ maxWidth: '1060px', margin: '0 auto' }}>
        {/* Header navigation */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
          <button onClick={() => navigate('/admin/complaints')} className="service-btn">
            <ArrowLeft size={16} /> Back to Complaints List
          </button>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-dim)' }}>Current Status:</span>
            <StatusBadge status={complaint.status} size="lg" />
          </div>
        </div>

        {feedbackMsg && (
          <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#34d399', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
            ✓ {feedbackMsg}
          </div>
        )}

        {/* Action Bar based on current state */}
        <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', borderLeft: '4px solid var(--primary)' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Administrative Triage Actions</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Step through verification or reject with audit trail accountability.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            {complaint.status === 'SUBMITTED' && (
              <button
                onClick={handleMarkUnderReview}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: 'rgba(251, 191, 36, 0.15)', borderColor: '#fbbf24', color: '#fbbf24' }}
              >
                <Clock size={16} /> Mark Under Review
              </button>
            )}

            {complaint.status === 'UNDER_REVIEW' && (
              <button
                onClick={handleVerify}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', fontWeight: 700 }}
              >
                <CheckCircle size={16} /> Verify & Queue for Dispatch
              </button>
            )}

            {['VERIFIED', 'ASSIGNED'].includes(complaint.status) && (
              <button
                onClick={openAssignModal}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', fontWeight: 700 }}
              >
                <Send size={16} /> {complaint.status === 'ASSIGNED' ? 'Reassign Volunteer' : 'Dispatch Volunteer'}
              </button>
            )}

            {['SUBMITTED', 'UNDER_REVIEW'].includes(complaint.status) && (
              <button
                onClick={() => setShowRejectModal(true)}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: 'rgba(244, 63, 94, 0.12)', borderColor: 'rgba(244, 63, 94, 0.4)', color: '#f43f5e' }}
              >
                <XCircle size={16} /> Reject Report
              </button>
            )}
          </div>
        </div>

        {/* Details Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '2rem', marginBottom: '2rem' }}>
          {/* Metadata Card */}
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: '0.75rem' }}>
              {complaint.title}
            </h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', fontSize: '0.88rem', color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Calendar size={15} />
                <span>Submitted on {new Date(complaint.createdAt).toLocaleString()}</span>
              </div>
              {complaint.addressText && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <MapPin size={15} color="var(--primary)" />
                  <span>{complaint.addressText}</span>
                </div>
              )}
            </div>

            <div style={{ marginBottom: '1.25rem' }}>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                Waste Classification & Pile Size
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <span className="badge-port">{complaint.wasteType}</span>
                <span className="badge-port" style={{ color: 'var(--accent-amber)', borderColor: 'rgba(245, 158, 11, 0.3)' }}>
                  {complaint.estimatedSize || 'MEDIUM_PILE'}
                </span>
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.35rem' }}>
                Citizen Description
              </div>
              <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '1rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)', lineHeight: 1.6 }}>
                {complaint.description}
              </div>
            </div>

            {complaint.rejectionReason && (
              <div style={{ marginTop: '1.5rem', background: 'rgba(244, 63, 94, 0.1)', border: '1px solid rgba(244, 63, 94, 0.35)', padding: '1rem', borderRadius: 'var(--radius-sm)', color: '#fb7185' }}>
                <strong>Rejection Reason:</strong> {complaint.rejectionReason}
              </div>
            )}
          </div>

          {/* Map View */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <MapPin size={18} color="var(--accent-cyan)" />
              Hotspot Coordinates
            </h3>
            <MapView
              height="260px"
              center={mapCenter}
              zoom={14}
              markers={[marker]}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.75rem', fontSize: '0.82rem', color: 'var(--text-dim)' }}>
              <span>Latitude: {complaint.latitude}</span>
              <span>Longitude: {complaint.longitude}</span>
            </div>
          </div>
        </div>

        {/* Proof Photos */}
        <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '1rem' }}>
            Proof Photos Inspection (Click to zoom)
          </h3>

          {complaint.images && complaint.images.length > 0 ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem' }}>
              {complaint.images.map((img) => (
                <div
                  key={img.id}
                  onClick={() => setLightboxImg(img.imageUrl)}
                  style={{
                    height: '160px',
                    borderRadius: 'var(--radius-sm)',
                    overflow: 'hidden',
                    border: '1px solid var(--border-subtle)',
                    cursor: 'zoom-in',
                    position: 'relative'
                  }}
                >
                  <img src={img.imageUrl} alt="Proof" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  <span style={{ position: 'absolute', bottom: '6px', left: '6px', background: 'rgba(0,0,0,0.7)', fontSize: '11px', padding: '2px 8px', borderRadius: '4px', color: '#fff' }}>
                    {img.imageType || 'INITIAL_PROOF'}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '2rem' }}>
              No photos attached to this report.
            </div>
          )}
        </div>

        {/* Reject Modal */}
        {showRejectModal && (
          <div style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.75)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            padding: '1rem'
          }}>
            <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: '#f43f5e' }}>
                Reject Complaint
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '1.25rem' }}>
                Please specify the reason (e.g. duplicate report, non-plastic waste, insufficient photo proof).
              </p>

              {rejectError && (
                <div style={{ color: '#f43f5e', fontSize: '0.85rem', marginBottom: '1rem' }}>
                  {rejectError}
                </div>
              )}

              <form onSubmit={handleReject} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <textarea
                  required
                  rows={3}
                  placeholder="Mandatory rejection reason..."
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    borderRadius: 'var(--radius-sm)',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--border-subtle)',
                    color: '#fff',
                    fontSize: '0.9rem'
                  }}
                />
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                  <button
                    type="button"
                    onClick={() => setShowRejectModal(false)}
                    className="service-btn"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={actionLoading}
                    className="service-btn"
                    style={{ background: '#f43f5e', color: '#fff', border: 'none', fontWeight: 700 }}
                  >
                    {actionLoading ? 'Rejecting...' : 'Confirm Rejection'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Dispatch Volunteer Assignment Modal */}
        {showAssignModal && (
          <div style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.75)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            padding: '1rem'
          }}>
            <div className="glass-panel" style={{ width: '100%', maxWidth: '480px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--primary)' }}>
                Dispatch Volunteer for Cleanup
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '1.25rem' }}>
                Select an active volunteer to mobilize for this verified waste site.
              </p>

              {assignError && (
                <div style={{ color: '#f43f5e', fontSize: '0.85rem', marginBottom: '1rem' }}>
                  {assignError}
                </div>
              )}

              {loadingVolunteers ? (
                <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                  Loading volunteer roster...
                </div>
              ) : (
                <form onSubmit={handleAssign} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                      Assignee Volunteer *
                    </label>
                    {volunteersList.length === 0 ? (
                      <div style={{ padding: '0.75rem', background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-sm)', color: 'var(--text-dim)', fontSize: '0.85rem' }}>
                        No volunteers currently registered.
                      </div>
                    ) : (
                      <select
                        value={selectedVolunteerId}
                        onChange={(e) => setSelectedVolunteerId(e.target.value)}
                        style={{
                          width: '100%',
                          padding: '0.75rem',
                          borderRadius: 'var(--radius-sm)',
                          background: '#0f172a',
                          border: '1px solid var(--border-subtle)',
                          color: '#fff',
                          fontSize: '0.9rem'
                        }}
                      >
                        {volunteersList.map(v => {
                          const vId = v.userId || v.id;
                          return (
                            <option key={vId} value={vId}>
                              {v.fullName || v.email} {v.phone ? `(${v.phone})` : ''} - {v.isAvailable ? '🟢 Online' : 'Standby'}
                            </option>
                          );
                        })}
                      </select>
                    )}
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                      Special Instructions / Notes
                    </label>
                    <textarea
                      rows={3}
                      placeholder="e.g. Bring heavy duty trash bags, safety gloves. Access through North Gate..."
                      value={assignNotes}
                      onChange={(e) => setAssignNotes(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '0.75rem',
                        borderRadius: 'var(--radius-sm)',
                        background: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid var(--border-subtle)',
                        color: '#fff',
                        fontSize: '0.9rem'
                      }}
                    />
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                    <button
                      type="button"
                      onClick={() => setShowAssignModal(false)}
                      className="service-btn"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={actionLoading || volunteersList.length === 0}
                      className="service-btn"
                      style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', border: 'none', fontWeight: 700 }}
                    >
                      {actionLoading ? 'Dispatching...' : 'Confirm Dispatch'}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        )}

        {/* Lightbox Modal */}
        {lightboxImg && (
          <div
            onClick={() => setLightboxImg(null)}
            style={{
              position: 'fixed',
              inset: 0,
              background: 'rgba(0,0,0,0.85)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 200,
              padding: '1.5rem',
              cursor: 'zoom-out'
            }}
          >
            <img src={lightboxImg} alt="Zoomed proof" style={{ maxWidth: '90vw', maxHeight: '85vh', borderRadius: '8px', objectFit: 'contain' }} />
          </div>
        )}
      </div>
    </div>
  );
}
