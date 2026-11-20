<template>
  <div class="table-view">
    <a-card v-if="$slots.filters" class="table-view__filter-card" :bordered="false">
      <slot name="filters" />
    </a-card>

    <BusinessTable
      :query="query"
      :fetch-api="fetchApi"
      :build-query="buildQuery"
      :columns="columns"
      :format-result="formatResult"
      :title="title"
      :empty-text="emptyText"
      :error-text="errorText"
      :row-key="rowKey"
      :selectable="selectable"
      :selectable-method="selectableMethod"
      @selection-change="emit('selection-change', $event)"
    >
      <template #toolbar>
        <slot name="batch-actions" />
      </template>
    </BusinessTable>
  </div>
</template>

<script setup lang="ts" generic="T extends Record<string, any>">
import BusinessTable from '@/components/business/BusinessTable.vue'
import type {
  BaseTableColumn,
  TableFetchParams,
  TableQuery,
  TableResultFormatter,
} from '@/types/table'

/**
 * 页面级布局组件：统一组装“筛选区 + 表格区”。
 * 表头、查询状态、请求参数和响应格式都由路由页面传入。
 */
defineOptions({ name: 'TableView' })

withDefaults(
  defineProps<{
    query: TableQuery
    fetchApi: (params: TableFetchParams) => Promise<unknown>
    buildQuery: (page: number, size: number) => TableFetchParams
    columns: BaseTableColumn<T>[]
    formatResult?: TableResultFormatter<T>
    title?: string
    emptyText?: string
    errorText?: string
    rowKey?: string
    selectable?: boolean
    selectableMethod?: (params: { row: T }) => boolean
  }>(),
  {
    title: '数据列表',
    emptyText: '暂无数据',
    errorText: '数据加载失败',
    rowKey: 'id',
    selectable: true,
  },
)

const emit = defineEmits<{
  (event: 'selection-change', rows: T[]): void
}>()
</script>

<style scoped>
.table-view {
  max-width: 1440px;
  margin: 0 auto;
}

.table-view__filter-card {
  margin-bottom: 10px;
  border-radius: 14px;
  box-shadow: 0 8px 30px rgba(32, 51, 84, 0.06);
}
</style>
