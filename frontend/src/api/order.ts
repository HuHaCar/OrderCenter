import type { CreateDraftPayload, CreateOrderPayload, Order, OrderDetail, OrderLog, OrderQuery, PageResult, ShippingOption } from '@/types/order'
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

export function updateOrder(id: number, payload: CreateOrderPayload): Promise<Order> {
  return unwrap<Order>(http.post('/updateOrder', { id, ...payload }))
}

export function forecastOrder(id: number): Promise<Order> {
  return unwrap<Order>(http.post('/forecastOrder', { id }))
}

export function deleteOrders(ids: number[]): Promise<number> {
  return unwrap<number>(http.post('/deleteOrders', { ids }))
}

export interface ExportedFile {
  blob: Blob
  fileName: string
}

function resolveFileName(disposition: string | undefined): string {
  if (!disposition) {
    return `order-export-${Date.now()}.xlsx`
  }

  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) {
    try {
      return decodeURIComponent(encoded)
    } catch {
      // 忽略非法编码，继续尝试普通 filename。
    }
  }

  const plain = disposition.match(/filename="?([^";]+)"?/i)?.[1]
  return plain || `order-export-${Date.now()}.xlsx`
}

/**
 * 导出接口返回二进制文件，不使用统一 ApiResponse 包装。
 */
export async function exportOrders(ids: number[]): Promise<ExportedFile> {
  const response = await http.post('/exportOrders', { ids }, { responseType: 'blob' })
  return {
    blob: response.data as Blob,
    fileName: resolveFileName(response.headers['content-disposition']),
  }
}

export function createDraftOrder(payload: CreateDraftPayload): Promise<Order> {
  return unwrap<Order>(http.post('/createDraftOrder', payload))
}

export function getOrderDetail(id: number): Promise<OrderDetail> {
  return unwrap<OrderDetail>(http.post('/queryOrderDetail', { id }))
}

export function getOrderLogs(orderId: number): Promise<OrderLog[]> {
  return unwrap<OrderLog[]>(http.post('/queryOrderLogs', { orderId }))
}

export function getShippingOptions(): Promise<ShippingOption[]> {
  return unwrap<ShippingOption[]>(http.get('/shippingOptions'))
}

export function getHealth(): Promise<Record<string, unknown>> {
  return unwrap<Record<string, unknown>>(http.get('/health'))
}
