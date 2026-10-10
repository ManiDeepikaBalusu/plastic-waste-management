import React from 'react';

const STATUS_CONFIG = {
  SUBMITTED: { label: 'Submitted', color: '#38bdf8', bg: 'rgba(56, 189, 248, 0.12)', border: 'rgba(56, 189, 248, 0.3)' },
  UNDER_REVIEW: { label: 'Under Review', color: '#fbbf24', bg: 'rgba(251, 191, 36, 0.12)', border: 'rgba(251, 191, 36, 0.3)' },
  VERIFIED: { label: 'Verified', color: '#34d399', bg: 'rgba(52, 211, 153, 0.12)', border: 'rgba(52, 211, 153, 0.3)' },
  ASSIGNED: { label: 'Assigned', color: '#a78bfa', bg: 'rgba(167, 139, 250, 0.12)', border: 'rgba(167, 139, 250, 0.3)' },
  ACCEPTED: { label: 'Accepted', color: '#818cf8', bg: 'rgba(129, 140, 248, 0.12)', border: 'rgba(129, 140, 248, 0.3)' },
  IN_PROGRESS: { label: 'In Progress', color: '#f97316', bg: 'rgba(249, 115, 22, 0.12)', border: 'rgba(249, 115, 22, 0.3)' },
  RESOLVED: { label: 'Resolved', color: '#10b981', bg: 'rgba(16, 185, 129, 0.18)', border: 'rgba(16, 185, 129, 0.4)' },
  REJECTED: { label: 'Rejected', color: '#f43f5e', bg: 'rgba(244, 63, 94, 0.12)', border: 'rgba(244, 63, 94, 0.3)' },
  CANCELLED: { label: 'Cancelled', color: '#94a3b8', bg: 'rgba(148, 163, 184, 0.12)', border: 'rgba(148, 163, 184, 0.3)' },
  DEFAULT: { label: 'Unknown', color: '#94a3b8', bg: 'rgba(148, 163, 184, 0.12)', border: 'rgba(148, 163, 184, 0.3)' }
};

export default function StatusBadge({ status, size = 'md' }) {
  const normStatus = (status || '').toUpperCase();
  const config = STATUS_CONFIG[normStatus] || { ...STATUS_CONFIG.DEFAULT, label: status || 'Unknown' };

  const padding = size === 'sm' ? '0.2rem 0.55rem' : size === 'lg' ? '0.45rem 1rem' : '0.3rem 0.75rem';
  const fontSize = size === 'sm' ? '0.72rem' : size === 'lg' ? '0.88rem' : '0.8rem';

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '0.4rem',
        padding,
        fontSize,
        fontWeight: 600,
        borderRadius: '9999px',
        color: config.color,
        background: config.bg,
        border: `1px solid ${config.border}`,
        letterSpacing: '0.02em',
        textTransform: 'uppercase',
        lineHeight: 1
      }}
    >
      <span
        style={{
          width: size === 'sm' ? '6px' : '7px',
          height: size === 'sm' ? '6px' : '7px',
          borderRadius: '50%',
          backgroundColor: config.color,
          boxShadow: `0 0 6px ${config.color}`
        }}
      />
      {config.label}
    </span>
  );
}
