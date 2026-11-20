import type { SalesLever } from '@/types/sales'

export const SALES_LEVER_OPTIONS: Array<{ label: string; value: SalesLever }> = [
  { label: '荣誉', value: 'A' },
  { label: '高级', value: 'B' },
  { label: '中级', value: 'C' },
  { label: '初级', value: 'D' },
]

export const SALES_LEVER_LABELS: Record<SalesLever, string> = {
  B: '高级',
  C: '中级',
  D: '初级',
  A: '荣誉',
}

export function getSalesLeverLabel(value: unknown): string {
  if (typeof value === 'string' && value in SALES_LEVER_LABELS) {
    return SALES_LEVER_LABELS[value as SalesLever]
  }
  return value == null || value === '' ? '—' : String(value)
}

export function getGenderLabel(value: unknown): string {
  if (value == null || value === '') return '—'
  const gender = Number(value)
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  if (gender === 0) return '未知'
  return '—'
}
