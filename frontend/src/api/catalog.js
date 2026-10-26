import http from './http'

export function getDepartmentsApi() {
  return http.get('/public/departments')
}

export function getDepartmentDetailApi(id) {
  return http.get(`/public/departments/${id}`)
}

export function getDoctorsApi(params) {
  return http.get('/public/doctors', { params })
}

export function getDoctorDetailApi(id) {
  return http.get(`/public/doctors/${id}`)
}

export function getSchedulesApi(params) {
  return http.get('/public/schedules', { params })
}

export function getScheduleDetailApi(id) {
  return http.get(`/public/schedules/${id}`)
}
