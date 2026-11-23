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
    :tooltip-config="tooltipConfig"
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
      <template #default="{ row, $rowIndex }">
        <CellRenderer
          v-if="column.renderCell"
          :render="column.renderCell"
          :context="{
            row,
            column,
            value: row[column.field],
            index: $rowIndex,
            $index: $rowIndex,
          }"
        />

        <slot
          v-else
          :name="`cell-${column.key}`"
          :row="row"
          :column="column"
          :$index="$rowIndex"
        >
          {{ column.formatter
            ? column.formatter(row[column.field], row)
            : (row[column.field] ?? '—') }}
        </slot>
      </template>
    </vxe-column>
  </vxe-table>
</template>

<script setup lang="ts" generic="T extends Record<string, any>">
import { defineComponent, ref } from 'vue'
import type { BaseTableColumn } from '@/types/table'
import { VxeColumn, VxeTable } from 'vxe-table'

/**
 * UI 基础组件：只负责“怎么展示表格”。
 * 不关心业务、不请求接口，任何列表都可以复用。
 *
 * renderCell 优先于自定义插槽，插槽优先于 formatter/default。
 * renderCell 返回 VNodeChild，适合状态标签、操作按钮等复杂单元格。
 */
defineOptions({ name: 'BaseTable' })

withDefaults(
  defineProps<{
    columns: BaseTableColumn<T>[]
    data: T[]
    loading?: boolean
    height?: number | string
    rowKey?: string
    virtual?: boolean
    emptyText?: string
    selectable?: boolean
    selectableMethod?: (params: { row: T }) => boolean
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

const tooltipConfig = {
  showAll: true,
  mode: 'tooltip' as const,
}

const tableRef = ref()

/**
 * Vue 模板不能直接输出 VNode，这里用一个小型函数组件承接 renderCell。
 */
const CellRenderer = defineComponent({
  name: 'CellRenderer',
  props: {
    render: {
      type: Function,
      required: true,
    },
    context: {
      type: Object,
      required: true,
    },
  },
  setup(props) {
    return () => (props.render as (context: unknown) => unknown)(props.context)
  },
})

function emitSelection() {
  const rows = tableRef.value?.getCheckboxRecords?.() ?? []
  emit(
    'selection-change',
    rows.map((row: { id: string | number }) => row.id),
  )
}
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
