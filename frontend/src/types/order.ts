export type OrderStatus = 'T' | 'S' | 'R' | 'D' | 'F' | 'V' | 'P' | 'C' | 'Q'

export interface Order {
  id: number
  orderNo: string
  shippingMethod: string
  shippingMethodLabel: string
  recipientCountry: string
  customerName: string | null
  customerPhone: string | null
  productName: string | null
  quantity: number | null
  unitPrice: number | null
  totalAmount: number | null
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
  shippingMethod: string
  recipientCountry: string
  customerName: string
  customerPhone: string
  productName: string
  quantity: number
  unitPrice: number
  remark?: string
}

export interface ShippingOption {
  code: string
  name: string
  allCountries: boolean
  countryCodes: string[]
}

export interface CreateDraftPayload {
  shippingMethod: string
  recipientCountry: string
  customerName?: string
  customerPhone?: string
  productName?: string
  quantity?: number
  unitPrice?: number
  remark?: string
}

export interface OrderLog {
  id: number
  action: string
  actionLabel: string
  description: string
  operator: string
  createdAt: string
}
