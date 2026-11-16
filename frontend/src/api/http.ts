import axios, {
  type AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import {
  clearAccessToken,
  getAccessToken,
  notifySessionExpired,
  refreshAccessToken,
} from '@/auth/session'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

interface PendingRequest {
  config: RetryableConfig
  replayable: boolean
  resolve: (value: AxiosResponse) => void
  reject: (reason?: unknown) => void
}

const AUTH_RETRY_REQUIRED_MESSAGE = '登录状态已刷新，请确认后重新提交'

/**
 * 这些 POST 接口只读或不产生业务副作用，401 刷新后可以安全重放。
 * 创建、编辑、预报、删除等提交类接口不自动重放，避免重复提交。
 */
const REPLAYABLE_POST_PATHS = [
  '/queryOrderList',
  '/queryOrderLogs',
  '/exportOrders',
]

/**
 * 全局刷新状态：同一时刻只允许一个刷新请求。
 * 后续 401 进入 pendingQueue，刷新成功后统一处理。
 */
let isRefreshing = false
const pendingQueue: PendingRequest[] = []

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

function isAuthRequest(url?: string) {
  return !!url && url.includes('/auth/')
}

function isReplayable(config: RetryableConfig) {
  const method = (config.method ?? 'get').toLowerCase()
  if (method === 'get' || method === 'head' || method === 'options') {
    return true
  }

  if (method === 'post') {
    const url = config.url ?? ''
    return REPLAYABLE_POST_PATHS.some((path) => url.includes(path))
  }

  return false
}

function applyAccessToken(config: RetryableConfig) {
  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
}

function flushPendingQueue() {
  const queue = pendingQueue.splice(0)

  for (const item of queue) {
    if (!item.replayable) {
      item.reject(new Error(AUTH_RETRY_REQUIRED_MESSAGE))
      continue
    }

    applyAccessToken(item.config)
    http(item.config).then(item.resolve, item.reject)
  }
}

function rejectPendingQueue(error: Error) {
  const queue = pendingQueue.splice(0)

  for (const item of queue) {
    item.reject(error)
  }
}

async function normalizeHttpError(error: unknown): Promise<Error> {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error : new Error('网络请求失败')
  }

  if (!error.response) {
    return new Error('无法连接后端服务，请确认 8080 端口上的 Spring Boot 已启动')
  }

  // 下载接口使用 responseType: 'blob'，错误响应也会被包成 Blob。
  let body: { message?: string } | undefined
  const rawBody = error.response.data as unknown
  if (rawBody instanceof Blob) {
    try {
      body = JSON.parse(await rawBody.text()) as { message?: string }
    } catch {
      body = undefined
    }
  } else {
    body = rawBody as { message?: string } | undefined
  }

  const status = error.response.status

  let message = body?.message
  if (!message) {
    if (status === 404) message = '接口不存在，请检查请求地址'
    else if (status === 500) message = '后端服务暂不可用，请查看后端日志'
    else if (status === 400) message = '请求参数不正确'
    else message = error.message || '网络请求失败'
  }

  return new Error(message)
}

async function handleUnauthorized(error: AxiosError): Promise<AxiosResponse> {
  const config = error.config as RetryableConfig | undefined
  if (!config) {
    throw await normalizeHttpError(error)
  }

  // 已经重试过一次仍然 401，不再进入刷新循环。
  if (config._retry) {
    throw new Error('登录已过期，请重新登录')
  }
  config._retry = true

  const replayable = isReplayable(config)

  // 已经在刷新：进入等待队列，不再重复发刷新请求。
  if (isRefreshing) {
    return new Promise<AxiosResponse>((resolve, reject) => {
      pendingQueue.push({ config, replayable, resolve, reject })
    })
  }

  isRefreshing = true

  try {
    await refreshAccessToken()
  } catch (refreshError) {
    const normalized = refreshError instanceof Error
      ? refreshError
      : new Error('登录已过期，请重新登录')

    rejectPendingQueue(normalized)
    clearAccessToken()
    isRefreshing = false
    notifySessionExpired()
    throw normalized
  }

  try {
    // 刷新成功：查询类请求统一重放，提交类请求拒绝并提示用户重新提交。
    flushPendingQueue()
  } finally {
    isRefreshing = false
  }

  if (!replayable) {
    throw new Error(AUTH_RETRY_REQUIRED_MESSAGE)
  }

  applyAccessToken(config)
  return http(config)
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && !isAuthRequest(config.url)) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (axios.isAxiosError(error)
        && error.response?.status === 401
        && !isAuthRequest(error.config?.url)) {
      return handleUnauthorized(error)
    }

    return Promise.reject(await normalizeHttpError(error))
  },
)

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const { data } = await request
  if (data.code !== 0) {
    throw new Error(data.message || '请求失败')
  }
  return data.data
}

export default http
