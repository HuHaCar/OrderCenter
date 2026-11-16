import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginRequest, logout as logoutRequest } from '@/api/auth'
import {
  clearAccessToken,
  refreshAccessToken,
  setAccessToken,
} from '@/auth/session'
import type { AuthUser, LoginPayload } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const initialized = ref(false)
  let bootstrapPromise: Promise<void> | null = null

  const isAuthenticated = computed(() => user.value !== null)
  const displayName = computed(() => user.value?.displayName || '未登录')
  const roleLabel = computed(() =>
    user.value?.role === 'ADMIN' ? '管理员' : '操作员',
  )

  async function login(payload: LoginPayload) {
    const result = await loginRequest(payload)
    setAccessToken(result.accessToken)
    user.value = result.user
    initialized.value = true
    return result
  }

  /**
   * 页面刷新后 access token 不在内存里，用 HttpOnly Cookie 恢复登录态。
   */
  async function bootstrap() {
    if (initialized.value) return
    if (bootstrapPromise) return bootstrapPromise

    bootstrapPromise = (async () => {
      try {
        if (!user.value) {
          const result = await refreshAccessToken()
          user.value = result.user
        }
      } catch {
        clearAccessToken()
        user.value = null
      } finally {
        initialized.value = true
        bootstrapPromise = null
      }
    })()

    return bootstrapPromise
  }

  async function logout() {
    await logoutRequest()
    clear()
    initialized.value = true
  }

  function clear() {
    clearAccessToken()
    user.value = null
  }

  return {
    user,
    initialized,
    isAuthenticated,
    displayName,
    roleLabel,
    login,
    bootstrap,
    logout,
    clear,
  }
})
