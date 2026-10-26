import axios from 'axios'
import { clearSession, getToken } from '@/utils/session'

const aiHttp = axios.create({
  baseURL: import.meta.env.VITE_AI_API_BASE_URL || '/ai-api',
  timeout: 70000,
})

aiHttp.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

aiHttp.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 200) {
        return payload.data
      }
      const error = new Error(payload.message || 'AI 请求处理失败')
      error.code = payload.code
      return Promise.reject(error)
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const payload = error.response?.data
    let message = payload?.message || payload?.detail
    if (!message) {
      if (!error.response) {
        message = '无法连接 AI 服务，请确认 ai-service 已启动'
      } else if (error.code === 'ECONNABORTED') {
        message = 'AI 服务响应超时，请稍后重试'
      } else {
        message = error.message || 'AI 请求失败'
      }
    }

    if (status === 401) {
      clearSession()
      window.dispatchEvent(new Event('auth:expired'))
    }

    const normalizedError = new Error(message)
    normalizedError.code = payload?.code || status
    normalizedError.status = status
    return Promise.reject(normalizedError)
  },
)

export function chatWithAiApi(data) {
  return aiHttp.post('/chat', data)
}

export function confirmAiActionApi(data) {
  return aiHttp.post('/confirm', data)
}

export function deleteAiConversationApi(conversationId) {
  return aiHttp.delete('/conversations/' + encodeURIComponent(conversationId))
}
