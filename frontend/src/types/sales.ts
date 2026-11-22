export type SalesLever = 'A' | 'B' | 'C' | 'D'

export interface Sales {
  id: number
  employeeNo: string
  employeeName: string
  lever: SalesLever
  entryDate: string
  gender: number | null
  phone: string | null
  email: string | null
  idCardNo: string | null
  remark: string | null
  currentMonthPerformance: number | null
  createdAt: string
  updatedAt: string
}

export interface SalesQuery {
  page: number
  size: number
  keyword?: string
  lever?: SalesLever
}

export interface SalesFilters {
  keyword: string
  lever?: SalesLever
}

export interface SalesPageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}
