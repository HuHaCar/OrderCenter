import { reactive, shallowRef } from 'vue'

export interface ModalState {
  visible: boolean
  loading: boolean
}

/**
 * 通用弹窗 Hook。
 * 统一管理 visible / loading / 业务数据，避免每个页面重复写一套开关状态。
 *
 * const modal = useModal<Order>()
 * modal.open(row)
 * modal.setLoading(true)
 * modal.close()
 */
export function useModal<T = unknown>() {
  const state = reactive<ModalState>({
    visible: false,
    loading: false,
  })
  const data = shallowRef<T | null>(null)

  function open(payload?: T) {
    data.value = payload ?? null
    state.loading = false
    state.visible = true
  }

  function close() {
    if (state.loading) return
    state.visible = false
  }

  function setLoading(loading: boolean) {
    state.loading = loading
  }

  return {
    state,
    data,
    open,
    close,
    setLoading,
  }
}
