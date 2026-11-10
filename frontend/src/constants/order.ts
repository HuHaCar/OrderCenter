import type { OrderStatus } from '@/types/order'

export const ORDER_STATUS_OPTIONS: Array<{ label: string; value: OrderStatus }> = [
  { label: '待付款', value: 'PENDING' },
  { label: '已付款', value: 'PAID' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]

export const ORDER_STATUS_META: Record<OrderStatus, { label: string; color: string }> = {
  PENDING: { label: '待付款', color: 'orange' },
  PAID: { label: '已付款', color: 'blue' },
  SHIPPED: { label: '已发货', color: 'cyan' },
  COMPLETED: { label: '已完成', color: 'green' },
  CANCELLED: { label: '已取消', color: 'default' },
}
