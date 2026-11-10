import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import type { OrderFilters } from '@/types/order'

/**
 * 订单查询状态：筛选条件、分页、总数。
 * 页面级组件负责展示和修改筛选条件；业务组件监听 refreshToken 拉取数据。
 */
export const useOrderStore = defineStore('order', () => {
  const filters = reactive<OrderFilters>({
    keyword: '',
    status: undefined,
  })
  const page = ref(1)
  const pageSize = ref(100)
  const total = ref(0)
  const refreshToken = ref(0)

  function search() {
    page.value = 1
    refreshToken.value += 1
  }

  function reset() {
    filters.keyword = ''
    filters.status = undefined
    page.value = 1
    refreshToken.value += 1
  }

  function changePage(nextPage: number, nextSize: number) {
    page.value = nextPage
    pageSize.value = nextSize
    refreshToken.value += 1
  }

  function refresh() {
    refreshToken.value += 1
  }

  function setTotal(value: number) {
    total.value = value
  }

  return {
    filters,
    page,
    pageSize,
    total,
    refreshToken,
    search,
    reset,
    changePage,
    refresh,
    setTotal,
  }
})
