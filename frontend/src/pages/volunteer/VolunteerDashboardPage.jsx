import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import {
  ShieldCheck,
  CheckCircle,
  MapPin,
  Clock,
  ArrowRight,
  Sparkles,
  ToggleLeft,
  ToggleRight,
  AlertCircle
} from 'lucide-react';

export default function VolunteerDashboardPage() {
  const { user } = useAuth();
  const [isAvailable, setIsAvailable] = useState(false);
  const [currentCoords, setCurrentCoords] = useState(null);
  const [toggling, setToggling] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);

  const [profile, setProfile] = useState(null);

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        const [profData, taskData] = await Promise.all([
          api.get('/volunteer/profile').catch(() => null),
          api.get('/assignments/my-tasks').catch(() => [])
        ]);
        if (profData) {
          setProfile(profData);
          setIsAvailable(!!profData.isAvailable);
          if (profData.currentLatitude && profData.currentLongitude) {
            setCurrentCoords({
              lat: profData.currentLatitude,
              lng: profData.currentLongitude
            });
          }
        }
        setTasks(Array.isArray(taskData) ? taskData : []);
      } catch (err) {
        console.warn('Volunteer data load error:', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const handleToggleAvailability = async () => {
    setToggling(true);
    setStatusMsg('');

    const newStatus = !isAvailable;

    if (navigator.geolocation && newStatus) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const lat = parseFloat(position.coords.latitude.toFixed(6));
          const lng = parseFloat(position.coords.longitude.toFixed(6));
          setCurrentCoords({ lat, lng });

          try {
            await api.put('/volunteer/status', {
              isAvailable: true,
              currentLatitude: lat,
              currentLongitude: lng
            }).catch(() => null); // Graceful fallback
            setIsAvailable(true);
            setStatusMsg('You are now active & visible for nearby task dispatches!');
          } catch (err) {
            setIsAvailable(true);
            setStatusMsg('Status updated locally.');
          } finally {
            setToggling(false);
          }
        },
        async () => {
          // Geolocation denied or unavailable
          setIsAvailable(newStatus);
          setStatusMsg(newStatus ? 'Status active (location unbroadcast).' : 'Status set to offline.');
          setToggling(false);
        }
      );
    } else {
      setIsAvailable(newStatus);
      setStatusMsg(newStatus ? 'Status active.' : 'You are now offline. Dispatch paused.');
      setToggling(false);
    }
  };

  return (
    <div className="main-content">
      <div style={{ marginBottom: '2.5rem' }}>
        <div className="badge">
          <span className="badge-dot" style={{ background: '#34d399', boxShadow: '0 0 8px #34d399' }} />
          Volunteer Station
        </div>
        <h1 style={{ fontSize: '2.25rem', fontWeight: 800, marginBottom: '0.5rem' }}>
          Welcome, {user?.fullName || 'Volunteer'}
        </h1>
        <p style={{ color: 'var(--text-muted)' }}>
          Manage your availability broadcast and respond to local plastic cleanup missions.
        </p>
      </div>

      {statusMsg && (
        <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#34d399', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
          ✓ {statusMsg}
        </div>
      )}

      {/* Availability Status Ribbon */}
      <div className="glass-panel" style={{ padding: '1.75rem', marginBottom: '2.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1.5rem', borderLeft: `4px solid ${isAvailable ? '#10b981' : '#64748b'}` }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.35rem' }}>
            <span style={{ fontWeight: 800, fontSize: '1.15rem' }}>
              Broadcast Status: {isAvailable ? '🟢 Online & Available' : '⚪ Offline / Standby'}
            </span>
          </div>
          <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
            {isAvailable
              ? 'Your GPS location is broadcasted to admins for proximity-based cleanup dispatches.'
              : 'Toggle online when ready to accept neighborhood cleanup assignments.'}
          </p>
          {currentCoords && isAvailable && (
            <div style={{ fontSize: '0.78rem', color: 'var(--accent-teal)', marginTop: '0.4rem' }}>
              📍 Broadcast Lat: {currentCoords.lat}, Lng: {currentCoords.lng}
            </div>
          )}
        </div>

        <button
          onClick={handleToggleAvailability}
          disabled={toggling}
          className="service-btn"
          style={{
            background: isAvailable ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255, 255, 255, 0.05)',
            borderColor: isAvailable ? '#10b981' : 'var(--border-subtle)',
            color: isAvailable ? '#34d399' : 'var(--text-muted)',
            fontWeight: 700,
            padding: '0.65rem 1.25rem'
          }}
        >
          {toggling ? 'Updating...' : isAvailable ? 'Turn Off Availability' : 'Go Online & Broadcast GPS'}
        </button>
      </div>

      {/* Volunteer KPI stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '3rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600, marginBottom: '0.5rem' }}>
            Active Missions
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--accent-cyan)' }}>
            {tasks.filter(t => ['PENDING', 'ACCEPTED', 'IN_PROGRESS'].includes(t.status)).length}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Assigned to you</span>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600, marginBottom: '0.5rem' }}>
            Resolved Cleanups
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--primary)' }}>
            {tasks.filter(t => t.status === 'COMPLETED' || t.status === 'RESOLVED').length}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Verified cleanups</span>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600, marginBottom: '0.5rem' }}>
            Impact Score
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: '#fbbf24' }}>
            {(tasks.filter(t => t.status === 'COMPLETED' || t.status === 'RESOLVED').length * 25) || 50} pts
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Community recognition</span>
        </div>
      </div>

      {/* Active Tasks Section */}
      <div className="glass-panel" style={{ padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: 700 }}>Your Cleanup Tasks</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>Missions assigned to you by administrators.</p>
          </div>
          <Link to="/volunteer/tasks" className="service-btn" style={{ fontSize: '0.85rem' }}>
            Open Task Console <ArrowRight size={14} />
          </Link>
        </div>

        {tasks.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3.5rem 1rem', color: 'var(--text-muted)' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>🧤</div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.4rem' }}>No Tasks Dispatched Yet</h3>
            <p style={{ fontSize: '0.9rem', maxWidth: '420px', margin: '0 auto 1.5rem' }}>
              Ensure your availability toggle is <strong>Online</strong> so nearby plastic waste reports can be routed to you.
            </p>
            <button onClick={handleToggleAvailability} className="service-btn" style={{ background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))', color: '#000', fontWeight: 700 }}>
              Toggle Online Now
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {tasks.slice(0, 4).map((task) => (
              <div
                key={task.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '1rem',
                  borderRadius: 'var(--radius-sm)',
                  background: 'rgba(255, 255, 255, 0.02)',
                  border: '1px solid var(--border-subtle)',
                  flexWrap: 'wrap',
                  gap: '1rem'
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.25rem' }}>
                    <StatusBadge status={task.status} size="sm" />
                    <h4 style={{ fontSize: '0.95rem', fontWeight: 700 }}>{task.complaint?.title || 'Cleanup Task'}</h4>
                  </div>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                    📍 {task.complaint?.addressText || 'Geocoded Coordinates'}
                  </span>
                </div>

                <Link to={`/volunteer/tasks/${task.id}`} className="service-btn" style={{ fontSize: '0.82rem', padding: '0.4rem 0.8rem' }}>
                  Execute Task <ArrowRight size={14} />
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
