<template>
  <TableView
    :query="orderStore"
    :fetch-api="fetchOrders"
    :build-query="buildOrderQuery"
    :columns="orderColumns"
    :format-result="formatOrderResult"
    title="订单列表"
    empty-text="暂无订单，试试调整筛选条件"
    error-text="订单加载失败"
    @selection-change="handleSelectionChange"
  >
    <template #filters>
      <div class="order-filters">
        <a-form layout="inline" class="filter-form" :model="orderStore.filters">
          <a-form-item label="关键词">
            <a-input
              v-model:value="orderStore.filters.keyword"
              allow-clear
              placeholder="订单号 / 客户 / 商品"
              class="filter-form__keyword"
              @press-enter="orderStore.search"
            />
          </a-form-item>

          <a-form-item label="订单状态">
            <a-select
              v-model:value="orderStore.filters.status"
              allow-clear
              placeholder="全部状态"
              :options="ORDER_STATUS_OPTIONS"
              class="filter-form__status"
              @change="orderStore.search"
            />
          </a-form-item>

          <a-form-item class="filter-form__actions">
            <a-space>
              <a-button
                type="primary"
                :loading="isSearching"
                :disabled="orderStore.loading && !isSearching"
                @click="orderStore.search"
              >
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button
                :loading="isResetting"
                :disabled="orderStore.loading && !isResetting"
                @click="orderStore.reset"
              >
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <a-button type="primary" class="order-filters__create" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新建订单
        </a-button>
      </div>
    </template>

    <template #batch-actions>
      <a-space class="batch-actions" :size="8">
        <span class="batch-actions__label">已选 {{ selectedOrders.length }} 项</span>
        <a-button
          size="small"
          :disabled="!selectedOrders.length"
          :loading="exportConfirm.state.loading"
          @click="openBatchExportConfirm"
        >
          批量导出
        </a-button>
        <a-button
          size="small"
          danger
          :disabled="!canBatchDelete"
          :title="selectedOrders.length && !canBatchDelete ? '仅草稿或已预报订单可以删除' : undefined"
          @click="openBatchDeleteConfirm"
        >
          批量删除
        </a-button>
      </a-space>
    </template>
  </TableView>

  <OrderFormModal
    :open="createModal.state.visible"
    :loading="createModal.state.loading"
    :shipping-options="shippingOptions"
    :shipping-options-loading="shippingOptionsLoading"
    :draft-loading="draftSaving"
    @update:open="createModal.close"
    @submit="handleCreateOrder"
    @save-draft="handleSaveDraft"
  />

  <OrderFormModal
    mode="edit"
    :open="editModal.state.visible"
    :loading="editModal.state.loading"
    :order="editingOrder"
    :shipping-options="shippingOptions"
    :shipping-options-loading="shippingOptionsLoading"
    @update:open="editModal.close"
    @submit="handleUpdateOrder"
  />

  <OrderLogModal
    :open="logModal.state.visible"
    :order="logOrder"
    :fetch-api="getOrderLogs"
    @update:open="logModal.close"
  />

  <OrderDetailModal
    :open="detailModal.state.visible"
    :order-id="detailOrder?.id ?? null"
    :fetch-api="getOrderDetail"
    @update:open="detailModal.close"
  />

  <BaseModal
    :open="deleteConfirm.state.visible"
    title="确认删除"
    width="420px"
    ok-text="确认删除"
    cancel-text="取消"
    :confirm-loading="deleteConfirm.state.loading"
    :cancel-button-props="{ disabled: deleteConfirm.state.loading }"
    :closable="!deleteConfirm.state.loading"
    @ok="handleConfirmDelete"
    @cancel="deleteConfirm.close"
  >
    <div class="delete-confirm">
      <p>确定要删除 <strong>{{ deleteTarget?.label }}</strong> 吗？</p>
      <p class="delete-confirm__hint">
        删除后订单状态将变为“已删除”，并从默认列表隐藏。
      </p>
    </div>
  </BaseModal>

  <BaseModal
    :open="exportConfirm.state.visible"
    title="确认导出"
    width="420px"
    ok-text="确认导出"
    cancel-text="取消"
    :confirm-loading="exportConfirm.state.loading"
    :cancel-button-props="{ disabled: exportConfirm.state.loading }"
    :closable="!exportConfirm.state.loading"
    @ok="handleConfirmExport"
    @cancel="exportConfirm.close"
  >
    <div class="export-confirm">
      <p>确定要导出 <strong>{{ exportTarget?.label }}</strong> 吗？</p>
      <p class="export-confirm__hint">
        将下载 Excel（.xlsx）文件，导出范围包含当前选中订单的全部状态。
      </p>
    </div>
  </BaseModal>

  <BaseModal
    :open="forecastConfirm.state.visible"
    title="确认预报"
    width="420px"
    ok-text="确认预报"
    cancel-text="取消"
    :confirm-loading="forecastConfirm.state.loading"
    :cancel-button-props="{ disabled: forecastConfirm.state.loading }"
    :closable="!forecastConfirm.state.loading"
    @ok="handleConfirmForecast"
    @cancel="forecastConfirm.close"
  >
    <div class="forecast-confirm">
      <p>
        确定要将订单
        <strong>{{ forecastTarget?.orderNo }}</strong>
        预报吗？
      </p>
      <p class="forecast-confirm__hint">
        预报后订单状态将变为“已预报”。
      </p>
    </div>
  </BaseModal>
</template>

<script setup lang="tsx">
import { computed, ref } from 'vue'
import { Button, Tag, message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import TableView from '@/components/page/TableView.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import OrderFormModal from '@/components/business/OrderFormModal.vue'
import OrderDetailModal from '@/components/business/OrderDetailModal.vue'
import OrderLogModal from '@/components/business/OrderLogModal.vue'
import { createDraftOrder, createOrder, deleteOrders, exportOrders, forecastOrder, getOrderDetail, getOrderList, getOrderLogs, getShippingOptions, updateOrder } from '@/api/order'
import { getCountryName } from '@/constants/countries'
import { ORDER_STATUS_META, ORDER_STATUS_OPTIONS } from '@/constants/order'
import { useModal } from '@/composables/useModal'
import { useOrderStore } from '@/stores/order'
import type { BaseTableColumn, TableFetchParams, TableResult } from '@/types/table'
import type { CreateDraftPayload, CreateOrderPayload, Order, OrderQuery, PageResult, ShippingOption } from '@/types/order'
import { downloadBlob } from '@/utils/download'
import { formatCurrency, formatDateTime } from '@/utils/format'

/**
 * 路由级页面：负责提供表头、响应格式、查询条件和页面级弹窗。
 * 表格的数据获取和渲染由 TableView + BusinessTable 负责。
 */
const orderStore = useOrderStore()
const createModal = useModal()
const editModal = useModal<Order>()
const editingOrder = editModal.data
const shippingOptions = ref<ShippingOption[]>([])
const shippingOptionsLoading = ref(false)
const draftSaving = ref(false)
const forecastConfirm = useModal<Order>()
const forecastTarget = forecastConfirm.data
const selectedOrders = ref<Order[]>([])
const selectedOrderIds = computed(() => selectedOrders.value.map((order) => order.id))
const canBatchDelete = computed(
  () =>
    selectedOrders.value.length > 0
    && selectedOrders.value.every((order) => order.status === 'T' || order.status === 'S'),
)
interface BatchTarget {
  ids: number[]
  label: string
}
const deleteConfirm = useModal<BatchTarget>()
const deleteTarget = deleteConfirm.data
const exportConfirm = useModal<BatchTarget>()
const exportTarget = exportConfirm.data
const logModal = useModal<Order>()
const logOrder = logModal.data
const detailModal = useModal<Order>()
const detailOrder = detailModal.data
let shippingOptionsLoaded = false
const isSearching = computed(
  () => orderStore.loading && orderStore.loadingAction === 'search',
)
const isResetting = computed(
  () => orderStore.loading && orderStore.loadingAction === 'reset',
)

const orderColumns: BaseTableColumn<Order>[] = [
  { key: 'serial', field: 'serial', title: '序号', width: 60, align: 'center' },
  { key: 'orderNo', field: 'orderNo', title: '订单号', width: 160 },
  {
    key: 'shipping',
    field: 'shippingMethod',
    title: '运输方式',
    width: 170,
    renderCell: ({ row }) => (
      <div class="shipping-cell">
        <span class="shipping-cell__method">
          {row.shippingMethodLabel || row.shippingMethod}
        </span>
        <span class="shipping-cell__country">
          {getCountryName(row.recipientCountry)}
        </span>
      </div>
    ),
  },
  { key: 'customerName', field: 'customerName', title: '客户', width: 95 },
  { key: 'customerPhone', field: 'customerPhone', title: '联系电话', width: 125 },
  { key: 'productName', field: 'productName', title: '商品', minWidth: 150 },
  { key: 'quantity', field: 'quantity', title: '数量', width: 64, align: 'right' },
  {
    key: 'totalAmount',
    field: 'totalAmount',
    title: '金额',
    width: 120,
    align: 'right',
    renderCell: ({ row }) => (
      row.totalAmount == null
        ? <span class="order-amount order-amount--empty">—</span>
        : <span class="order-amount">¥ {formatCurrency(row.totalAmount)}</span>
    ),
  },
  {
    key: 'status',
    field: 'status',
    title: '状态',
    width: 90,
    align: 'center',
    renderCell: ({ row }) => (
      <Tag color={ORDER_STATUS_META[row.status]?.color}>
        {ORDER_STATUS_META[row.status]?.label ?? row.statusLabel}
      </Tag>
    ),
  },
  {
    key: 'createdAt',
    field: 'createdAt',
    title: '下单时间',
    width: 140,
    renderCell: ({ row }) => (
      <span class="order-time">{formatDateTime(row.createdAt)}</span>
    ),
  },
  {
    key: 'actions',
    field: 'id',
    title: '操作',
    width: 280,
    align: 'center',
    fixed: 'right',
    renderCell: ({ row }) => (
      <div class="row-actions">
        <Button
          type="link"
          size="small"
          onClick={() => openDetail(row)}
        >
          详情
        </Button>
        {row.status === 'T' && (
          <Button
            type="link"
            size="small"
            onClick={() => openForecastConfirm(row)}
          >
            预报
          </Button>
        )}
        {(row.status === 'T' || row.status === 'S') && (
          <>
            <Button
              type="link"
              size="small"
              onClick={() => openEditOrder(row)}
            >
              编辑
            </Button>
            <Button
              type="link"
              size="small"
              danger
              onClick={() => openDeleteConfirm(row)}
            >
              删除
            </Button>
          </>
        )}
        <Button
          type="link"
          size="small"
          onClick={() => openLogModal(row)}
        >
          日志
        </Button>
      </div>
    ),
  },
]

/**
 * 页面级响应适配器。
 * 当前后端返回 records/total/page/size；
 * 如果后端改成 InnerList/TotalCount，只需要在这里改映射。
 */
function formatOrderResult(result: unknown): TableResult<Order> {
  const page = result as Partial<PageResult<Order>> | null | undefined
  return {
    data: page?.records ?? [],
    total: page?.total ?? 0,
    page: page?.page,
    size: page?.size,
  }
}

function buildOrderQuery(page: number, size: number): TableFetchParams {
  return {
    page,
    size,
    keyword: orderStore.filters.keyword.trim() || undefined,
    status: orderStore.filters.status,
  }
}

function fetchOrders(params: TableFetchParams) {
  return getOrderList(params as unknown as OrderQuery)
}

function openCreateModal() {
  draftSaving.value = false
  createModal.open()
  void ensureShippingOptions()
}

async function ensureShippingOptions() {
  if (shippingOptionsLoaded || shippingOptionsLoading.value) return

  shippingOptionsLoading.value = true
  try {
    shippingOptions.value = await getShippingOptions()
    shippingOptionsLoaded = true
  } catch (error) {
    message.error(error instanceof Error ? error.message : '运输方式加载失败')
  } finally {
    shippingOptionsLoading.value = false
  }
}

async function handleCreateOrder(payload: CreateOrderPayload) {
  createModal.setLoading(true)

  try {
    const created = await createOrder(payload)
    createModal.setLoading(false)
    createModal.close()
    message.success(`订单 ${created.orderNo} 创建成功`)
    orderStore.search()
  } catch (error) {
    createModal.setLoading(false)
    message.error(error instanceof Error ? error.message : '创建订单失败')
  }
}

async function handleSaveDraft(payload: CreateDraftPayload) {
  if (draftSaving.value) return
  draftSaving.value = true

  try {
    const draft = await createDraftOrder(payload)
    draftSaving.value = false
    createModal.close()
    message.success(`草稿 ${draft.orderNo} 已保存`)
    orderStore.search()
  } catch (error) {
    draftSaving.value = false
    message.error(error instanceof Error ? error.message : '保存草稿失败')
  }
}

function openEditOrder(order: Order) {
  void ensureShippingOptions()
  editModal.open(order)
}

async function handleUpdateOrder(payload: CreateOrderPayload) {
  const order = editingOrder.value
  if (!order) return

  editModal.setLoading(true)
  try {
    const updated = await updateOrder(order.id, payload)
    editModal.setLoading(false)
    editModal.close()
    message.success(
      updated.status === 'T'
        ? '订单已修改并退回草稿'
        : '订单修改成功',
    )
    orderStore.search()
  } catch (error) {
    editModal.setLoading(false)
    message.error(error instanceof Error ? error.message : '修改订单失败')
  }
}

function openForecastConfirm(order: Order) {
  forecastConfirm.open(order)
}

async function handleConfirmForecast() {
  const order = forecastTarget.value
  if (!order) return

  forecastConfirm.setLoading(true)
  try {
    await forecastOrder(order.id)
    forecastConfirm.setLoading(false)
    forecastConfirm.close()
    message.success(`订单 ${order.orderNo} 已预报`)
    orderStore.search()
  } catch (error) {
    forecastConfirm.setLoading(false)
    message.error(error instanceof Error ? error.message : '预报订单失败')
  }
}

function handleSelectionChange(orders: Order[]) {
  selectedOrders.value = orders
}

function openLogModal(order: Order) {
  logModal.open(order)
}

function openDetail(order: Order) {
  detailModal.open(order)
}

function openDeleteConfirm(order: Order) {
  deleteConfirm.open({
    ids: [order.id],
    label: `订单 ${order.orderNo}`,
  })
}

function openBatchDeleteConfirm() {
  if (!canBatchDelete.value) return
  deleteConfirm.open({
    ids: [...selectedOrderIds.value],
    label: `选中的 ${selectedOrderIds.value.length} 个订单`,
  })
}

function openBatchExportConfirm() {
  if (!selectedOrderIds.value.length) return
  exportConfirm.open({
    ids: [...selectedOrderIds.value],
    label: `选中的 ${selectedOrderIds.value.length} 个订单`,
  })
}

async function handleConfirmExport() {
  const target = exportTarget.value
  if (!target) return

  exportConfirm.setLoading(true)
  try {
    const file = await exportOrders(target.ids)
    downloadBlob(file.blob, file.fileName)
    exportConfirm.setLoading(false)
    exportConfirm.close()
    message.success(`已导出 ${target.ids.length} 个订单`)
  } catch (error) {
    exportConfirm.setLoading(false)
    message.error(error instanceof Error ? error.message : '导出订单失败')
  }
}

async function handleConfirmDelete() {
  const target = deleteTarget.value
  if (!target) return

  deleteConfirm.setLoading(true)
  try {
    const deleted = await deleteOrders(target.ids)
    deleteConfirm.setLoading(false)
    deleteConfirm.close()
    selectedOrders.value = []
    message.success(`已删除 ${deleted} 个订单`)
    orderStore.refresh()
  } catch (error) {
    deleteConfirm.setLoading(false)
    message.error(error instanceof Error ? error.message : '删除订单失败')
  }
}
</script>

<style scoped>
.order-filters {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px 28px;
}

.order-filters__create {
  flex: none;
  border-radius: 8px;
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.2);
}

.filter-form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  row-gap: 14px;
}

.filter-form :deep(.ant-form-item) {
  margin-right: 22px;
  margin-bottom: 0;
}

.filter-form :deep(.ant-form-item-label > label) {
  color: #657186;
  font-size: 13px;
}

.filter-form__keyword {
  width: 292px;
}

.filter-form__status {
  width: 160px;
}

.batch-actions__label {
  color: #98a3b3;
  font-size: 12px;
}

.delete-confirm {
  padding: 4px 0 2px;
  color: #4b5567;
  font-size: 14px;
  line-height: 1.75;
}

.delete-confirm p {
  margin: 0;
}

.delete-confirm strong {
  color: #162033;
  font-weight: 600;
}

.delete-confirm__hint {
  margin-top: 6px !important;
  color: #98a3b3;
  font-size: 12px;
}

.export-confirm {
  padding: 4px 0 2px;
  color: #4b5567;
  font-size: 14px;
  line-height: 1.75;
}

.export-confirm p {
  margin: 0;
}

.export-confirm strong {
  color: #162033;
  font-weight: 600;
}

.export-confirm__hint {
  margin-top: 6px !important;
  color: #98a3b3;
  font-size: 12px;
}

.forecast-confirm {
  padding: 4px 0 2px;
  color: #4b5567;
  font-size: 14px;
  line-height: 1.75;
}

.forecast-confirm p {
  margin: 0;
}

.forecast-confirm strong {
  color: #162033;
  font-weight: 600;
}

.forecast-confirm__hint {
  margin-top: 6px !important;
  color: #98a3b3;
  font-size: 12px;
}

:global(.shipping-cell) {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  line-height: 1.35;
}

:global(.shipping-cell__method) {
  overflow: hidden;
  color: #2c3648;
  font-size: 12.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.shipping-cell__country) {
  color: #8a96a8;
  font-size: 11.5px;
}

:global(.order-amount) {
  color: #162033;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

:global(.order-amount--empty) {
  color: #a1abba;
  font-weight: 400;
}

:global(.order-time) {
  color: #7c889b;
  font-variant-numeric: tabular-nums;
}

:global(.row-actions) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

:global(.row-actions .ant-btn) {
  padding: 0 6px;
}

@media (max-width: 760px) {
  .order-filters {
    align-items: stretch;
    flex-direction: column;
  }

  .order-filters__create {
    width: 100%;
  }

  .filter-form,
  .filter-form :deep(.ant-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .filter-form__keyword,
  .filter-form__status {
    width: 100%;
  }

  .filter-form__actions :deep(.ant-space) {
    width: 100%;
  }

  .filter-form__actions :deep(.ant-space-item) {
    flex: 1;
  }

  .filter-form__actions :deep(.ant-btn) {
    width: 100%;
  }

  .batch-actions__label {
    display: none;
  }
}
</style>
