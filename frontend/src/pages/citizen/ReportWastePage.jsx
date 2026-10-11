import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { complaintsApi } from '../../api/client';
import MapView from '../../components/MapView';
import InAppCameraCapture from '../../components/InAppCameraCapture';
import { Camera, MapPin, AlertCircle, CheckCircle, Upload, X, ArrowRight, ShieldCheck } from 'lucide-react';

export default function ReportWastePage() {
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [wasteType, setWasteType] = useState('PLASTIC_BOTTLES');
  const [estimatedSize, setEstimatedSize] = useState('MEDIUM_PILE');
  const [addressText, setAddressText] = useState('');
  const [latitude, setLatitude] = useState('12.971598');
  const [longitude, setLongitude] = useState('77.594562');
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [deviceMetadata, setDeviceMetadata] = useState(null);
  const [showCamera, setShowCamera] = useState(false);

  const [gettingLocation, setGettingLocation] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // GPS Location Handler
  const handleGetLocation = () => {
    if (!navigator.geolocation) {
      setErrorMsg('Geolocation is not supported by your browser.');
      return;
    }

    setGettingLocation(true);
    setErrorMsg('');

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude.toFixed(6));
        setLongitude(position.coords.longitude.toFixed(6));
        setGettingLocation(false);
        setSuccessMsg('GPS location captured successfully!');
        setTimeout(() => setSuccessMsg(''), 3000);
      },
      (error) => {
        setGettingLocation(false);
        setErrorMsg(`Location error: ${error.message}. Please enter coordinates manually or pin them on the map.`);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  // Map Picker Handler
  const handleMapLocationSelect = (coords) => {
    setLatitude(coords.lat.toString());
    setLongitude(coords.lng.toString());
  };

  // File Picker Handler with Client-side Magic/MIME & Size Validation
  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    if (selectedFiles.length + files.length > 5) {
      setErrorMsg('You can upload a maximum of 5 images per complaint.');
      return;
    }

    const validFiles = [];
    const validPreviews = [];
    let hasError = false;
    let isGpsMapCamera = false;
    let fileModTime = null;

    files.forEach((file) => {
      if (file.size > 10 * 1024 * 1024) {
        setErrorMsg(`File '${file.name}' exceeds the 10MB limit.`);
        hasError = true;
        return;
      }

      const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg'];
      if (!allowedTypes.includes(file.type.toLowerCase())) {
        setErrorMsg(`File '${file.name}' is not a valid JPG, PNG, or WEBP image.`);
        hasError = true;
        return;
      }

      if (file.name.toLowerCase().includes('gpsmapcamera') ||
          file.name.toLowerCase().includes('gps_map_camera') ||
          file.name.toLowerCase().includes('gps camera')) {
        isGpsMapCamera = true;
      }

      if (file.lastModified && !fileModTime) {
        fileModTime = new Date(file.lastModified).toISOString();
      }

      validFiles.push(file);
      validPreviews.push(URL.createObjectURL(file));
    });

    if (!hasError) {
      setErrorMsg('');
      setSelectedFiles((prev) => [...prev, ...validFiles]);
      setPreviews((prev) => [...prev, ...validPreviews]);

      const defaultSource = isGpsMapCamera ? 'GPS_MAP_CAMERA_FILE' : 'GALLERY_UPLOAD';

      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            setDeviceMetadata({
              deviceLatitude: null, // File uploads do not use browser position as photo capture GPS
              deviceLongitude: null,
              deviceAccuracy: pos.coords.accuracy,
              deviceGpsTimestamp: new Date(pos.timestamp || Date.now()).toISOString(),
              deviceCapturedAt: null, // Preserved null: file uploads do not fabricate capture time
              browserUploadLatitude: pos.coords.latitude,
              browserUploadLongitude: pos.coords.longitude,
              fileLastModifiedAt: fileModTime,
              captureSource: defaultSource
            });
          },
          () => {
            // Gracefully handle denied permission or unavailable GPS without blocking
            setDeviceMetadata({
              deviceLatitude: null,
              deviceLongitude: null,
              deviceAccuracy: null,
              deviceGpsTimestamp: null,
              deviceCapturedAt: null,
              browserUploadLatitude: null,
              browserUploadLongitude: null,
              fileLastModifiedAt: fileModTime,
              captureSource: defaultSource
            });
          },
          { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
        );
      } else {
        setDeviceMetadata({
          deviceLatitude: null,
          deviceLongitude: null,
          deviceAccuracy: null,
          deviceGpsTimestamp: null,
          deviceCapturedAt: null,
          browserUploadLatitude: null,
          browserUploadLongitude: null,
          fileLastModifiedAt: fileModTime,
          captureSource: defaultSource
        });
      }
    }
  };

  const handleRemoveImage = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
    setPreviews((prev) => prev.filter((_, i) => i !== index));
  };

  const handlePhotoCaptured = (captured) => {
    if (selectedFiles.length >= 5) {
      setErrorMsg('You can upload a maximum of 5 images per complaint.');
      setShowCamera(false);
      return;
    }
    setSelectedFiles((prev) => [...prev, captured.file]);
    setPreviews((prev) => [...prev, captured.previewUrl]);

    setDeviceMetadata({
      deviceLatitude: captured.deviceLatitude,
      deviceLongitude: captured.deviceLongitude,
      deviceAccuracy: captured.deviceAccuracy,
      deviceCapturedAt: captured.deviceCapturedAt,
      deviceGpsTimestamp: captured.deviceGpsTimestamp,
      fileLastModifiedAt: captured.fileLastModifiedAt,
      captureSource: captured.captureSource
    });

    if (captured.deviceLatitude && captured.deviceLongitude) {
      setLatitude(Number(captured.deviceLatitude).toFixed(6));
      setLongitude(Number(captured.deviceLongitude).toFixed(6));
      setSuccessMsg('Photo captured and report coordinates synchronized with GPS!');
      setTimeout(() => setSuccessMsg(''), 4000);
    }
    setShowCamera(false);
  };

  // Submit Handler
  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!title.trim() || !description.trim()) {
      setErrorMsg('Please fill in required fields: Title and Description.');
      return;
    }

    if (!latitude || !longitude) {
      setErrorMsg('Please provide valid Latitude and Longitude coordinates.');
      return;
    }

    const latNum = parseFloat(latitude);
    const lngNum = parseFloat(longitude);

    if (isNaN(latNum) || latNum < -90 || latNum > 90) {
      setErrorMsg('Latitude must be a valid number between -90 and 90.');
      return;
    }

    if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
      setErrorMsg('Longitude must be a valid number between -180 and 180.');
      return;
    }

    if (selectedFiles.length === 0) {
      setErrorMsg('Please upload at least one proof photo showing the plastic waste.');
      return;
    }

    setSubmitting(true);

    try {
      const isLiveCamera = deviceMetadata?.captureSource === 'IN_APP_CAMERA' || deviceMetadata?.captureSource === 'CAMERA';
      const dataPayload = {
        title: title.trim(),
        description: description.trim(),
        wasteType,
        estimatedSize,
        latitude: latNum,
        longitude: lngNum,
        addressText: addressText.trim() || undefined,
        deviceLatitude: isLiveCamera && deviceMetadata?.deviceLatitude != null ? parseFloat(deviceMetadata.deviceLatitude) : undefined,
        deviceLongitude: isLiveCamera && deviceMetadata?.deviceLongitude != null ? parseFloat(deviceMetadata.deviceLongitude) : undefined,
        deviceCapturedAt: isLiveCamera ? deviceMetadata?.deviceCapturedAt || undefined : undefined,
        captureSource: deviceMetadata?.captureSource || 'GALLERY_UPLOAD',
        deviceAccuracy: deviceMetadata?.deviceAccuracy != null ? parseFloat(deviceMetadata.deviceAccuracy) : undefined,
        deviceGpsTimestamp: deviceMetadata?.deviceGpsTimestamp || undefined,
        fileLastModifiedAt: deviceMetadata?.fileLastModifiedAt || undefined,
        browserUploadLatitude: deviceMetadata?.browserUploadLatitude != null ? parseFloat(deviceMetadata.browserUploadLatitude) : undefined,
        browserUploadLongitude: deviceMetadata?.browserUploadLongitude != null ? parseFloat(deviceMetadata.browserUploadLongitude) : undefined
      };

      const result = await complaintsApi.submitWithPhotos(dataPayload, selectedFiles);
      setSuccessMsg(`Complaint '${result.title}' submitted successfully! Redirecting...`);
      setTimeout(() => {
        navigate('/citizen/complaints');
      }, 1500);
    } catch (err) {
      setErrorMsg(err.message || 'Submission failed. Please check inputs and ensure backend is running.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="main-content">
      <div style={{ maxWidth: '860px', margin: '0 auto' }}>
        <div style={{ marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.5rem' }}>Report Plastic Waste</h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Provide photos and location details so administrators can verify and dispatch cleanup volunteers.
          </p>
        </div>

        {errorMsg && (
          <div style={{
            background: 'rgba(244, 63, 94, 0.12)',
            border: '1px solid rgba(244, 63, 94, 0.35)',
            color: '#f43f5e',
            padding: '1rem',
            borderRadius: 'var(--radius-sm)',
            fontSize: '0.9rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            marginBottom: '1.5rem'
          }}>
            <AlertCircle size={20} />
            <span>{errorMsg}</span>
          </div>
        )}

        {successMsg && (
          <div style={{
            background: 'rgba(16, 185, 129, 0.15)',
            border: '1px solid rgba(16, 185, 129, 0.4)',
            color: '#34d399',
            padding: '1rem',
            borderRadius: 'var(--radius-sm)',
            fontSize: '0.9rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            marginBottom: '1.5rem'
          }}>
            <CheckCircle size={20} />
            <span>{successMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="glass-panel" style={{ padding: '2.5rem 2rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Title */}
          <div>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
              Report Title *
            </label>
            <input
              type="text"
              required
              placeholder="e.g., Heavy plastic bottle dumping along lakeshore"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{
                width: '100%',
                padding: '0.8rem 1rem',
                borderRadius: 'var(--radius-sm)',
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid var(--border-subtle)',
                color: 'var(--text-main)',
                fontSize: '0.95rem'
              }}
            />
          </div>

          {/* Description */}
          <div>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
              Description & Accessibility Details *
            </label>
            <textarea
              required
              rows={3}
              placeholder="Describe the type of plastic waste, how accessible the site is, and whether safety gear is needed..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              style={{
                width: '100%',
                padding: '0.8rem 1rem',
                borderRadius: 'var(--radius-sm)',
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid var(--border-subtle)',
                color: 'var(--text-main)',
                fontSize: '0.95rem',
                fontFamily: 'inherit',
                resize: 'vertical'
              }}
            />
          </div>

          {/* Category & Estimated Size */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                Waste Classification
              </label>
              <select
                value={wasteType}
                onChange={(e) => setWasteType(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.8rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                  background: '#0f172a',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-main)',
                  fontSize: '0.95rem'
                }}
              >
                <option value="PLASTIC_BOTTLES">Plastic Bottles (PET)</option>
                <option value="SINGLE_USE_BAGS">Single-Use Carry Bags</option>
                <option value="MICROPLASTICS_CONTAINERS">Food Containers & Straws</option>
                <option value="INDUSTRIAL_PLASTIC">Industrial Packaging / Tarps</option>
                <option value="PLASTIC_MIXED">Mixed Plastic Debris</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                Estimated Accumulation Size
              </label>
              <select
                value={estimatedSize}
                onChange={(e) => setEstimatedSize(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.8rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                  background: '#0f172a',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-main)',
                  fontSize: '0.95rem'
                }}
              >
                <option value="SMALL_BAG">Small (1-2 garbage bags)</option>
                <option value="MEDIUM_PILE">Medium (Scattered pile, ~10kg)</option>
                <option value="LARGE_DUMP">Large (Truck load / open dump site)</option>
              </select>
            </div>
          </div>

          {/* Address */}
          <div>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
              Address / Landmark
            </label>
            <input
              type="text"
              placeholder="e.g., Near South Gate, Lakeview Promenade"
              value={addressText}
              onChange={(e) => setAddressText(e.target.value)}
              style={{
                width: '100%',
                padding: '0.8rem 1rem',
                borderRadius: 'var(--radius-sm)',
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid var(--border-subtle)',
                color: 'var(--text-main)',
                fontSize: '0.95rem'
              }}
            />
          </div>

          {/* Map Location & GPS */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.6rem', flexWrap: 'wrap', gap: '0.5rem' }}>
              <label style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                Geospatial Location *
              </label>
              <button
                type="button"
                onClick={handleGetLocation}
                disabled={gettingLocation}
                className="service-btn"
                style={{ fontSize: '0.8rem', padding: '0.35rem 0.75rem' }}
              >
                <MapPin size={14} />
                {gettingLocation ? 'Locating...' : 'Use My GPS Location'}
              </button>
            </div>

            <div style={{ marginBottom: '0.75rem' }}>
              <MapView
                height="280px"
                pickerMode={true}
                selectedLocation={{ lat: parseFloat(latitude) || 12.9716, lng: parseFloat(longitude) || 77.5946 }}
                center={[parseFloat(latitude) || 12.9716, parseFloat(longitude) || 77.5946]}
                onLocationSelect={handleMapLocationSelect}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <input
                type="number"
                step="any"
                required
                placeholder="Latitude"
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: 'var(--radius-sm)',
                  background: 'rgba(255, 255, 255, 0.04)',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-main)',
                  fontSize: '0.88rem'
                }}
              />
              <input
                type="number"
                step="any"
                required
                placeholder="Longitude"
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: 'var(--radius-sm)',
                  background: 'rgba(255, 255, 255, 0.04)',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-main)',
                  fontSize: '0.88rem'
                }}
              />
            </div>
          </div>

          {/* Photo Upload & Thumbnails */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.6rem', flexWrap: 'wrap', gap: '0.5rem' }}>
              <label style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                Proof Photos (Max 5 photos, JPG/PNG/WEBP, up to 10MB each) *
              </label>
              {deviceMetadata?.deviceLatitude && (
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  padding: '0.2rem 0.6rem',
                  borderRadius: '12px',
                  background: 'rgba(16, 185, 129, 0.15)',
                  border: '1px solid rgba(16, 185, 129, 0.3)',
                  color: '#34d399',
                  fontSize: '0.75rem',
                  fontWeight: 600
                }}>
                  <ShieldCheck size={14} />
                  GPS Attached: {Number(deviceMetadata.deviceLatitude).toFixed(4)}, {Number(deviceMetadata.deviceLongitude).toFixed(4)}
                </div>
              )}
            </div>

            {/* Action buttons for photo capture vs file select */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
              <button
                type="button"
                onClick={() => setShowCamera(true)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                  padding: '1rem',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid rgba(14, 165, 233, 0.4)',
                  background: 'rgba(14, 165, 233, 0.1)',
                  color: '#38bdf8',
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
              >
                <Camera size={20} />
                <span>Take Live Photo (In-App Camera)</span>
              </button>

              <div>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  onChange={handleFileChange}
                  id="report-photo-picker"
                  style={{ display: 'none' }}
                />
                <label
                  htmlFor="report-photo-picker"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.5rem',
                    padding: '1rem',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px dashed rgba(16, 185, 129, 0.4)',
                    background: 'rgba(16, 185, 129, 0.05)',
                    color: '#34d399',
                    fontWeight: 600,
                    fontSize: '0.9rem',
                    cursor: 'pointer',
                    height: '100%',
                    boxSizing: 'border-box'
                  }}
                >
                  <Upload size={20} />
                  <span>Choose from Device Files</span>
                </label>
              </div>
            </div>

            {previews.length > 0 && (
              <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                {previews.map((url, idx) => (
                  <div
                    key={idx}
                    style={{
                      position: 'relative',
                      width: '100px',
                      height: '100px',
                      borderRadius: 'var(--radius-sm)',
                      overflow: 'hidden',
                      border: '1px solid var(--border-subtle)'
                    }}
                  >
                    <img src={url} alt={`Preview ${idx + 1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    <button
                      type="button"
                      onClick={() => handleRemoveImage(idx)}
                      style={{
                        position: 'absolute',
                        top: '4px',
                        right: '4px',
                        background: 'rgba(0,0,0,0.75)',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '50%',
                        width: '22px',
                        height: '22px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                      title="Remove image"
                    >
                      <X size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Submit Action */}
          <button
            type="submit"
            disabled={submitting}
            className="service-btn"
            style={{
              background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))',
              color: '#000',
              fontWeight: 800,
              padding: '0.95rem',
              fontSize: '1rem',
              border: 'none',
              borderRadius: 'var(--radius-sm)',
              boxShadow: '0 0 20px var(--primary-glow)',
              cursor: submitting ? 'not-allowed' : 'pointer',
              opacity: submitting ? 0.75 : 1,
              marginTop: '0.5rem'
            }}
          >
            {submitting ? 'Uploading to Cloudinary & Submitting...' : 'Submit Plastic Waste Report'}
            {!submitting && <ArrowRight size={18} />}
          </button>
        </form>

        {showCamera && (
          <InAppCameraCapture
            onPhotoCaptured={handlePhotoCaptured}
            onClose={() => setShowCamera(false)}
          />
        )}
      </div>
    </div>
  );
}
