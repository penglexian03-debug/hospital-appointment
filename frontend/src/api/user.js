import http from './http'

export function getProfileApi() {
  return http.get('/user/profile')
}

export function updateProfileApi(data) {
  return http.put('/user/profile', data)
}
