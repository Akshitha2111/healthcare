import api from './api'

const data = (promise) => promise.then((r) => r.data)

export const authApi = {
  register: (body) => data(api.post('/api/auth/register', body)),
  login: (body) => data(api.post('/api/auth/login', body)),
  logout: () => data(api.post('/api/auth/logout')),
  me: () => data(api.get('/api/auth/me')),
}

export const patientApi = {
  profile: () => data(api.get('/api/patients/me')),
  updateProfile: (body) => data(api.put('/api/patients/me', body)),
  summary: () => data(api.get('/api/patients/me/summary')),
}

export const predictionApi = {
  symptoms: () => data(api.get('/api/symptoms')),
  create: (symptoms) => data(api.post('/api/predictions', { symptoms })),
  mine: (params) => data(api.get('/api/predictions/my', { params })),
  get: (id) => data(api.get(`/api/predictions/${id}`)),
}

export const appointmentApi = {
  doctors: () => data(api.get('/api/doctors')),
  slots: (doctorId, date) => data(api.get(`/api/doctors/${doctorId}/slots`, { params: { date } })),
  mine: (params) => data(api.get('/api/appointments/my', { params })),
  book: (body) => data(api.post('/api/appointments', body)),
  cancel: (id) => data(api.patch(`/api/appointments/${id}/cancel`)),
}

export const adminApi = {
  dashboard: () => data(api.get('/api/admin/dashboard')),
  analytics: () => data(api.get('/api/admin/analytics')),
  appointments: (params) => data(api.get('/api/admin/appointments', { params })),
  setStatus: (id, status) => data(api.patch(`/api/admin/appointments/${id}/status`, { status })),
  doctors: () => data(api.get('/api/admin/doctors')),
  createDoctor: (body) => data(api.post('/api/admin/doctors', body)),
  updateDoctor: (id, body) => data(api.put(`/api/admin/doctors/${id}`, body)),
}
