import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api, complaintsApi, volunteersApi, assignmentsApi, verificationsApi } from '../../api/client';
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
  Send,
  Info,
  FileCheck,
  Hash
} from 'lucide-react';

export default function AdminComplaintReviewPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [complaint, setComplaint] = useState(null);
  const [verifications, setVerifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState('');

  // Admin Verification Decision Modal state
  const [decisionModal, setDecisionModal] = useState({
    open: false,
    verificationId: null,
    decisionType: 'APPROVE',
    reason: '',
    error: ''
  });

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
      const vList = await verificationsApi.getComplaintVerifications(id).catch(() => []);
      setVerifications(Array.isArray(vList) ? vList : []);
    } catch (err) {
      setError(err.message || 'Failed to load complaint for review');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetails();
  }, [id]);

  const handleOpenDecisionModal = (verificationId, decisionType) => {
    setDecisionModal({
      open: true,
      verificationId,
      decisionType,
      reason: '',
      error: ''
    });
  };

  const handleSubmitDecision = async (e) => {
    e.preventDefault();
    if (!decisionModal.reason.trim()) {
      setDecisionModal(prev => ({ ...prev, error: 'A mandatory review reason is required.' }));
      return;
    }

    try {
      setActionLoading(true);
      setDecisionModal(prev => ({ ...prev, error: '' }));
      const updated = await verificationsApi.submitDecision(decisionModal.verificationId, {
        decision: decisionModal.decisionType,
        reason: decisionModal.reason.trim()
      });
      // Immediately update local state so action buttons hide instantly
      setVerifications(prev => prev.map(item => item.id === decisionModal.verificationId ? { ...item, ...updated } : item));
      setDecisionModal({ open: false, verificationId: null, decisionType: 'APPROVE', reason: '', error: '' });
      setFeedbackMsg(`Verification record successfully ${decisionModal.decisionType === 'APPROVE' ? 'APPROVED' : 'REJECTED'} by admin.`);
      await fetchDetails();
    } catch (err) {
      setDecisionModal(prev => ({ ...prev, error: err.message || 'Failed to submit verification decision.' }));
    } finally {
      setActionLoading(false);
    }
  };

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
              Step through verification, review cleanup proof, or manage volunteer dispatch.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
            {/* Pending Cleanup Proof Verification Case */}
            {verifications.find(v => (v.checkType === 'CLEANUP_RESOLUTION' || v.assignmentId) &&
              (v.reviewDecision === 'PENDING' || (!v.reviewDecision && v.verificationStatus === 'NEEDS_REVIEW')) &&
              v.reviewDecision !== 'APPROVED' && v.reviewDecision !== 'REJECTED' &&
              v.verificationStatus !== 'APPROVED_BY_ADMIN' && v.verificationStatus !== 'REJECTED_BY_ADMIN') && (
              <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', flexWrap: 'wrap' }}>
                <span style={{ fontSize: '0.82rem', color: '#fbbf24', display: 'flex', alignItems: 'center', gap: '0.35rem', fontWeight: 600 }}>
                  <AlertTriangle size={15} /> Cleanup Proof Pending Review
                </span>
                <button
                  onClick={() => handleOpenDecisionModal(verifications.find(v => (v.checkType === 'CLEANUP_RESOLUTION' || v.assignmentId) &&
                    (v.reviewDecision === 'PENDING' || (!v.reviewDecision && v.verificationStatus === 'NEEDS_REVIEW')) &&
                    v.reviewDecision !== 'APPROVED' && v.reviewDecision !== 'REJECTED' &&
                    v.verificationStatus !== 'APPROVED_BY_ADMIN' && v.verificationStatus !== 'REJECTED_BY_ADMIN').id, 'APPROVE')}
                  disabled={actionLoading}
                  className="service-btn"
                  style={{ background: 'rgba(16, 185, 129, 0.2)', borderColor: '#10b981', color: '#34d399', fontWeight: 700 }}
                >
                  <CheckCircle size={16} /> Approve Cleanup
                </button>
                <button
                  onClick={() => handleOpenDecisionModal(verifications.find(v => (v.checkType === 'CLEANUP_RESOLUTION' || v.assignmentId) &&
                    (v.reviewDecision === 'PENDING' || (!v.reviewDecision && v.verificationStatus === 'NEEDS_REVIEW')) &&
                    v.reviewDecision !== 'APPROVED' && v.reviewDecision !== 'REJECTED' &&
                    v.verificationStatus !== 'APPROVED_BY_ADMIN' && v.verificationStatus !== 'REJECTED_BY_ADMIN').id, 'REJECT')}
                  disabled={actionLoading}
                  className="service-btn"
                  style={{ background: 'rgba(244, 63, 94, 0.2)', borderColor: '#f43f5e', color: '#f43f5e', fontWeight: 700 }}
                >
                  <XCircle size={16} /> Reject Cleanup
                </button>
              </div>
            )}

            {/* Initial Citizen Report Under Review */}
            {complaint.status === 'UNDER_REVIEW' && !verifications.some(v => (v.checkType === 'CLEANUP_RESOLUTION' || v.assignmentId) && (v.verificationStatus === 'NEEDS_REVIEW' || v.reviewDecision === 'PENDING')) && (
              <button
                onClick={handleVerify}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', fontWeight: 700 }}
              >
                <CheckCircle size={16} /> Verify & Queue for Dispatch
              </button>
            )}

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

            {/* Dispatch Volunteer (only when VERIFIED and ready for dispatch) */}
            {complaint.status === 'VERIFIED' && (
              <button
                onClick={openAssignModal}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', fontWeight: 700 }}
              >
                <Send size={16} /> Dispatch Volunteer
              </button>
            )}

            {/* Reassign Volunteer (only when cleanup proof was explicitly rejected) */}
            {verifications.some(v => (v.checkType === 'CLEANUP_RESOLUTION' || v.assignmentId) && (v.verificationStatus === 'REJECTED' || v.reviewDecision === 'REJECTED')) && !['RESOLVED', 'REJECTED', 'CANCELLED'].includes(complaint.status) && (
              <button
                onClick={openAssignModal}
                disabled={actionLoading}
                className="service-btn"
                style={{ background: 'linear-gradient(135deg, #f59e0b, #fbbf24)', color: '#000', fontWeight: 700 }}
              >
                <Send size={16} /> Reassign Volunteer
              </button>
            )}

            {/* Active Task In Progress Notification */}
            {['ASSIGNED', 'ACCEPTED', 'IN_PROGRESS'].includes(complaint.status) &&
             !verifications.some(v => (v.checkType === 'CLEANUP_RESOLUTION' || v.assignmentId) && (v.verificationStatus === 'REJECTED' || v.reviewDecision === 'REJECTED' || v.verificationStatus === 'NEEDS_REVIEW' || v.reviewDecision === 'PENDING')) && (
              <span style={{ fontSize: '0.85rem', color: '#38bdf8', display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 600 }}>
                <Clock size={16} /> Cleanup in Progress by Assigned Volunteer
              </span>
            )}

            {complaint.status === 'RESOLVED' && (
              <span style={{ fontSize: '0.85rem', color: '#34d399', display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700 }}>
                <CheckCircle size={16} /> Completed & Resolved
              </span>
            )}

            {['SUBMITTED', 'UNDER_REVIEW'].includes(complaint.status) && !verifications.some(v => (v.checkType === 'CLEANUP_RESOLUTION' || v.assignmentId) && (v.verificationStatus === 'NEEDS_REVIEW' || v.reviewDecision === 'PENDING')) && (
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

        {/* GPS-Based Photo Verification & Integrity Console */}
        <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem', borderTop: '3px solid var(--accent-cyan)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <ShieldCheck size={24} color="var(--primary)" />
              <h3 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
                GPS Photo Verification & Integrity Console
              </h3>
            </div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)', background: 'rgba(255,255,255,0.05)', padding: '0.25rem 0.6rem', borderRadius: '4px' }}>
              Tolerance: 150m (Citizen) / 100m (Cleanup)
            </span>
          </div>

          <div style={{
            background: 'rgba(14, 165, 233, 0.08)',
            border: '1px solid rgba(14, 165, 233, 0.25)',
            borderRadius: 'var(--radius-sm)',
            padding: '0.85rem 1rem',
            marginBottom: '1.5rem',
            fontSize: '0.82rem',
            color: '#bae6fd',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.6rem',
            lineHeight: 1.5
          }}>
            <Info size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Untrusted Evidence Model:</strong> Device coordinates and timestamps are compared with embedded EXIF headers and server receipt time.
              Missing EXIF GPS is standard on modern mobile browsers due to OS privacy stripping and is <em>never</em> treated as fraud.
            </div>
          </div>

          {verifications.length === 0 ? (
            <div style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '1.5rem', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)' }}>
              No automated verification records registered for this submission.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {verifications.map((v, idx) => {
                const verifStatus = v.verificationStatus || v.status || 'VERIFIED';
                const distMeters = v.distanceMeters != null ? v.distanceMeters : v.calculatedDistanceMeters;
                const distTol = v.distanceToleranceMeters != null ? v.distanceToleranceMeters : 200;
                const isWithinTolerance = distMeters != null ? distMeters <= distTol : false;
                const auditNotes = v.adminNotes || v.decisionReason;
                const isDecided = v.reviewDecision === 'APPROVED' || v.reviewDecision === 'REJECTED' ||
                                  verifStatus === 'APPROVED_BY_ADMIN' || verifStatus === 'REJECTED_BY_ADMIN';
                const isPendingDecision = !isDecided && (v.reviewDecision === 'PENDING' || verifStatus === 'NEEDS_REVIEW');
                const flags = typeof v.checkReasons === 'string'
                  ? (() => { try { return JSON.parse(v.checkReasons); } catch(e) { return []; } })()
                  : (Array.isArray(v.checkReasons) ? v.checkReasons : (v.flags || []));

                return (
                  <div
                    key={v.id || idx}
                    style={{
                      background: 'rgba(15, 23, 42, 0.75)',
                      border: `1px solid ${isDecided ? 'var(--border-subtle)' : isPendingDecision ? 'rgba(245, 158, 11, 0.4)' : 'var(--border-subtle)'}`,
                      borderRadius: 'var(--radius-sm)',
                      padding: '1.25rem'
                    }}
                  >
                    {/* Record Header */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <FileCheck size={16} color="var(--accent-cyan)" />
                        <span style={{ fontWeight: 700, fontSize: '0.95rem' }}>
                          Evidence Record #{idx + 1} ({v.checkType || v.entityType || 'REPORT'})
                        </span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{
                          padding: '0.2rem 0.65rem',
                          borderRadius: '12px',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          background: (v.reviewDecision === 'APPROVED' || verifStatus === 'APPROVED_BY_ADMIN') ? 'rgba(16, 185, 129, 0.2)'
                            : (v.reviewDecision === 'REJECTED' || verifStatus === 'REJECTED_BY_ADMIN') ? 'rgba(244, 63, 94, 0.2)'
                            : isPendingDecision ? 'rgba(245, 158, 11, 0.2)'
                            : 'rgba(16, 185, 129, 0.2)',
                          color: (v.reviewDecision === 'APPROVED' || verifStatus === 'APPROVED_BY_ADMIN') ? '#34d399'
                            : (v.reviewDecision === 'REJECTED' || verifStatus === 'REJECTED_BY_ADMIN') ? '#f43f5e'
                            : isPendingDecision ? '#fbbf24'
                            : '#34d399',
                          border: `1px solid ${
                            (v.reviewDecision === 'APPROVED' || verifStatus === 'APPROVED_BY_ADMIN') ? 'rgba(16, 185, 129, 0.4)'
                            : (v.reviewDecision === 'REJECTED' || verifStatus === 'REJECTED_BY_ADMIN') ? 'rgba(244, 63, 94, 0.4)'
                            : isPendingDecision ? 'rgba(245, 158, 11, 0.4)'
                            : 'rgba(16, 185, 129, 0.4)'
                          }`
                        }}>
                          {v.reviewDecision === 'APPROVED' ? 'APPROVED' : v.reviewDecision === 'REJECTED' ? 'REJECTED' : verifStatus}
                        </span>
                      </div>
                    </div>

                    {/* Verification Data Grid */}
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
                      {/* Distance Check */}
                      <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.75rem', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.05)' }}>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                          Distance to Report Site
                        </div>
                        <div style={{ fontSize: '0.95rem', fontWeight: 700, color: distMeters != null ? (isWithinTolerance ? '#34d399' : '#f87171') : '#fbbf24' }}>
                          {distMeters != null ? `${distMeters.toFixed(1)} m` : 'N/A (No GPS Distance)'}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                          Tolerance: {distTol ? `${distTol.toFixed(0)}m` : '200m'}
                          {distMeters != null ? (isWithinTolerance ? ' (Within bounds ✓)' : ' (Exceeded ⚠️)') : ' (Pending review)'}
                        </div>
                      </div>

                      {/* Device GPS */}
                      <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.75rem', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.05)' }}>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                          Device Live GPS
                        </div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 600, fontFamily: 'monospace', color: 'var(--text-main)' }}>
                          {v.deviceLatitude != null ? `${Number(v.deviceLatitude).toFixed(5)}, ${Number(v.deviceLongitude).toFixed(5)}` : 'Not provided'}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                          {v.verifiedAt ? `Recorded: ${new Date(v.verifiedAt).toLocaleTimeString()}` : 'Live client coordinate'}
                        </div>
                      </div>

                      {/* EXIF Metadata */}
                      <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.75rem', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.05)' }}>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                          EXIF Embedded GPS
                        </div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 600, fontFamily: 'monospace', color: v.exifLatitude ? '#38bdf8' : 'var(--text-muted)' }}>
                          {v.exifLatitude != null ? `${Number(v.exifLatitude).toFixed(5)}, ${Number(v.exifLongitude).toFixed(5)}` : 'Stripped / Missing'}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                          {v.exifLatitude != null ? 'Camera Geotag' : 'No EXIF GPS tags'}
                        </div>
                      </div>

                      {/* Check Type & Confidence */}
                      <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.75rem', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.05)' }}>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                          Audit Type & Verifier
                        </div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-main)' }}>
                          {v.checkType || 'INITIAL_REPORT'}
                        </div>
                        <div style={{ fontSize: '0.72rem', fontWeight: 600, color: isDecided ? (v.reviewDecision === 'APPROVED' ? '#34d399' : '#f43f5e') : isPendingDecision ? '#fbbf24' : '#34d399', marginTop: '2px' }}>
                          {isDecided ? (v.reviewDecision === 'APPROVED' ? '✓ Approved by Admin' : '✗ Rejected by Admin') : isPendingDecision ? '⚠️ Pending Admin Decision' : '✓ Verified by ' + (v.verifierType || 'System')}
                        </div>
                      </div>
                    </div>

                    {/* Flags */}
                    {flags && flags.length > 0 && (
                      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1rem', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', fontWeight: 600 }}>Audit Flags:</span>
                        {flags.map((flag, fIdx) => (
                          <span
                            key={fIdx}
                            style={{
                              padding: '0.15rem 0.5rem',
                              borderRadius: '4px',
                              fontSize: '0.72rem',
                              fontWeight: 600,
                              background: flag.includes('EXIF_GPS_STRIPPED') || flag.includes('EXIF_GPS_MISSING') ? 'rgba(148, 163, 184, 0.15)' : flag.includes('MISMATCH') || flag.includes('DUPLICATE') ? 'rgba(244, 63, 94, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                              color: flag.includes('EXIF_GPS_STRIPPED') || flag.includes('EXIF_GPS_MISSING') ? '#94a3b8' : flag.includes('MISMATCH') || flag.includes('DUPLICATE') ? '#f87171' : '#34d399',
                              border: `1px solid ${flag.includes('EXIF_GPS_STRIPPED') || flag.includes('EXIF_GPS_MISSING') ? 'rgba(148, 163, 184, 0.3)' : flag.includes('MISMATCH') || flag.includes('DUPLICATE') ? 'rgba(244, 63, 94, 0.3)' : 'rgba(16, 185, 129, 0.3)'}`
                            }}
                          >
                            {flag}
                          </span>
                        ))}
                      </div>
                    )}

                    {/* Decision info or Actions */}
                    {isPendingDecision && (
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(245, 158, 11, 0.08)', padding: '0.75rem 1rem', borderRadius: '6px', border: '1px solid rgba(245, 158, 11, 0.25)', flexWrap: 'wrap', gap: '0.75rem' }}>
                        <div style={{ fontSize: '0.85rem', color: '#fbbf24', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <AlertTriangle size={16} />
                          <span>Requires Admin Decision ({v.checkType || 'Evidence'}). Submissions are preserved for audit review.</span>
                        </div>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                          <button
                            type="button"
                            onClick={() => handleOpenDecisionModal(v.id, 'APPROVE')}
                            disabled={actionLoading}
                            className="service-btn"
                            style={{ background: 'rgba(16, 185, 129, 0.2)', borderColor: '#10b981', color: '#34d399', padding: '0.4rem 0.85rem', fontSize: '0.82rem' }}
                          >
                            <CheckCircle size={14} /> Approve Photo
                          </button>
                          <button
                            type="button"
                            onClick={() => handleOpenDecisionModal(v.id, 'REJECT')}
                            disabled={actionLoading}
                            className="service-btn"
                            style={{ background: 'rgba(244, 63, 94, 0.2)', borderColor: '#f43f5e', color: '#f43f5e', padding: '0.4rem 0.85rem', fontSize: '0.82rem' }}
                          >
                            <XCircle size={14} /> Reject Photo
                          </button>
                        </div>
                      </div>
                    )}

                    {isDecided && (
                      <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.82rem', color: 'var(--text-muted)', border: '1px solid var(--border-subtle)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                          <span style={{ fontWeight: 700, color: (v.reviewDecision === 'APPROVED' || verifStatus === 'APPROVED_BY_ADMIN') ? '#34d399' : '#f43f5e' }}>
                            {(v.reviewDecision === 'APPROVED' || verifStatus === 'APPROVED_BY_ADMIN') ? '✓ Photo Approved by Admin' : '✗ Photo Rejected by Admin'}
                          </span>
                          {v.reviewedAt && (
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                              • {new Date(v.reviewedAt).toLocaleString()}
                            </span>
                          )}
                        </div>
                        <div>
                          <strong>Reviewer:</strong> {v.reviewedByName || v.reviewedByEmail || 'System Administrator'}
                          {auditNotes ? ` — Reason: "${auditNotes}"` : ''}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
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

        {/* Admin Verification Decision Modal */}
        {decisionModal.open && (
          <div style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.75)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 110,
            padding: '1rem'
          }}>
            <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: decisionModal.decisionType === 'APPROVE' ? '#34d399' : '#f43f5e' }}>
                {decisionModal.decisionType === 'APPROVE' ? 'Approve Verification' : 'Reject Verification'}
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '1.25rem' }}>
                Please specify a mandatory audit reason for this {decisionModal.decisionType.toLowerCase()} decision.
              </p>

              {decisionModal.error && (
                <div style={{ color: '#f43f5e', fontSize: '0.85rem', marginBottom: '1rem' }}>
                  {decisionModal.error}
                </div>
              )}

              <form onSubmit={handleSubmitDecision} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <textarea
                  required
                  rows={3}
                  placeholder={`Mandatory reason for ${decisionModal.decisionType.toLowerCase()}...`}
                  value={decisionModal.reason}
                  onChange={(e) => setDecisionModal(prev => ({ ...prev, reason: e.target.value }))}
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
                    onClick={() => setDecisionModal(prev => ({ ...prev, open: false }))}
                    className="service-btn"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={actionLoading}
                    className="service-btn"
                    style={{
                      background: decisionModal.decisionType === 'APPROVE' ? '#10b981' : '#f43f5e',
                      color: '#fff',
                      border: 'none',
                      fontWeight: 700
                    }}
                  >
                    {actionLoading ? 'Saving...' : 'Submit Decision'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

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
