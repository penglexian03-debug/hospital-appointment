import http from './http'

export function getMyAppointmentsApi(params) {
  return http.get('/appointments/my', { params })
}

export function createAppointmentApi(data) {
  return http.post('/appointments', data)
}

export function getAppointmentDetailApi(id) {
  return http.get(`/appointments/${id}`)
}

export function cancelAppointmentApi(id, data) {
  return http.put(`/appointments/${id}/cancel`, data)
}
