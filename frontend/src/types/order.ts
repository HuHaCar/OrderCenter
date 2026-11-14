export type OrderStatus = 'T' | 'S' | 'R' | 'D' | 'F' | 'V' | 'P' | 'C' | 'Q'

export interface Order {
  id: number
  orderNo: string
  customerName: string
  customerPhone: string
  productName: string
  quantity: number
  unitPrice: number
  totalAmount: number
  status: OrderStatus
  statusLabel: string
  remark: string | null
  createdAt: string
}

export interface OrderQuery {
  page: number
  size: number
  keyword?: string
  status?: OrderStatus
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface OrderFilters {
  keyword: string
  status?: OrderStatus
}

export interface CreateOrderPayload {
  customerName: string
  customerPhone: string
  productName: string
  quantity: number
  unitPrice: number
  remark?: string
}
