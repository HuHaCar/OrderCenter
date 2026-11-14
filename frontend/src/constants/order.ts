import type { OrderStatus } from '@/types/order'

export const ORDER_STATUS_OPTIONS: Array<{ label: string; value: OrderStatus }> = [
  { label: '草稿', value: 'T' },
  { label: '已预报', value: 'S' },
  { label: '已收货', value: 'R' },
  { label: '已发货', value: 'D' },
  { label: '已签收', value: 'V' },
  { label: '已退件', value: 'F' },
  { label: '已理赔', value: 'P' },
  { label: '弃件', value: 'Q' },
  { label: '已删除', value: 'C' },
]

export const ORDER_STATUS_META: Record<OrderStatus, { label: string; color: string }> = {
  T: { label: '草稿', color: 'default' },
  S: { label: '已预报', color: 'blue' },
  R: { label: '已收货', color: 'geekblue' },
  D: { label: '已发货', color: 'cyan' },
  V: { label: '已签收', color: 'green' },
  F: { label: '已退件', color: 'orange' },
  P: { label: '已理赔', color: 'purple' },
  Q: { label: '弃件', color: 'red' },
  C: { label: '已删除', color: 'default' },
}
