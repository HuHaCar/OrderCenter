<template>
  <a-card class="business-table-card" :bordered="false">
    <div class="business-table-card__head">
      <div class="business-table-card__heading">
        <span class="business-table-card__title">订单列表</span>
        <span class="business-table-card__count">共 {{ orderStore.total }} 条</span>
      </div>
      <div v-if="$slots.toolbar" class="business-table-card__toolbar">
        <slot name="toolbar" />
      </div>
      <a-button :loading="loading" size="small" @click="orderStore.refresh">
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
      :loading="loading"
      :height="tableHeight"
      row-key="id"
      empty-text="暂无订单，试试调整筛选条件"
    >
      <template #cell-totalAmount="{ row }">
        <span class="order-amount">¥ {{ formatCurrency(row.totalAmount) }}</span>
      </template>

      <template #cell-status="{ row }">
        <a-tag :color="ORDER_STATUS_META[row.status as Order['status']]?.color">
          {{ ORDER_STATUS_META[row.status as Order['status']]?.label ?? row.statusLabel }}
        </a-tag>
      </template>

      <template #cell-createdAt="{ row }">
        <span class="order-time">{{ formatDateTime(row.createdAt) }}</span>
      </template>
    </BaseTable>

    <div class="business-table-card__footer">
      <span class="business-table-card__hint">支持 100 条 / 页的虚拟滚动列表</span>
      <a-pagination
        :current="orderStore.page"
        :page-size="orderStore.pageSize"
        :total="orderStore.total"
        :show-size-changer="true"
        :page-size-options="['20', '50', '100', '200']"
        :show-total="showTotal"
        @change="handlePageChange"
      />
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import { ORDER_STATUS_META } from '@/constants/order'
import { useOrderStore } from '@/stores/order'
import type { BaseTableColumn } from '@/types/table'
import type { Order, OrderQuery, PageResult } from '@/types/order'
import { formatCurrency, formatDateTime } from '@/utils/format'

/**
 * 业务组件：负责订单数据获取、分页、加载/错误状态。
 * 具体 API 函数由页面级组件通过 fetchApi 注入，业务组件不直接依赖 api/order.ts。
 */
defineOptions({ name: 'BusinessTable' })

const props = defineProps<{
  fetchApi: (query: OrderQuery) => Promise<PageResult<Order>>
}>()

const orderStore = useOrderStore()
const rows = ref<Order[]>([])
const loading = ref(false)
const errorMessage = ref('')
const tableHeight = ref(520)
let requestSeq = 0

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

const columns: BaseTableColumn[] = [
  { key: 'orderNo', field: 'orderNo', title: '订单号', width: 160 },
  { key: 'customerName', field: 'customerName', title: '客户', width: 105 },
  { key: 'customerPhone', field: 'customerPhone', title: '联系电话', width: 135 },
  { key: 'productName', field: 'productName', title: '商品', minWidth: 200 },
  { key: 'quantity', field: 'quantity', title: '数量', width: 74, align: 'right' },
  { key: 'totalAmount', field: 'totalAmount', title: '金额', width: 130, align: 'right' },
  { key: 'status', field: 'status', title: '状态', width: 104, align: 'center' },
  { key: 'createdAt', field: 'createdAt', title: '下单时间', width: 160 },
]

function showTotal(total: number) {
  return `共 ${total} 条`
}

async function loadOrders() {
  const seq = ++requestSeq
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await props.fetchApi({
      page: orderStore.page,
      size: orderStore.pageSize,
      keyword: orderStore.filters.keyword.trim() || undefined,
      status: orderStore.filters.status,
    })

    if (seq !== requestSeq) return
    rows.value = result.records
    orderStore.setTotal(result.total)
  } catch (error) {
    if (seq !== requestSeq) return
    rows.value = []
    orderStore.setTotal(0)
    errorMessage.value = error instanceof Error ? error.message : '订单加载失败'
    message.error(errorMessage.value)
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

function handlePageChange(page: number, size: number) {
  orderStore.changePage(page, size)
}

watch(() => orderStore.refreshToken, loadOrders, { immediate: true })
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

.order-amount {
  color: #162033;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.order-time {
  color: #7c889b;
  font-variant-numeric: tabular-nums;
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
