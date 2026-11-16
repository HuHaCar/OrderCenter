import axios from 'axios'

/**
 * 不走全局 401 刷新拦截器的原始实例。
 * 登录、刷新、退出必须使用它，避免刷新接口自己触发刷新。
 */
export const rawHttp = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})
