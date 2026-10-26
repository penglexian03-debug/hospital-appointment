import axios from 'axios'
import { clearSession, getToken } from '@/utils/session'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 200) {
        return payload.data
      }
      const error = new Error(payload.message || '请求处理失败')
      error.code = payload.code
      error.data = payload.data
      return Promise.reject(error)
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const payload = error.response?.data
    let message = payload?.message
    if (!message) {
      if (!error.response) {
        message = '无法连接后端服务，请确认后端已经启动'
      } else if (status === 500) {
        message = '服务暂时不可用，请稍后重试'
      } else if (error.code === 'ECONNABORTED') {
        message = '请求超时，请稍后重试'
      } else {
        message = error.message || '网络请求失败'
      }
    }

    if (status === 401) {
      clearSession()
      window.dispatchEvent(new Event('auth:expired'))
    }

    const normalizedError = new Error(message)
    normalizedError.code = payload?.code || status
    normalizedError.status = status
    normalizedError.data = payload?.data
    return Promise.reject(normalizedError)
  },
)

export default http
