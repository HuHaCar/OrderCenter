import axios from 'axios'
import { rawHttp } from '@/api/raw'
import type { ApiResponse } from '@/api/http'
import type { AuthTokenResult } from '@/types/auth'

let accessToken: string | null = null
let sessionExpiredHandler: (() => void) | null = null

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token: string) {
  accessToken = token
}

export function clearAccessToken() {
  accessToken = null
}

export function setSessionExpiredHandler(handler: () => void) {
  sessionExpiredHandler = handler
}

export function notifySessionExpired() {
  sessionExpiredHandler?.()
}

/**
 * 用 HttpOnly Cookie 里的 refresh token 换取新的 access token。
 * 这个请求不能经过 http.ts 的 401 刷新队列。
 */
export async function refreshAccessToken(): Promise<AuthTokenResult> {
  try {
    const { data } = await rawHttp.post<ApiResponse<AuthTokenResult>>('/auth/refresh')
    if (data.code !== 0) {
      throw new Error(data.message || '登录已过期，请重新登录')
    }
    accessToken = data.data.accessToken
    return data.data
  } catch (error) {
    accessToken = null

    if (axios.isAxiosError(error)) {
      if (!error.response) {
        throw new Error('无法连接后端服务，请确认 8080 端口上的 Spring Boot 已启动')
      }
      const body = error.response.data as { message?: string } | undefined
      throw new Error(body?.message || '登录已过期，请重新登录')
    }

    throw error instanceof Error ? error : new Error('登录已过期，请重新登录')
  }
}
