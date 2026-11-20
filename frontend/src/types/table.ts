import type { VNodeChild } from 'vue'

export type TableAlign = 'left' | 'center' | 'right'

export interface TableCellContext<Row = Record<string, unknown>> {
  row: Row
  column: BaseTableColumn<Row>
  value: unknown
  index: number
  $index: number
}

export type TableCellRenderer<Row = Record<string, unknown>> = (
  context: TableCellContext<Row>,
) => VNodeChild

export interface BaseTableColumn<Row = Record<string, unknown>> {
  key: string
  field: string
  title: string
  width?: number | string
  minWidth?: number | string
  align?: TableAlign
  fixed?: 'left' | 'right'
  ellipsis?: boolean
  formatter?: (value: unknown, row: Row) => string
  renderCell?: TableCellRenderer<Row>
}

/**
 * 分页表格状态。页面/模块负责提供这份状态，
 * BusinessTable 只读取并调用对应方法，不依赖具体 Pinia store。
 */
export interface TableQuery {
  page: number
  pageSize: number
  total: number
  loading: boolean
  loadingAction: string | null
  refreshToken: number
  setLoading(value: boolean): void
  setTotal(value: number): void
  changePage(page: number, size: number): void
  refresh(): void
}

/**
 * 页面通过 buildQuery 组合业务筛选条件，例如订单的 keyword/status。
 */
export interface TableFetchParams {
  page: number
  size: number
  [key: string]: unknown
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
