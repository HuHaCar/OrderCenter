<template>
  <TableView
    :fetch-api="getOrderList"
    :columns="orderColumns"
    :format-result="formatOrderResult"
    @forecast="openForecastConfirm"
    @edit="openEditOrder"
    @delete="openDeleteConfirm"
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
        <span class="batch-actions__label">已选 {{ selectedOrderIds.length }} 项</span>
        <a-button
          size="small"
          danger
          :disabled="!selectedOrderIds.length"
          @click="openBatchDeleteConfirm"
        >
          批量删除
        </a-button>
        <a-button size="small" disabled title="下一阶段接入">导出订单</a-button>
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

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import TableView from '@/components/page/TableView.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import OrderFormModal from '@/components/business/OrderFormModal.vue'
import { createDraftOrder, createOrder, deleteOrders, forecastOrder, getOrderList, getShippingOptions, updateOrder } from '@/api/order'
import { ORDER_STATUS_OPTIONS } from '@/constants/order'
import { useModal } from '@/composables/useModal'
import { useOrderStore } from '@/stores/order'
import type { BaseTableColumn, TableResult } from '@/types/table'
import type { CreateDraftPayload, CreateOrderPayload, Order, PageResult, ShippingOption } from '@/types/order'

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
const selectedOrderIds = ref<number[]>([])
interface DeleteTarget {
  ids: number[]
  label: string
}
const deleteConfirm = useModal<DeleteTarget>()
const deleteTarget = deleteConfirm.data
let shippingOptionsLoaded = false
const isSearching = computed(
  () => orderStore.loading && orderStore.loadingAction === 'search',
)
const isResetting = computed(
  () => orderStore.loading && orderStore.loadingAction === 'reset',
)

const orderColumns: BaseTableColumn[] = [
  { key: 'serial', field: 'serial', title: '序号', width: 60, align: 'center' },
  { key: 'orderNo', field: 'orderNo', title: '订单号', width: 160 },
  { key: 'shipping', field: 'shippingMethod', title: '运输方式', width: 170 },
  { key: 'customerName', field: 'customerName', title: '客户', width: 95 },
  { key: 'customerPhone', field: 'customerPhone', title: '联系电话', width: 125 },
  { key: 'productName', field: 'productName', title: '商品', minWidth: 150 },
  { key: 'quantity', field: 'quantity', title: '数量', width: 64, align: 'right' },
  { key: 'totalAmount', field: 'totalAmount', title: '金额', width: 120, align: 'right' },
  { key: 'status', field: 'status', title: '状态', width: 90, align: 'center' },
  { key: 'createdAt', field: 'createdAt', title: '下单时间', width: 140 },
  { key: 'actions', field: 'id', title: '操作', width: 180, align: 'center', fixed: 'right' },
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
    orderStore.refresh()
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
    orderStore.refresh()
  } catch (error) {
    forecastConfirm.setLoading(false)
    message.error(error instanceof Error ? error.message : '预报订单失败')
  }
}

function handleSelectionChange(ids: number[]) {
  selectedOrderIds.value = ids
}

function openDeleteConfirm(order: Order) {
  deleteConfirm.open({
    ids: [order.id],
    label: `订单 ${order.orderNo}`,
  })
}

function openBatchDeleteConfirm() {
  if (!selectedOrderIds.value.length) return
  deleteConfirm.open({
    ids: [...selectedOrderIds.value],
    label: `选中的 ${selectedOrderIds.value.length} 个订单`,
  })
}

async function handleConfirmDelete() {
  const target = deleteTarget.value
  if (!target) return

  deleteConfirm.setLoading(true)
  try {
    const deleted = await deleteOrders(target.ids)
    deleteConfirm.setLoading(false)
    deleteConfirm.close()
    selectedOrderIds.value = []
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
