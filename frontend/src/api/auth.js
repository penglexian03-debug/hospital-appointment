import http from './http'

export function loginApi(data) {
  return http.post('/auth/login', data)
}

export function registerApi(data) {
  return http.post('/auth/register', data)
}

export function logoutApi() {
  return http.post('/auth/logout')
}
