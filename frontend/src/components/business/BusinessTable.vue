<template>
  <a-card class="business-table-card" :bordered="false">
    <div class="business-table-card__head">
      <div class="business-table-card__heading">
        <span class="business-table-card__title">{{ title }}</span>
        <span class="business-table-card__count">共 {{ query.total }} 条</span>
      </div>
      <div v-if="$slots.toolbar" class="business-table-card__toolbar">
        <slot name="toolbar" />
      </div>
      <a-button
        :loading="query.loading && query.loadingAction === 'refresh'"
        :disabled="query.loading && query.loadingAction !== 'refresh'"
        size="small"
        @click="query.refresh"
      >
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <a-alert
      v-if="errorMessage"
      class="business-table-card__alert"
      type="error"
      show-icon
      :message="errorMessage"
      closable
      @close="errorMessage = ''"
    />

    <BaseTable
      :columns="columns"
      :data="rows"
      :loading="query.loading"
      :height="tableHeight"
      :row-key="rowKey"
      :empty-text="emptyText"
      :selectable="selectable"
      :selectable-method="selectableMethod"
      @selection-change="handleSelectionChange"
    />

    <div class="business-table-card__footer">
      <span class="business-table-card__hint">支持虚拟滚动列表</span>
      <a-pagination
        :current="query.page"
        :page-size="query.pageSize"
        :total="query.total"
        :show-size-changer="true"
        :page-size-options="['20', '50', '100', '200']"
        :show-total="showTotal"
        :disabled="query.loading"
        @change="handlePageChange"
      />
    </div>
  </a-card>
</template>

<script setup lang="ts" generic="T extends Record<string, any>">
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import type {
  BaseTableColumn,
  TableFetchParams,
  TableQuery,
  TableResult,
  TableResultFormatter,
} from '@/types/table'

/**
 * 通用业务表格：
 * - 只负责数据请求、分页、loading、错误、selection
 * - 查询状态由页面通过 query 注入
 * - 请求参数由页面通过 buildQuery 组装
 * - 单元格渲染由页面 columns[].renderCell 负责
 */
defineOptions({ name: 'BusinessTable' })

const props = withDefaults(
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

const rows = shallowRef<T[]>([])
const errorMessage = ref('')
const tableHeight = ref(520)
let requestSeq = 0

const defaultFormatResult: TableResultFormatter<T> = (result) => {
  const page = result as
    | { records?: T[]; total?: number; page?: number; size?: number }
    | null
    | undefined

  return {
    data: page?.records ?? [],
    total: page?.total ?? 0,
    page: page?.page,
    size: page?.size,
  }
}

/** 根据视口高度调整表格高度，保证底部分页条始终可见。 */
function syncTableHeight() {
  const next = Math.round(window.innerHeight - 300)
  tableHeight.value = Math.min(760, Math.max(300, next))
}

onMounted(() => {
  syncTableHeight()
  window.addEventListener('resize', syncTableHeight)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncTableHeight)
})

function showTotal(total: number) {
  return `共 ${total} 条`
}

async function loadData() {
  const seq = ++requestSeq
  props.query.setLoading(true)
  errorMessage.value = ''

  try {
    const rawResult = await props.fetchApi(
      props.buildQuery(props.query.page, props.query.pageSize),
    )

    if (seq !== requestSeq) return

    const result = (props.formatResult ?? defaultFormatResult)(rawResult)
    const currentPage = result.page ?? props.query.page
    const currentSize = result.size ?? props.query.pageSize

    rows.value = result.data.map((item, index) => ({
      ...item,
      serial: (currentPage - 1) * currentSize + index + 1,
    })) as T[]

    props.query.setTotal(result.total)
    emit('selection-change', [])
  } catch (error) {
    if (seq !== requestSeq) return
    rows.value = []
    props.query.setTotal(0)
    errorMessage.value = error instanceof Error
      ? error.message
      : props.errorText
    message.error(errorMessage.value)
  } finally {
    if (seq === requestSeq) {
      props.query.setLoading(false)
    }
  }
}

function handleSelectionChange(ids: Array<string | number>) {
  const selectedIds = new Set(ids.map(Number))
  emit(
    'selection-change',
    rows.value.filter((row) => selectedIds.has(Number(row.id))),
  )
}

function handlePageChange(page: number, size: number) {
  props.query.changePage(page, size)
}

watch(
  () => props.query.refreshToken,
  loadData,
  { immediate: true },
)
</script>

<style scoped>
.business-table-card {
  border-radius: 14px;
  box-shadow: 0 8px 30px rgba(32, 51, 84, 0.06);
}

.business-table-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.business-table-card__heading {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.business-table-card__title {
  color: #162033;
  font-size: 16px;
  font-weight: 700;
}

.business-table-card__count {
  color: #98a3b3;
  font-size: 12px;
}

.business-table-card__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.business-table-card__alert {
  margin-bottom: 14px;
}

.business-table-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 18px;
}

.business-table-card__hint {
  color: #a1abba;
  font-size: 12px;
}

@media (max-width: 760px) {
  .business-table-card__hint {
    display: none;
  }

  .business-table-card__footer {
    justify-content: flex-end;
  }
}
</style>
