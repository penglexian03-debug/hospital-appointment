import http from './http'

export function getDashboardStatsApi() {
  return http.get('/admin/dashboard/stats')
}

export function getAdminDepartmentsApi(params) {
  return http.get('/admin/departments', { params })
}

export function getAdminDepartmentApi(id) {
  return http.get(`/admin/departments/${id}`)
}

export function createDepartmentApi(data) {
  return http.post('/admin/departments', data)
}

export function updateDepartmentApi(id, data) {
  return http.put(`/admin/departments/${id}`, data)
}

export function updateDepartmentStatusApi(id, status) {
  return http.put(`/admin/departments/${id}/status`, null, { params: { status } })
}

export function deleteDepartmentApi(id) {
  return http.delete(`/admin/departments/${id}`)
}

export function getAdminDoctorsApi(params) {
  return http.get('/admin/doctors', { params })
}

export function getAdminDoctorApi(id) {
  return http.get(`/admin/doctors/${id}`)
}

export function createDoctorApi(data) {
  return http.post('/admin/doctors', data)
}

export function updateDoctorApi(id, data) {
  return http.put(`/admin/doctors/${id}`, data)
}

export function updateDoctorStatusApi(id, status) {
  return http.put(`/admin/doctors/${id}/status`, null, { params: { status } })
}

export function deleteDoctorApi(id) {
  return http.delete(`/admin/doctors/${id}`)
}

export function getAdminSchedulesApi(params) {
  return http.get('/admin/schedules', { params })
}

export function getAdminScheduleApi(id) {
  return http.get(`/admin/schedules/${id}`)
}

export function createScheduleApi(data) {
  return http.post('/admin/schedules', data)
}

export function updateScheduleApi(id, data) {
  return http.put(`/admin/schedules/${id}`, data)
}

export function updateScheduleStatusApi(id, status) {
  return http.put(`/admin/schedules/${id}/status`, null, { params: { status } })
}

export function deleteScheduleApi(id) {
  return http.delete(`/admin/schedules/${id}`)
}

export function getAdminAppointmentsApi(params) {
  return http.get('/admin/appointments', { params })
}

export function updateAppointmentStatusApi(id, status) {
  return http.put(`/admin/appointments/${id}/status`, null, { params: { status } })
}

export function getAdminUsersApi(params) {
  return http.get('/admin/users', { params })
}

export function getAdminUserApi(id) {
  return http.get(`/admin/users/${id}`)
}

export function updateUserStatusApi(id, status) {
  return http.put(`/admin/users/${id}/status`, null, { params: { status } })
}
