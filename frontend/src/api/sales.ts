import http, { unwrap } from './http'
import type { Sales, SalesPageResult, SalesQuery } from '@/types/sales'

export function getSalesList(params: SalesQuery): Promise<SalesPageResult<Sales>> {
  return unwrap<SalesPageResult<Sales>>(
    http.get('/querySalesList', { params }),
  )
}
