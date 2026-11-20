import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import type { SalesFilters } from '@/types/sales'

export type SalesLoadingAction = 'search' | 'reset' | 'refresh' | 'page' | null

/**
 * 业务员列表查询状态。
 * 页面负责筛选条件和分页；BusinessTable 监听 refreshToken 拉取数据。
 */
export const useSalesStore = defineStore('sales', () => {
  const filters = reactive<SalesFilters>({
    keyword: '',
    lever: undefined,
  })
  const page = ref(1)
  const pageSize = ref(100)
  const total = ref(0)
  const refreshToken = ref(0)
  const loading = ref(false)
  const loadingAction = ref<SalesLoadingAction>(null)

  function search() {
    page.value = 1
    loadingAction.value = 'search'
    refreshToken.value += 1
  }

  function reset() {
    filters.keyword = ''
    filters.lever = undefined
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
