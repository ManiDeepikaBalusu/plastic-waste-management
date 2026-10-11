const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

class ApiError extends Error {
  constructor(message, status, validationErrors = null, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.validationErrors = validationErrors;
    this.data = data;
  }
}

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const token = localStorage.getItem('token');

  const headers = {
    ...(options.headers || {})
  };

  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Include credentials for HttpOnly cookie exchange
  const config = {
    ...options,
    headers,
    credentials: 'include'
  };

  try {
    const res = await fetch(url, config);

    if (res.status === 204) {
      return null;
    }

    const contentType = res.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const data = isJson ? await res.json() : await res.text();

    if (!res.ok) {
      let errorMessage = data?.message || data?.error || `Request failed with status ${res.status}`;
      if (data?.validationErrors && typeof data.validationErrors === 'object') {
        const details = Object.values(data.validationErrors).filter(Boolean);
        if (details.length > 0) {
          errorMessage = details.join('. ');
        }
      }
      throw new ApiError(errorMessage, res.status, data?.validationErrors, data);
    }

    return data;
  } catch (err) {
    if (err instanceof ApiError) {
      throw err;
    }
    throw new ApiError(err.message || 'Network connection failed. Is the backend running?', 0);
  }
}

export const api = {
  get: (endpoint) => request(endpoint, { method: 'GET' }),
  post: (endpoint, body) => request(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  }),
  put: (endpoint, body) => request(endpoint, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  }),
  patch: (endpoint, body) => request(endpoint, {
    method: 'PATCH',
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  }),
  delete: (endpoint) => request(endpoint, { method: 'DELETE' }),

  postForm: (endpoint, formData) => request(endpoint, {
    method: 'POST',
    body: formData // Let browser set boundary automatically
  }),
  patchForm: (endpoint, formData) => request(endpoint, {
    method: 'PATCH',
    body: formData
  })
};

// Domain-specific API abstractions
export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (payload) => api.post('/auth/register', payload),
  getMe: () => api.get('/users/me'),
  logout: () => api.post('/auth/logout', {})
};

export const complaintsApi = {
  submitWithPhotos: (complaintData, files = []) => {
    const formData = new FormData();
    const dataBlob = new Blob([JSON.stringify(complaintData)], { type: 'application/json' });
    formData.append('data', dataBlob);
    
    files.forEach(file => {
      formData.append('files', file);
    });

    return api.postForm('/complaints/submit-with-photos', formData);
  },
  submitJson: (payload) => api.post('/complaints', payload),
  getMy: () => api.get('/complaints/my'),
  getById: (id) => api.get(`/complaints/${id}`),
  getAll: (status) => api.get(`/complaints${status ? `?status=${encodeURIComponent(status)}` : ''}`),
  review: (id, remarks) => api.patch(`/complaints/${id}/review${remarks ? `?remarks=${encodeURIComponent(remarks)}` : ''}`),
  verify: (id, remarks) => api.patch(`/complaints/${id}/verify${remarks ? `?remarks=${encodeURIComponent(remarks)}` : ''}`),
  reject: (id, rejectionReason) => api.patch(`/complaints/${id}/reject`, { rejectionReason }),
  cancel: (id, cancellationReason) => api.patch(`/complaints/${id}/cancel`, { cancellationReason })
};

export const volunteersApi = {
  updateStatus: (statusData) => api.put('/volunteer/status', statusData),
  getProfile: () => api.get('/volunteer/profile'),
  getAvailable: () => api.get('/volunteer/available')
};

export const assignmentsApi = {
  assign: (data) => api.post('/assignments', data),
  getMyTasks: () => api.get('/assignments/my-tasks'),
  getById: (id) => api.get(`/assignments/${id}`),
  accept: (id) => api.patch(`/assignments/${id}/accept`),
  decline: (id, reason) => api.patch(`/assignments/${id}/decline${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),
  start: (id) => api.patch(`/assignments/${id}/start`),
  resolveWithProof: (id, notes, files = [], metadata = {}) => {
    const formData = new FormData();
    if (notes) formData.append('notes', notes);
    if (metadata.deviceLatitude) formData.append('deviceLatitude', metadata.deviceLatitude);
    if (metadata.deviceLongitude) formData.append('deviceLongitude', metadata.deviceLongitude);
    if (metadata.deviceCapturedAt) formData.append('deviceCapturedAt', metadata.deviceCapturedAt);
    if (metadata.captureSource) formData.append('captureSource', metadata.captureSource);
    files.forEach(file => formData.append('files', file));
    return api.postForm(`/assignments/${id}/resolve`, formData);
  }
};

export const verificationsApi = {
  getComplaintVerifications: (complaintId) => api.get(`/verifications/complaint/${complaintId}`),
  getAssignmentVerifications: (assignmentId) => api.get(`/verifications/assignment/${assignmentId}`),
  submitDecision: (verificationId, decisionData) => api.post(`/verifications/${verificationId}/decision`, decisionData)
};

export const notificationsApi = {
  getAll: () => api.get('/notifications'),
  markAsRead: (id) => api.patch(`/notifications/${id}/read`)
};

export default api;

