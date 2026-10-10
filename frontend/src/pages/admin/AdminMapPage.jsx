import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { complaintsApi } from '../../api/client';
import MapView from '../../components/MapView';
import { MapPin, Filter, Eye, AlertCircle } from 'lucide-react';

export default function AdminMapPage() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeFilter, setActiveFilter] = useState('ALL');

  useEffect(() => {
    async function loadMapData() {
      try {
        setLoading(true);
        setError(null);
        const data = await complaintsApi.getAll();
        setComplaints(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.message || 'Failed to load map data');
      } finally {
        setLoading(false);
      }
    }
    loadMapData();
  }, []);

  const filtered = complaints.filter(c => {
    if (!c.latitude || !c.longitude) return false;
    if (activeFilter === 'ALL') return true;
    if (activeFilter === 'ACTIVE') return ['SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'ASSIGNED', 'IN_PROGRESS'].includes(c.status);
    return c.status === activeFilter;
  });

  const markers = filtered.map(c => ({
    id: c.id,
    lat: parseFloat(c.latitude),
    lng: parseFloat(c.longitude),
    title: c.title,
    description: c.addressText || c.description,
    status: c.status
  }));

  const initialCenter = markers.length > 0 ? [markers[0].lat, markers[0].lng] : [12.9716, 77.5946];

  return (
    <div className="main-content">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.35rem' }}>Geospatial Waste Map</h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Real-time visualization of plastic waste reports across neighborhoods.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {[
            { label: 'All Hotspots', value: 'ALL' },
            { label: 'Active', value: 'ACTIVE' },
            { label: 'Verified', value: 'VERIFIED' },
            { label: 'Resolved', value: 'RESOLVED' }
          ].map(f => (
            <button
              key={f.value}
              onClick={() => setActiveFilter(f.value)}
              style={{
                padding: '0.4rem 0.8rem',
                borderRadius: 'var(--radius-sm)',
                border: activeFilter === f.value ? '1px solid var(--primary)' : '1px solid var(--border-subtle)',
                background: activeFilter === f.value ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                color: activeFilter === f.value ? '#34d399' : 'var(--text-muted)',
                fontSize: '0.82rem',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', color: '#f43f5e', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
          <AlertCircle size={18} /> {error}
        </div>
      )}

      <div className="glass-panel" style={{ padding: '1rem', position: 'relative' }}>
        {loading ? (
          <div style={{ height: '620px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
            <div className="badge-dot" style={{ width: '16px', height: '16px', marginRight: '0.75rem' }} />
            Loading geospatial map points...
          </div>
        ) : (
          <MapView
            height="620px"
            center={initialCenter}
            zoom={13}
            markers={markers}
          />
        )}

        <div style={{
          position: 'absolute',
          top: '24px',
          right: '24px',
          zIndex: 1000,
          background: 'rgba(9, 13, 22, 0.88)',
          backdropFilter: 'blur(10px)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-sm)',
          padding: '10px 14px',
          fontSize: '12px',
          color: 'var(--text-main)',
          display: 'flex',
          flexDirection: 'column',
          gap: '6px'
        }}>
          <div style={{ fontWeight: 700, borderBottom: '1px solid var(--border-subtle)', paddingBottom: '4px' }}>
            Map Legend ({markers.length} Pins)
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#38bdf8' }}></span>
            <span>Submitted / Active</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#fbbf24' }}></span>
            <span>Under Review</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981' }}></span>
            <span>Resolved</span>
          </div>
        </div>
      </div>
    </div>
  );
}
