import axios from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      return Promise.reject(new Error('无法连接后端服务，请确认 8080 端口上的 Spring Boot 已启动'))
    }

    const body = error.response.data as { message?: string } | undefined
    const status = error.response.status

    let message = body?.message
    if (!message) {
      if (status === 404) message = '接口不存在，请检查请求地址'
      else if (status === 500) message = '后端服务暂不可用，请查看后端日志'
      else if (status === 400) message = '请求参数不正确'
      else message = error.message || '网络请求失败'
    }

    return Promise.reject(new Error(message))
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
