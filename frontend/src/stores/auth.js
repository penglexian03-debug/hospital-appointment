import { computed, reactive, readonly } from 'vue'
import { loginApi, logoutApi, registerApi } from '@/api/auth'
import { getProfileApi, updateProfileApi } from '@/api/user'
import {
  clearSession,
  getStoredUser,
  getToken,
  saveSession,
  saveStoredUser,
} from '@/utils/session'

const state = reactive({
  token: getToken(),
  user: getStoredUser(),
  initialized: false,
})

let verificationTask = null

function setSession(loginResult) {
  state.token = loginResult.token
  state.user = loginResult.user
  state.initialized = true
  saveSession(loginResult.token, loginResult.user)
}

function clearAuth() {
  state.token = ''
  state.user = null
  state.initialized = true
  clearSession()
}

async function ensureSession() {
  if (!state.token) {
    state.initialized = true
    return false
  }
  if (state.initialized && state.user) {
    return true
  }
  if (verificationTask) {
    return verificationTask
  }

  verificationTask = (async () => {
    try {
      const user = await getProfileApi()
      state.user = user
      state.initialized = true
      saveStoredUser(user)
      return true
    } catch {
      clearAuth()
      return false
    } finally {
      verificationTask = null
    }
  })()

  return verificationTask
}

async function login(form) {
  const result = await loginApi(form)
  setSession(result)
  return result.user
}

async function register(form) {
  const result = await registerApi(form)
  setSession(result)
  return result.user
}

async function updateProfile(form) {
  const user = await updateProfileApi(form)
  state.user = user
  saveStoredUser(user)
  return user
}

async function logout() {
  try {
    if (state.token) {
      await logoutApi()
    }
  } catch {
    // The local session must still be cleared if the server is unavailable.
  } finally {
    clearAuth()
  }
}

const isAuthenticated = computed(() => Boolean(state.token))
const isAdmin = computed(() => state.user?.role === 2)
const displayName = computed(() => state.user?.realName || state.user?.username || '用户')

export function useAuthStore() {
  return {
    state: readonly(state),
    isAuthenticated,
    isAdmin,
    displayName,
    ensureSession,
    login,
    register,
    updateProfile,
    logout,
  }
}
