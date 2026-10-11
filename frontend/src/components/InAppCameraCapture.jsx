import React, { useState, useRef, useEffect } from 'react'
import { Camera, MapPin, RefreshCw, CheckCircle2, AlertTriangle, X, ShieldCheck } from 'lucide-react'

export default function InAppCameraCapture({ onPhotoCaptured, onClose, mode = 'modal' }) {
  const [stream, setStream] = useState(null)
  const [capturedImage, setCapturedImage] = useState(null)
  const [coords, setCoords] = useState(null)
  const [accuracy, setAccuracy] = useState(null)
  const [gpsLoading, setGpsLoading] = useState(false)
  const [cameraLoading, setCameraLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [fallbackMode, setFallbackMode] = useState(false)

  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const fileInputRef = useRef(null)

  // Start Camera Stream
  const startCamera = async () => {
    setErrorMsg('')
    setCameraLoading(true)

    // Request GPS in parallel
    acquireGps()

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setFallbackMode(true)
      setCameraLoading(false)
      setErrorMsg('Direct browser camera access is unavailable on this device/browser. Please use standard camera file capture.')
      return
    }

    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: 'environment' },
          width: { ideal: 1920 },
          height: { ideal: 1080 }
        },
        audio: false
      })

      setStream(mediaStream)
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream
      }
      setCameraLoading(false)
    } catch (err) {
      console.warn('getUserMedia error:', err)
      setFallbackMode(true)
      setCameraLoading(false)
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        setErrorMsg('Camera permission was denied. Please allow camera access in browser settings or use file upload fallback.')
      } else {
        setErrorMsg(`Camera error: ${err.message}. Switching to camera file fallback.`)
      }
    }
  }

  // Acquire Geolocation
  const acquireGps = () => {
    if (!navigator.geolocation) {
      setErrorMsg('Geolocation is not supported by your browser.')
      return
    }

    setGpsLoading(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude
        })
        setAccuracy(pos.coords.accuracy)
        setGpsLoading(false)
      },
      (err) => {
        setGpsLoading(false)
        console.warn('Geolocation error:', err)
        if (err.code === 1) {
          setErrorMsg('Location permission was denied. Live GPS coordinates could not be attached.')
        } else {
          setErrorMsg('GPS acquisition timed out or failed. Please ensure location is enabled.')
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  }

  useEffect(() => {
    startCamera()
    return () => {
      // Clean up camera stream on unmount
      if (stream) {
        stream.getTracks().forEach(track => track.stop())
      }
    }
  }, [])

  // Snap photo from video feed
  const handleSnapPhoto = () => {
    if (!videoRef.current) return

    const video = videoRef.current
    const canvas = canvasRef.current || document.createElement('canvas')
    canvas.width = video.videoWidth || 1280
    canvas.height = video.videoHeight || 720

    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

    canvas.toBlob((blob) => {
      if (!blob) {
        setErrorMsg('Failed to process image capture.')
        return
      }

      const file = new File([blob], `proof_photo_${Date.now()}.jpg`, { type: 'image/jpeg' })
      const previewUrl = URL.createObjectURL(blob)

      setCapturedImage({
        file,
        previewUrl,
        timestamp: new Date().toISOString(),
        coords: coords ? { ...coords } : null,
        accuracy,
        source: 'IN_APP_CAMERA'
      })
    }, 'image/jpeg', 0.92)
  }

  // Handle file input fallback
  const handleFallbackFileChange = (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    const previewUrl = URL.createObjectURL(file)
    setCapturedImage({
      file,
      previewUrl,
      timestamp: new Date().toISOString(),
      coords: coords ? { ...coords } : null,
      accuracy,
      source: 'DEVICE_FILE_CAPTURE'
    })
  }

  // Confirm and return photo
  const handleConfirmPhoto = () => {
    if (!capturedImage) return

    onPhotoCaptured({
      file: capturedImage.file,
      previewUrl: capturedImage.previewUrl,
      deviceLatitude: capturedImage.coords?.latitude || null,
      deviceLongitude: capturedImage.coords?.longitude || null,
      deviceCapturedAt: capturedImage.timestamp,
      captureSource: capturedImage.source
    })

    // Stop streams
    if (stream) {
      stream.getTracks().forEach(track => track.stop())
    }
  }

  const handleRetake = () => {
    if (capturedImage?.previewUrl) {
      URL.revokeObjectURL(capturedImage.previewUrl)
    }
    setCapturedImage(null)
    if (!stream && !fallbackMode) {
      startCamera()
    }
  }

  const handleCancel = () => {
    if (stream) {
      stream.getTracks().forEach(track => track.stop())
    }
    if (onClose) onClose()
  }

  const content = (
    <div style={{
      background: '#0f172a',
      color: '#f8fafc',
      borderRadius: '16px',
      overflow: 'hidden',
      maxWidth: '560px',
      width: '100%',
      margin: '0 auto',
      border: '1px solid rgba(255,255,255,0.1)',
      boxShadow: '0 25px 50px -12px rgba(0,0,0,0.5)'
    }}>
      {/* Header */}
      <div style={{
        padding: '1rem 1.25rem',
        borderBottom: '1px solid rgba(255,255,255,0.08)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        background: '#1e293b'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <ShieldCheck size={20} color="#10b981" />
          <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 600 }}>GPS Verified In-App Camera</h3>
        </div>
        {onClose && (
          <button
            type="button"
            onClick={handleCancel}
            style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px' }}
          >
            <X size={20} />
          </button>
        )}
      </div>

      {/* Permission & Security Notice Banner */}
      <div style={{
        background: 'rgba(6, 182, 212, 0.08)',
        borderBottom: '1px solid rgba(6, 182, 212, 0.2)',
        padding: '0.75rem 1.25rem',
        fontSize: '0.8rem',
        color: '#bae6fd',
        lineHeight: 1.4
      }}>
        <strong>Why Camera & Location?</strong> Capturing live photos with device coordinates provides tamper-evident proof that waste was reported and cleaned up at the true physical site.
      </div>

      {/* Viewfinder or Preview Area */}
      <div style={{ position: 'relative', width: '100%', height: '360px', background: '#020617', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {capturedImage ? (
          <img
            src={capturedImage.previewUrl}
            alt="Captured Proof"
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        ) : fallbackMode ? (
          <div style={{ textAlign: 'center', padding: '1.5rem' }}>
            <AlertTriangle size={42} color="#f59e0b" style={{ margin: '0 auto 1rem' }} />
            <h4 style={{ margin: '0 0 0.5rem', color: '#f8fafc' }}>Direct Viewfinder Unavailable</h4>
            <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginBottom: '1.25rem', maxWidth: '340px' }}>
              Your device camera can still be used via standard camera capture.
            </p>
            <input
              type="file"
              ref={fileInputRef}
              accept="image/*"
              capture="environment"
              onChange={handleFallbackFileChange}
              style={{ display: 'none' }}
            />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              style={{
                background: 'linear-gradient(135deg, #10b981, #059669)',
                color: '#fff',
                border: 'none',
                padding: '0.65rem 1.25rem',
                borderRadius: '8px',
                fontWeight: 600,
                fontSize: '0.9rem',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}
            >
              <Camera size={18} /> Open Device Camera
            </button>
          </div>
        ) : (
          <>
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
            {/* Live Camera Grid overlay */}
            <div style={{
              position: 'absolute',
              inset: 0,
              pointerEvents: 'none',
              border: '2px solid rgba(255,255,255,0.15)',
              display: 'grid',
              gridTemplateColumns: '1fr 1fr 1fr',
              gridTemplateRows: '1fr 1fr 1fr'
            }}>
              <div style={{ borderRight: '1px dashed rgba(255,255,255,0.1)', borderBottom: '1px dashed rgba(255,255,255,0.1)' }} />
              <div style={{ borderRight: '1px dashed rgba(255,255,255,0.1)', borderBottom: '1px dashed rgba(255,255,255,0.1)' }} />
              <div style={{ borderBottom: '1px dashed rgba(255,255,255,0.1)' }} />
              <div style={{ borderRight: '1px dashed rgba(255,255,255,0.1)', borderBottom: '1px dashed rgba(255,255,255,0.1)' }} />
              <div style={{ borderRight: '1px dashed rgba(255,255,255,0.1)', borderBottom: '1px dashed rgba(255,255,255,0.1)' }} />
              <div style={{ borderBottom: '1px dashed rgba(255,255,255,0.1)' }} />
            </div>
          </>
        )}

        {/* GPS Badge in Viewfinder */}
        <div style={{
          position: 'absolute',
          bottom: '12px',
          left: '12px',
          right: '12px',
          background: 'rgba(15, 23, 42, 0.85)',
          backdropFilter: 'blur(6px)',
          borderRadius: '8px',
          padding: '0.5rem 0.75rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: '0.78rem',
          border: '1px solid rgba(255,255,255,0.1)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <MapPin size={15} color={coords ? '#10b981' : '#f59e0b'} />
            {gpsLoading ? (
              <span style={{ color: '#94a3b8' }}>Acquiring GPS lock...</span>
            ) : coords ? (
              <span style={{ color: '#e2e8f0', fontFamily: 'monospace' }}>
                {coords.latitude.toFixed(6)}, {coords.longitude.toFixed(6)}
                {accuracy ? ` (±${Math.round(accuracy)}m)` : ''}
              </span>
            ) : (
              <span style={{ color: '#f59e0b' }}>GPS coordinates pending</span>
            )}
          </div>
          <button
            type="button"
            onClick={acquireGps}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#38bdf8',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '3px',
              fontSize: '0.75rem'
            }}
          >
            <RefreshCw size={12} /> Retry GPS
          </button>
        </div>
      </div>

      {/* Errors display */}
      {errorMsg && (
        <div style={{
          padding: '0.6rem 1.25rem',
          background: 'rgba(239, 68, 68, 0.1)',
          borderBottom: '1px solid rgba(239, 68, 68, 0.2)',
          color: '#f87171',
          fontSize: '0.8rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem'
        }}>
          <AlertTriangle size={15} />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Action Controls */}
      <div style={{
        padding: '1rem 1.25rem',
        background: '#1e293b',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        {capturedImage ? (
          <>
            <button
              type="button"
              onClick={handleRetake}
              style={{
                background: 'rgba(255,255,255,0.08)',
                color: '#e2e8f0',
                border: '1px solid rgba(255,255,255,0.15)',
                padding: '0.65rem 1rem',
                borderRadius: '8px',
                fontWeight: 500,
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem'
              }}
            >
              <RefreshCw size={15} /> Retake
            </button>
            <button
              type="button"
              onClick={handleConfirmPhoto}
              style={{
                background: 'linear-gradient(135deg, #10b981, #059669)',
                color: '#fff',
                border: 'none',
                padding: '0.65rem 1.25rem',
                borderRadius: '8px',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
                boxShadow: '0 4px 12px rgba(16, 185, 129, 0.25)'
              }}
            >
              <CheckCircle2 size={16} /> Attach Geotagged Photo
            </button>
          </>
        ) : (
          <>
            <button
              type="button"
              onClick={handleCancel}
              style={{
                background: 'transparent',
                color: '#94a3b8',
                border: 'none',
                padding: '0.65rem',
                fontSize: '0.85rem',
                cursor: 'pointer'
              }}
            >
              Cancel
            </button>
            {!fallbackMode && (
              <button
                type="button"
                onClick={handleSnapPhoto}
                disabled={cameraLoading}
                style={{
                  background: 'linear-gradient(135deg, #0284c7, #0369a1)',
                  color: '#fff',
                  border: 'none',
                  padding: '0.75rem 1.75rem',
                  borderRadius: '30px',
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  boxShadow: '0 4px 14px rgba(2, 132, 199, 0.35)'
                }}
              >
                <Camera size={18} /> Take Photo
              </button>
            )}
          </>
        )}
      </div>
      <canvas ref={canvasRef} style={{ display: 'none' }} />
    </div>
  )

  if (mode === 'modal') {
    return (
      <div style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(5px)',
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem'
      }}>
        {content}
      </div>
    )
  }

  return content
}
