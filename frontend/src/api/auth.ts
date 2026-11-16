import http, { unwrap, type ApiResponse } from './http'
import { rawHttp } from './raw'
import type { AuthTokenResult, AuthUser, LoginPayload } from '@/types/auth'

export function login(payload: LoginPayload): Promise<AuthTokenResult> {
  return unwrap<AuthTokenResult>(http.post('/auth/login', payload))
}

export async function logout(): Promise<void> {
  const { data } = await rawHttp.post<ApiResponse<null>>('/auth/logout')
  if (data.code !== 0) {
    throw new Error(data.message || '退出登录失败')
  }
}

export function getCurrentUser(): Promise<AuthUser> {
  return unwrap<AuthUser>(http.get('/auth/me'))
}
