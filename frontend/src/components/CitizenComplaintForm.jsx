import React, { useState } from 'react'

export default function CitizenComplaintForm({ onComplaintSubmitted }) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [wasteType, setWasteType] = useState('PLASTIC_BOTTLES')
  const [estimatedSize, setEstimatedSize] = useState('MEDIUM_PILE')
  const [addressText, setAddressText] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [selectedFiles, setSelectedFiles] = useState([])
  const [previews, setPreviews] = useState([])

  const [gettingLocation, setGettingLocation] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  // GPS Location Handler
  const handleGetLocation = () => {
    if (!navigator.geolocation) {
      setErrorMsg('Geolocation is not supported by your browser.')
      return
    }

    setGettingLocation(true)
    setErrorMsg('')

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude.toFixed(7))
        setLongitude(position.coords.longitude.toFixed(7))
        setGettingLocation(false)
        setSuccessMsg('GPS coordinates captured successfully!')
        setTimeout(() => setSuccessMsg(''), 3000)
      },
      (error) => {
        setGettingLocation(false)
        setErrorMsg(`Location error: ${error.message}. Please enter coordinates manually.`)
      },
      { enableHighAccuracy: true, timeout: 10000 }
    )
  }

  // File Picker Handler with Client-side Magic/MIME & Size Validation
  const handleFileChange = (e) => {
    const files = Array.from(e.target.files)
    if (!files.length) return

    if (selectedFiles.length + files.length > 5) {
      setErrorMsg('You can upload a maximum of 5 images per complaint.')
      return
    }

    const validFiles = []
    const validPreviews = []
    let hasError = false

    files.forEach((file) => {
      // 10 MB limit
      if (file.size > 10 * 1024 * 1024) {
        setErrorMsg(`File '${file.name}' exceeds the 10MB limit.`)
        hasError = true
        return
      }

      const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg']
      if (!allowedTypes.includes(file.type.toLowerCase())) {
        setErrorMsg(`File '${file.name}' is not a valid JPG, PNG, or WEBP image.`)
        hasError = true
        return
      }

      validFiles.push(file)
      validPreviews.push(URL.createObjectURL(file))
    })

    if (!hasError) {
      setErrorMsg('')
      setSelectedFiles((prev) => [...prev, ...validFiles])
      setPreviews((prev) => [...prev, ...validPreviews])
    }
  }

  const handleRemoveImage = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index))
    setPreviews((prev) => prev.filter((_, i) => i !== index))
  }

  // Submit Handler
  const handleSubmit = async (e) => {
    e.preventDefault()
    setErrorMsg('')
    setSuccessMsg('')

    if (!title.trim() || !description.trim()) {
      setErrorMsg('Please fill in required fields: Title and Description.')
      return
    }

    if (!latitude || !longitude) {
      setErrorMsg('Please provide valid Latitude and Longitude coordinates (use GPS button).')
      return
    }

    const latNum = parseFloat(latitude)
    const lngNum = parseFloat(longitude)

    if (isNaN(latNum) || latNum < -90 || latNum > 90) {
      setErrorMsg('Latitude must be a valid number between -90 and 90.')
      return
    }

    if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
      setErrorMsg('Longitude must be a valid number between -180 and 180.')
      return
    }

    setSubmitting(true)

    try {
      // Create FormData payload
      const formData = new FormData()
      const dataPayload = {
        title: title.trim(),
        description: description.trim(),
        wasteType,
        estimatedSize,
        latitude: latNum,
        longitude: lngNum,
        addressText: addressText.trim()
      }

      formData.append(
        'data',
        new Blob([JSON.stringify(dataPayload)], { type: 'application/json' })
      )

      selectedFiles.forEach((file) => {
        formData.append('files', file)
      })

      const response = await fetch('http://localhost:8080/api/v1/complaints/submit-with-photos', {
        method: 'POST',
        headers: {
          // Token will be sent if logged in via Bearer or HttpOnly cookie
          ...(localStorage.getItem('jwt_token')
            ? { Authorization: `Bearer ${localStorage.getItem('jwt_token')}` }
            : {})
        },
        body: formData
      })

      if (response.ok) {
        const result = await response.json()
        setSuccessMsg(`Complaint '${result.title}' filed successfully! (ID: ${result.id})`)
        setTitle('')
        setDescription('')
        setAddressText('')
        setLatitude('')
        setLongitude('')
        setSelectedFiles([])
        setPreviews([])
        if (onComplaintSubmitted) onComplaintSubmitted(result)
      } else {
        const errorData = await response.json().catch(() => ({}))
        setErrorMsg(errorData.message || `Submission failed with HTTP ${response.status}`)
      }
    } catch (err) {
      setErrorMsg(`Network error: ${err.message}. Ensure backend is running.`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="glass-panel" style={{ padding: '2rem', maxWidth: '720px', margin: '0 auto' }}>
      <h2 className="section-title">Report Plastic Waste Hotspot</h2>
      <p className="section-desc">Snap photos, add GPS coordinates, and submit for volunteer cleanup dispatch.</p>

      {errorMsg && (
        <div style={{ padding: '0.85rem 1.25rem', borderRadius: '8px', background: 'rgba(244, 63, 94, 0.15)', border: '1px solid rgba(244, 63, 94, 0.3)', color: '#fb7185', marginBottom: '1.25rem', fontSize: '0.9rem' }}>
          ⚠️ {errorMsg}
        </div>
      )}

      {successMsg && (
        <div style={{ padding: '0.85rem 1.25rem', borderRadius: '8px', background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.3)', color: '#34d399', marginBottom: '1.25rem', fontSize: '0.9rem' }}>
          ✅ {successMsg}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Title */}
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem', color: '#94a3b8' }}>
            Complaint Title *
          </label>
          <input
            type="text"
            required
            placeholder="e.g. Plastic bottle accumulation near Central Lake"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', color: '#fff', fontSize: '0.95rem' }}
          />
        </div>

        {/* Description */}
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem', color: '#94a3b8' }}>
            Description & Waste Details *
          </label>
          <textarea
            required
            rows={3}
            placeholder="Describe the waste type, accessibility, and severity..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', color: '#fff', fontSize: '0.95rem' }}
          />
        </div>

        {/* Waste Type & Estimated Size */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem', color: '#94a3b8' }}>
              Waste Category
            </label>
            <select
              value={wasteType}
              onChange={(e) => setWasteType(e.target.value)}
              style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', color: '#fff', fontSize: '0.95rem' }}
            >
              <option value="PLASTIC_BOTTLES">Plastic Bottles / Packaging</option>
              <option value="SINGLE_USE_BAGS">Single-Use Plastic Bags</option>
              <option value="MICROPLASTICS_CONTAINERS">Food Containers & Cutlery</option>
              <option value="INDUSTRIAL_PLASTIC">Industrial / Construction Plastic</option>
              <option value="PLASTIC_MIXED">Mixed Plastic Waste</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem', color: '#94a3b8' }}>
              Estimated Dump Size
            </label>
            <select
              value={estimatedSize}
              onChange={(e) => setEstimatedSize(e.target.value)}
              style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', color: '#fff', fontSize: '0.95rem' }}
            >
              <option value="SMALL_BAG">Small (1-2 Bags)</option>
              <option value="MEDIUM_PILE">Medium (Scatter Pile)</option>
              <option value="LARGE_DUMP">Large (Truck / Illegal Dump Site)</option>
            </select>
          </div>
        </div>

        {/* Address */}
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem', color: '#94a3b8' }}>
            Street Address / Landmark
          </label>
          <input
            type="text"
            placeholder="e.g. Near 4th Cross Road, Lake view park"
            value={addressText}
            onChange={(e) => setAddressText(e.target.value)}
            style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', color: '#fff', fontSize: '0.95rem' }}
          />
        </div>

        {/* Geolocation Controls */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#94a3b8' }}>GPS Location Coordinates *</label>
            <button
              type="button"
              onClick={handleGetLocation}
              disabled={gettingLocation}
              style={{ padding: '0.35rem 0.75rem', borderRadius: '6px', background: 'rgba(6, 182, 212, 0.15)', border: '1px solid rgba(6, 182, 212, 0.3)', color: '#38bdf8', fontSize: '0.8rem', fontWeight: 600, cursor: 'pointer' }}
            >
              {gettingLocation ? '📍 Locating...' : '📍 Use My GPS Location'}
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <input
              type="number"
              step="any"
              required
              placeholder="Latitude (-90 to 90)"
              value={latitude}
              onChange={(e) => setLatitude(e.target.value)}
              style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', color: '#fff', fontSize: '0.95rem' }}
            />
            <input
              type="number"
              step="any"
              required
              placeholder="Longitude (-180 to 180)"
              value={longitude}
              onChange={(e) => setLongitude(e.target.value)}
              style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', color: '#fff', fontSize: '0.95rem' }}
            />
          </div>
        </div>

        {/* Photo Upload & Previews */}
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem', color: '#94a3b8' }}>
            Initial Proof Photos (Max 5 images, <= 10MB each)
          </label>
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            onChange={handleFileChange}
            style={{ display: 'none' }}
            id="file-upload-input"
          />
          <label
            htmlFor="file-upload-input"
            style={{
              display: 'block',
              padding: '1.5rem',
              borderRadius: '8px',
              border: '2px dashed rgba(16, 185, 129, 0.4)',
              background: 'rgba(16, 185, 129, 0.05)',
              textAlign: 'center',
              cursor: 'pointer',
              color: '#34d399',
              fontWeight: 600,
              fontSize: '0.9rem'
            }}
          >
            📸 Click or Drag to Select Photos (JPG, PNG, WEBP)
          </label>

          {/* Image Previews */}
          {previews.length > 0 && (
            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginTop: '1rem' }}>
              {previews.map((url, idx) => (
                <div key={idx} style={{ position: 'relative', width: '90px', height: '90px', borderRadius: '8px', overflow: 'hidden', border: '1px solid rgba(255,255,255,0.2)' }}>
                  <img src={url} alt={`Preview ${idx + 1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  <button
                    type="button"
                    onClick={() => handleRemoveImage(idx)}
                    style={{ position: 'absolute', top: '4px', right: '4px', background: 'rgba(0,0,0,0.7)', color: '#fff', border: 'none', borderRadius: '50%', width: '20px', height: '20px', cursor: 'pointer', fontSize: '0.75rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={submitting}
          className="service-btn"
          style={{ padding: '0.85rem', background: 'linear-gradient(135deg, #10b981, #06b6d4)', color: '#fff', fontSize: '1rem', border: 'none', borderRadius: '8px', fontWeight: 700, cursor: 'pointer' }}
        >
          {submitting ? 'Submitting Report & Uploading Photos...' : '🚀 Submit Plastic Waste Report'}
        </button>
      </form>
    </div>
  )
}
