<template>
  <vxe-table
    ref="tableRef"
    class="base-table"
    border="inner"
    :data="data"
    :height="height"
    :loading="loading"
    :row-config="{ keyField: rowKey, height: 48 }"
    :scroll-y="{ enabled: virtual, gt: 18 }"
    :show-overflow="true"
    :empty-text="emptyText"
    :checkbox-config="selectableMethod ? { checkMethod: selectableMethod } : undefined"
    @checkbox-change="emitSelection"
    @checkbox-all="emitSelection"
  >
    <vxe-column
      v-if="selectable"
      type="checkbox"
      width="46"
      fixed="left"
    />
    <vxe-column
      v-for="column in columns"
      :key="column.key"
      :field="column.field"
      :title="column.title"
      :width="column.width"
      :min-width="column.minWidth"
      :align="column.align ?? 'left'"
      :fixed="column.fixed"
      :show-overflow="column.ellipsis ?? true"
    >
      <template #default="{ row }">
        <slot :name="`cell-${column.key}`" :row="row" :column="column">
          {{ column.formatter ? column.formatter(row[column.field], row) : (row[column.field] ?? '—') }}
        </slot>
      </template>
    </vxe-column>
  </vxe-table>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { BaseTableColumn } from '@/types/table'
import { VxeColumn, VxeTable } from 'vxe-table'

/**
 * UI 基础组件：只负责“怎么展示表格”。
 * 不关心订单业务、不请求接口，任何列表都可以复用。
 * 通过 vxe-table 的 scroll-y 配置开启虚拟滚动。
 */
withDefaults(
  defineProps<{
    columns: BaseTableColumn[]
    data: Record<string, unknown>[]
    loading?: boolean
    height?: number | string
    rowKey?: string
    virtual?: boolean
    emptyText?: string
    selectable?: boolean
    selectableMethod?: (params: { row: Record<string, unknown> }) => boolean
  }>(),
  {
    loading: false,
    height: 540,
    rowKey: 'id',
    virtual: true,
    emptyText: '暂无数据',
    selectable: false,
  },
)

const emit = defineEmits<{
  (event: 'selection-change', ids: Array<string | number>): void
}>()

const tableRef = ref()

function emitSelection() {
  const rows = tableRef.value?.getCheckboxRecords?.() ?? []
  emit('selection-change', rows.map((row: { id: string | number }) => row.id))
}

defineOptions({ name: 'BaseTable' })
</script>

<style scoped>
.base-table {
  --vxe-ui-font-family: var(--app-font);
  width: 100%;
}

.base-table :deep(.vxe-table--header-wrapper) {
  background: #fafbfc;
}

.base-table :deep(.vxe-header--column) {
  color: #6b778c;
  font-size: 12px;
  font-weight: 600;
}

.base-table :deep(.vxe-body--row.row--hover) {
  background: #f6f9ff;
}

.base-table :deep(.vxe-body--column) {
  color: #2c3648;
  font-size: 13px;
}
</style>
