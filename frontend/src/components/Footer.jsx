import React from 'react';

export default function Footer() {
  return (
    <footer className="footer">
      <div style={{ maxWidth: '1280px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '0.75rem', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, color: 'var(--text-main)' }}>
          <span>🌱 EcoClean Network</span>
          <span style={{ color: 'var(--text-dim)' }}>•</span>
          <span style={{ fontSize: '0.8rem', color: 'var(--accent-teal)' }}>Smart Plastic Waste Management</span>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
          Empowering citizens, local communities, and verified volunteers with real-time reporting, dispatching, and audit trails.
        </p>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '0.5rem' }}>
          © {new Date().getFullYear()} EcoClean Network. All rights reserved. Built with React, Spring Boot & PostgreSQL.
        </div>
      </div>
    </footer>
  );
}
