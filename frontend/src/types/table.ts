export type TableAlign = 'left' | 'center' | 'right'

export interface BaseTableColumn {
  key: string
  field: string
  title: string
  width?: number | string
  minWidth?: number | string
  align?: TableAlign
  ellipsis?: boolean
  formatter?: (value: unknown, row: Record<string, unknown>) => string
}

/**
 * 表格数据统一后的结构。
 * 无论后端返回 records/total 还是 InnerList/TotalCount，
 * 都由最外层页面通过 formatResult 转成这个结构。
 */
export interface TableResult<T> {
  data: T[]
  total: number
  page?: number
  size?: number
  [key: string]: unknown
}

export type TableResultFormatter<T> = (result: unknown) => TableResult<T>

