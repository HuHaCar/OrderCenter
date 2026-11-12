import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import type { OrderFilters } from '@/types/order'

/**
 * 订单查询状态：筛选条件、分页、总数。
 * 页面级组件负责展示和修改筛选条件；业务组件监听 refreshToken 拉取数据。
 */
export type OrderLoadingAction = 'search' | 'reset' | 'refresh' | 'page' | null

export const useOrderStore = defineStore('order', () => {
  const filters = reactive<OrderFilters>({
    keyword: '',
    status: undefined,
  })
  const page = ref(1)
  const pageSize = ref(100)
  const total = ref(0)
  const refreshToken = ref(0)
  const loading = ref(false)
  const loadingAction = ref<OrderLoadingAction>(null)

  function search() {
    page.value = 1
    loadingAction.value = 'search'
    refreshToken.value += 1
  }

  function reset() {
    filters.keyword = ''
    filters.status = undefined
    page.value = 1
    loadingAction.value = 'reset'
    refreshToken.value += 1
  }

  function changePage(nextPage: number, nextSize: number) {
    page.value = nextPage
    pageSize.value = nextSize
    loadingAction.value = 'page'
    refreshToken.value += 1
  }

  function refresh() {
    loadingAction.value = 'refresh'
    refreshToken.value += 1
  }

  function setTotal(value: number) {
    total.value = value
  }

  function setLoading(value: boolean) {
    loading.value = value
    if (!value) {
      loadingAction.value = null
    }
  }

  return {
    filters,
    page,
    pageSize,
    total,
    refreshToken,
    loading,
    loadingAction,
    search,
    reset,
    changePage,
    refresh,
    setTotal,
    setLoading,
  }
})
