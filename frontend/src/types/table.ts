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
