import React, { useEffect, useRef } from 'react';
import L from 'leaflet';

export default function MapView({
  center = [12.9716, 77.5946], // Default center
  zoom = 13,
  markers = [],
  pickerMode = false,
  selectedLocation = null,
  onLocationSelect = null,
  height = '420px'
}) {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersGroupRef = useRef(null);
  const pickerMarkerRef = useRef(null);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: center,
        zoom: zoom,
        zoomControl: true
      });

      // OpenStreetMap standard tile layer (no API key required)
      const tileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
      });

      tileLayer.on('tileerror', (error) => {
        console.warn('Map tile loading error:', error);
      });

      tileLayer.addTo(map);

      markersGroupRef.current = L.layerGroup().addTo(map);
      mapInstanceRef.current = map;

      // Handle map clicks in picker mode
      map.on('click', (e) => {
        if (pickerMode && onLocationSelect) {
          onLocationSelect({
            lat: parseFloat(e.latlng.lat.toFixed(6)),
            lng: parseFloat(e.latlng.lng.toFixed(6))
          });
        }
      });
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  // Sync center and zoom when changed
  useEffect(() => {
    if (mapInstanceRef.current && center && center[0] && center[1]) {
      mapInstanceRef.current.setView(center, zoom);
    }
  }, [center?.[0], center?.[1], zoom]);

  // Update Markers
  useEffect(() => {
    if (!mapInstanceRef.current || !markersGroupRef.current) return;

    markersGroupRef.current.clearLayers();

    markers.forEach((m) => {
      if (!m.lat || !m.lng) return;

      const isResolved = m.status === 'RESOLVED';
      const isUnderReview = m.status === 'UNDER_REVIEW';
      const pinColor = isResolved ? '#10b981' : isUnderReview ? '#fbbf24' : '#38bdf8';

      const customIcon = L.divIcon({
        className: 'custom-map-pin',
        html: `
          <div style="
            background: ${pinColor};
            width: 26px;
            height: 26px;
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            display: flex;
            align-items: center;
            justify-content: center;
            border: 2px solid white;
            box-shadow: 0 0 10px ${pinColor};
          ">
            <span style="transform: rotate(45deg); font-size: 11px; font-weight: bold; color: black;">•</span>
          </div>
        `,
        iconSize: [26, 26],
        iconAnchor: [13, 26]
      });

      const marker = L.marker([m.lat, m.lng], { icon: customIcon });

      if (m.title || m.description) {
        marker.bindPopup(`
          <div style="font-family: sans-serif; padding: 4px; color: #1e293b;">
            <strong style="font-size: 14px; display: block; margin-bottom: 4px;">${m.title || 'Waste Site'}</strong>
            <div style="font-size: 12px; margin-bottom: 6px; color: #475569;">${m.description || ''}</div>
            <span style="font-size: 10px; font-weight: bold; background: #e2e8f0; padding: 2px 6px; border-radius: 4px;">
              ${m.status || 'SUBMITTED'}
            </span>
          </div>
        `);
      }

      if (m.onClick) {
        marker.on('click', m.onClick);
      }

      marker.addTo(markersGroupRef.current);
    });
  }, [markers]);

  // Update Picker Pin
  useEffect(() => {
    if (!mapInstanceRef.current) return;

    if (pickerMarkerRef.current) {
      pickerMarkerRef.current.remove();
      pickerMarkerRef.current = null;
    }

    if (pickerMode && selectedLocation && selectedLocation.lat && selectedLocation.lng) {
      const pickerIcon = L.divIcon({
        className: 'picker-pin',
        html: `
          <div style="
            background: #10b981;
            width: 30px;
            height: 30px;
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            display: flex;
            align-items: center;
            justify-content: center;
            border: 3px solid #ffffff;
            box-shadow: 0 0 14px #10b981;
          ">
            <span style="transform: rotate(45deg); font-size: 14px; color: black; font-weight: bold;">📍</span>
          </div>
        `,
        iconSize: [30, 30],
        iconAnchor: [15, 30]
      });

      pickerMarkerRef.current = L.marker([selectedLocation.lat, selectedLocation.lng], {
        icon: pickerIcon
      }).addTo(mapInstanceRef.current);
    }
  }, [pickerMode, selectedLocation?.lat, selectedLocation?.lng]);

  return (
    <div
      style={{
        width: '100%',
        height,
        borderRadius: 'var(--radius-md)',
        overflow: 'hidden',
        border: '1px solid var(--border-subtle)',
        position: 'relative'
      }}
    >
      <div ref={mapContainerRef} style={{ width: '100%', height: '100%' }} />
      {pickerMode && (
        <div
          style={{
            position: 'absolute',
            bottom: '12px',
            left: '12px',
            zIndex: 1000,
            background: 'rgba(9, 13, 22, 0.85)',
            backdropFilter: 'blur(8px)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-sm)',
            padding: '6px 12px',
            fontSize: '12px',
            color: 'var(--text-muted)'
          }}
        >
          💡 Click on the map to pin precise waste location
        </div>
      )}
    </div>
  );
}
