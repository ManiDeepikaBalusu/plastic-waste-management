import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { complaintsApi } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import StatusBadge from '../../components/StatusBadge';
import MapView from '../../components/MapView';
import {
  ArrowLeft,
  Calendar,
  MapPin,
  AlertCircle,
  CheckCircle,
  Clock,
  Trash2,
  Image as ImageIcon,
  ShieldCheck,
  UserCheck
} from 'lucide-react';

export default function ComplaintDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Cancellation modal state
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState('');

  const [selectedPhoto, setSelectedPhoto] = useState(null);

  const fetchDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await complaintsApi.getById(id);
      setComplaint(data);
    } catch (err) {
      setError(err.message || 'Failed to load complaint details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetails();
  }, [id]);

  const handleCancelComplaint = async (e) => {
    e.preventDefault();
    if (!cancelReason.trim()) {
      setCancelError('Please enter a cancellation reason.');
      return;
    }

    try {
      setCancelling(true);
      setCancelError('');
      await complaintsApi.cancel(id, cancelReason.trim());
      setShowCancelModal(false);
      fetchDetails();
    } catch (err) {
      setCancelError(err.message || 'Failed to cancel complaint.');
    } finally {
      setCancelling(false);
    }
  };

  const isOwner = user && complaint && user.id === complaint.citizenId;
  const canCancel = (isOwner || user?.role === 'ADMIN') &&
    ['SUBMITTED', 'UNDER_REVIEW'].includes(complaint?.status);

  if (loading) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '6rem 1rem' }}>
        <div className="badge-dot" style={{ width: '20px', height: '20px', margin: '0 auto 1.5rem' }} />
        <div style={{ color: 'var(--text-muted)' }}>Loading report details & audit history...</div>
      </div>
    );
  }

  if (error || !complaint) {
    return (
      <div className="main-content" style={{ maxWidth: '640px', margin: '3rem auto' }}>
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center' }}>
          <AlertCircle size={40} color="#f43f5e" style={{ margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.5rem' }}>Unable to Load Report</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>{error || 'Complaint not found.'}</p>
          <button onClick={() => navigate(-1)} className="service-btn">
            <ArrowLeft size={16} /> Go Back
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
      <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
        {/* Top Navigation / Action Bar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
          <button
            onClick={() => navigate(-1)}
            className="service-btn"
            style={{ padding: '0.45rem 0.9rem' }}
          >
            <ArrowLeft size={16} /> Back
          </button>

          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            <StatusBadge status={complaint.status} size="lg" />
            {canCancel && (
              <button
                onClick={() => setShowCancelModal(true)}
                className="service-btn"
                style={{ borderColor: 'rgba(244, 63, 94, 0.4)', color: '#f43f5e', background: 'rgba(244, 63, 94, 0.1)' }}
              >
                <Trash2 size={16} /> Cancel Report
              </button>
            )}
          </div>
        </div>

        {/* Complaint Overview Card */}
        <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, marginBottom: '0.75rem', lineHeight: 1.25 }}>
            {complaint.title}
          </h1>

          <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap', fontSize: '0.88rem', color: 'var(--text-dim)', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Calendar size={15} />
              <span>Reported on {new Date(complaint.createdAt).toLocaleString()}</span>
            </div>
            {complaint.addressText && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <MapPin size={15} color="var(--primary)" />
                <span>{complaint.addressText}</span>
              </div>
            )}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <span className="badge-port" style={{ padding: '2px 8px', borderRadius: '4px' }}>
                {complaint.wasteType}
              </span>
            </div>
          </div>

          <div style={{ marginBottom: '1.5rem' }}>
            <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Description
            </h3>
            <p style={{ color: 'var(--text-main)', fontSize: '1.02rem', lineHeight: 1.6, background: 'rgba(255, 255, 255, 0.02)', padding: '1rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)' }}>
              {complaint.description}
            </p>
          </div>

          {complaint.rejectionReason && (
            <div style={{ background: 'rgba(244, 63, 94, 0.1)', border: '1px solid rgba(244, 63, 94, 0.3)', padding: '1rem', borderRadius: 'var(--radius-sm)', color: '#fb7185', marginBottom: '1.5rem' }}>
              <strong>Admin Rejection Reason:</strong> {complaint.rejectionReason}
            </div>
          )}

          {complaint.cancellationReason && (
            <div style={{ background: 'rgba(148, 163, 184, 0.1)', border: '1px solid rgba(148, 163, 184, 0.3)', padding: '1rem', borderRadius: 'var(--radius-sm)', color: '#94a3b8', marginBottom: '1.5rem' }}>
              <strong>Cancellation Reason:</strong> {complaint.cancellationReason}
            </div>
          )}
        </div>

        {/* Photos and Map Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '2rem', marginBottom: '2rem' }}>
          {/* Photo Gallery */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <ImageIcon size={18} color="var(--primary)" />
              Uploaded Proof Photos ({complaint.images?.length || 0})
            </h3>

            {complaint.images && complaint.images.length > 0 ? (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))', gap: '0.75rem' }}>
                {complaint.images.map((img) => (
                  <div
                    key={img.id}
                    onClick={() => setSelectedPhoto(img.imageUrl)}
                    style={{
                      height: '110px',
                      borderRadius: 'var(--radius-sm)',
                      overflow: 'hidden',
                      border: '1px solid var(--border-subtle)',
                      cursor: 'pointer',
                      position: 'relative'
                    }}
                  >
                    <img src={img.imageUrl} alt="Proof" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    <span style={{ position: 'absolute', bottom: '4px', left: '4px', background: 'rgba(0,0,0,0.65)', fontSize: '10px', padding: '2px 6px', borderRadius: '4px', color: '#fff' }}>
                      {img.imageType || 'INITIAL_PROOF'}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-dim)', fontSize: '0.9rem' }}>
                No images attached to this complaint.
              </div>
            )}
          </div>

          {/* Map Location */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <MapPin size={18} color="var(--accent-cyan)" />
              Geographic Location
            </h3>
            <MapView
              height="260px"
              center={mapCenter}
              zoom={14}
              markers={[marker]}
            />
            <div style={{ marginTop: '0.75rem', fontSize: '0.8rem', color: 'var(--text-dim)', display: 'flex', justifyContent: 'space-between' }}>
              <span>Lat: {complaint.latitude}</span>
              <span>Lng: {complaint.longitude}</span>
            </div>
          </div>
        </div>

        {/* Progress History Audit Log */}
        <div className="glass-panel" style={{ padding: '2rem' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock size={20} color="var(--accent-amber)" />
            Immutable Audit Trail & Status Timeline
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Initial Submission Log */}
            <div style={{ display: 'flex', gap: '1rem', position: 'relative' }}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <div style={{ width: '14px', height: '14px', borderRadius: '50%', background: '#38bdf8', boxShadow: '0 0 8px #38bdf8' }} />
                <div style={{ width: '2px', flex: 1, background: 'var(--border-subtle)', marginTop: '4px' }} />
              </div>
              <div style={{ paddingBottom: '1.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.25rem' }}>
                  <span style={{ fontWeight: 700, fontSize: '0.92rem' }}>SUBMISSION</span>
                  <StatusBadge status="SUBMITTED" size="sm" />
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>{new Date(complaint.createdAt).toLocaleString()}</span>
                </div>
                <div style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
                  Report submitted by citizen with GPS coordinates and initial photos.
                </div>
              </div>
            </div>

            {/* If resolved or verified */}
            {complaint.status === 'VERIFIED' && (
              <div style={{ display: 'flex', gap: '1rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  <div style={{ width: '14px', height: '14px', borderRadius: '50%', background: '#34d399', boxShadow: '0 0 8px #34d399' }} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.92rem' }}>VERIFICATION</span>
                    <StatusBadge status="VERIFIED" size="sm" />
                  </div>
                  <div style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
                    Report verified by administrator. Queued for volunteer dispatch.
                  </div>
                </div>
              </div>
            )}

            {complaint.status === 'RESOLVED' && (
              <div style={{ display: 'flex', gap: '1rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  <div style={{ width: '14px', height: '14px', borderRadius: '50%', background: '#10b981', boxShadow: '0 0 8px #10b981' }} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.92rem' }}>CLEANUP RESOLVED</span>
                    <StatusBadge status="RESOLVED" size="sm" />
                  </div>
                  <div style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
                    Volunteer finished cleanup and submitted verified after-photo.
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Cancellation Modal */}
        {showCancelModal && (
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
                Cancel Waste Report
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginBottom: '1.25rem' }}>
                Please provide a cancellation reason (e.g. reported by mistake, already cleaned by shop owner).
              </p>

              {cancelError && (
                <div style={{ color: '#f43f5e', fontSize: '0.85rem', marginBottom: '1rem' }}>
                  {cancelError}
                </div>
              )}

              <form onSubmit={handleCancelComplaint} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <textarea
                  required
                  rows={3}
                  placeholder="Reason for cancellation..."
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
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
                    onClick={() => setShowCancelModal(false)}
                    className="service-btn"
                  >
                    Keep Report
                  </button>
                  <button
                    type="submit"
                    disabled={cancelling}
                    className="service-btn"
                    style={{ background: '#f43f5e', color: '#fff', border: 'none', fontWeight: 700 }}
                  >
                    {cancelling ? 'Cancelling...' : 'Confirm Cancel'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Lightbox Photo Preview */}
        {selectedPhoto && (
          <div
            onClick={() => setSelectedPhoto(null)}
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
            <img src={selectedPhoto} alt="Full view" style={{ maxWidth: '90vw', maxHeight: '85vh', borderRadius: '8px', objectFit: 'contain' }} />
          </div>
        )}
      </div>
    </div>
  );
}
