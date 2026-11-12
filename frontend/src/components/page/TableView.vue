<template>
  <div class="table-view">
    <a-card v-if="$slots.filters" class="table-view__filter-card" :bordered="false">
      <slot name="filters" />
    </a-card>

    <BusinessTable
      :fetch-api="fetchApi"
      :columns="columns"
      :format-result="formatResult"
    >
      <template #toolbar>
        <slot name="batch-actions" />
      </template>
    </BusinessTable>
  </div>
</template>

<script setup lang="ts">
import BusinessTable from '@/components/business/BusinessTable.vue'
import type { BaseTableColumn, TableResultFormatter } from '@/types/table'
import type { Order, OrderQuery } from '@/types/order'

/**
 * 页面级布局组件：统一组装“筛选区 + 表格区”。
 * 表头与后端响应格式都由路由页面传入，具体数据获取下沉在 BusinessTable。
 */
defineOptions({ name: 'TableView' })

defineProps<{
  fetchApi: (query: OrderQuery) => Promise<unknown>
  columns: BaseTableColumn[]
  formatResult?: TableResultFormatter<Order>
}>()
</script>

<style scoped>
.table-view {
  max-width: 1440px;
  margin: 0 auto;
}

.table-view__filter-card {
  margin-bottom: 18px;
  border-radius: 14px;
  box-shadow: 0 8px 30px rgba(32, 51, 84, 0.06);
}
</style>
