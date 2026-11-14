import type { CreateOrderPayload, Order, OrderQuery, PageResult } from '@/types/order'
import http, { unwrap } from './http'

/**
 * 页面级组件导入这个函数，再通过 props 注入给业务组件。
 * 业务组件不直接依赖具体 API，方便测试与复用。
 */
export function getOrderList(params: OrderQuery): Promise<PageResult<Order>> {
  return unwrap<PageResult<Order>>(http.get('/queryOrderList', { params }))
}

export function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return unwrap<Order>(http.post('/createOrder', payload))
}

export function getHealth(): Promise<Record<string, unknown>> {
  return unwrap<Record<string, unknown>>(http.get('/health'))
}
