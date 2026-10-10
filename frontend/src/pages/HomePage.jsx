import React, { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { complaintsApi } from '../api/client';
import MapView from '../components/MapView';
import { Camera, MapPin, CheckCircle, Users, ArrowRight, ShieldCheck, Sparkles } from 'lucide-react';

export default function HomePage() {
  const { user, role, loading: authLoading } = useAuth();
  const [recentComplaints, setRecentComplaints] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadPreview() {
      if (!user || role !== 'ADMIN') {
        setLoading(false);
        return;
      }
      try {
        const data = await complaintsApi.getAll().catch(() => []);
        setRecentComplaints(Array.isArray(data) ? data.slice(0, 8) : []);
      } catch (err) {
        console.warn('Could not load complaints preview:', err);
      } finally {
        setLoading(false);
      }
    }
    loadPreview();
  }, [user, role]);

  const markers = recentComplaints
    .filter(c => c.latitude && c.longitude)
    .map(c => ({
      id: c.id,
      lat: parseFloat(c.latitude),
      lng: parseFloat(c.longitude),
      title: c.title,
      description: c.addressText || c.description,
      status: c.status
    }));

  return (
    <div className="main-content">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="badge">
          <span className="badge-dot" />
          COMMUNITY-DRIVEN CIVIC CLEANUP PLATFORM
        </div>
        <h1 className="hero-title">
          Turn Plastic Waste Into <br />
          <span className="hero-highlight">Actionable Community Cleanups</span>
        </h1>
        <p className="hero-description">
          Empowering citizens to report localized plastic pollution with geolocated photos. Admins triage and dispatch verified alerts directly to nearby volunteers for transparent, verified resolution.
        </p>

        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', marginTop: '2rem', flexWrap: 'wrap' }}>
          <Link
            to={user ? "/citizen/report" : "/login"}
            className="service-btn"
            style={{
              background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))',
              color: '#000',
              fontWeight: 800,
              padding: '0.85rem 1.75rem',
              fontSize: '1rem',
              boxShadow: '0 0 20px var(--primary-glow)'
            }}
          >
            <Camera size={18} />
            Report Plastic Waste
            <ArrowRight size={18} />
          </Link>

          {user ? (
            <Link
              to={role === 'ADMIN' ? '/admin' : role === 'VOLUNTEER' ? '/volunteer' : '/citizen/complaints'}
              className="service-btn"
              style={{ padding: '0.85rem 1.75rem', fontSize: '1rem' }}
            >
              <Users size={18} />
              Go to My Dashboard
            </Link>
          ) : (
            <Link
              to="/register"
              className="service-btn"
              style={{ padding: '0.85rem 1.75rem', fontSize: '1rem' }}
            >
              <Users size={18} />
              Join as Volunteer
            </Link>
          )}
        </div>
      </section>

      {/* Stats Counter Ribbon */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '3.5rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem', textAlign: 'center' }}>
          <div style={{ fontSize: '2.25rem', fontWeight: 800, color: 'var(--primary)' }}>
            {recentComplaints.length > 0 ? recentComplaints.length : '100%'}
          </div>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 600 }}>Active Reports Tracked</div>
        </div>
        <div className="glass-panel" style={{ padding: '1.5rem', textAlign: 'center' }}>
          <div style={{ fontSize: '2.25rem', fontWeight: 800, color: 'var(--accent-cyan)' }}>Leaflet + OSM</div>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 600 }}>GPS Spatial Mapping</div>
        </div>
        <div className="glass-panel" style={{ padding: '1.5rem', textAlign: 'center' }}>
          <div style={{ fontSize: '2.25rem', fontWeight: 800, color: '#fbbf24' }}>Cloudinary</div>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 600 }}>Secure Media Storage</div>
        </div>
        <div className="glass-panel" style={{ padding: '1.5rem', textAlign: 'center' }}>
          <div style={{ fontSize: '2.25rem', fontWeight: 800, color: '#34d399' }}>Append-Only</div>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 600 }}>Immutable Audit Trails</div>
        </div>
      </div>

      {/* 3-Step Workflow Guide */}
      <div style={{ marginBottom: '4rem' }}>
        <h2 className="section-title" style={{ textAlign: 'center' }}>How the Platform Operates</h2>
        <p className="section-desc" style={{ textAlign: 'center', maxWidth: '640px', margin: '0 auto 2.5rem' }}>
          A closed-loop workflow ensuring every piece of plastic reported is validated, assigned, and documented.
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem' }}>
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '12px', background: 'rgba(56, 189, 248, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#38bdf8', marginBottom: '1.25rem' }}>
              <Camera size={24} />
            </div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem' }}>1. Citizen Reports</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem' }}>
              Citizens capture photos of street dumps or waterbody plastic. Real-time GPS tagging provides exact geospatial coordinates.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '2rem' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '12px', background: 'rgba(251, 191, 36, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fbbf24', marginBottom: '1.25rem' }}>
              <ShieldCheck size={24} />
            </div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem' }}>2. Admin Triage & Verification</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem' }}>
              Administrators review photos, filter duplicate reports, verify waste severity, and query available nearby volunteers.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '2rem' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#10b981', marginBottom: '1.25rem' }}>
              <CheckCircle size={24} />
            </div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem' }}>3. Volunteer Cleanup</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem' }}>
              Nearby volunteers accept tasks, navigate to the site, clear the plastic waste, and upload verified after-photos to resolve the alert.
            </p>
          </div>
        </div>
      </div>

      {/* Live Map Preview Section */}
      <div className="glass-panel" style={{ padding: '2rem', marginBottom: '4rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <MapPin size={20} color="var(--primary)" />
              Live Waste Hotspots Map
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              Geospatial view of active reports and community cleanup activities.
            </p>
          </div>
          {role === 'ADMIN' && (
            <Link to="/admin/map" className="service-btn">
              Open Fullscreen Admin Map <ArrowRight size={14} />
            </Link>
          )}
        </div>

        <MapView
          height="460px"
          markers={markers}
          center={markers.length > 0 ? [markers[0].lat, markers[0].lng] : [12.9716, 77.5946]}
          zoom={12}
        />
      </div>
    </div>
  );
}
