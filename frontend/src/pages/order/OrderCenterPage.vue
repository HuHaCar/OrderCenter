<template>
  <TableView
    :fetch-api="getOrderList"
    :columns="orderColumns"
    :format-result="formatOrderResult"
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
        <span class="batch-actions__label">批量操作</span>
        <a-button size="small" disabled title="下一阶段接入">批量删除</a-button>
        <a-button size="small" disabled title="下一阶段接入">导出订单</a-button>
      </a-space>
    </template>
  </TableView>

  <a-modal
    v-model:open="createModal.state.visible"
    title="新增订单"
    :confirm-loading="createModal.state.loading"
    ok-text="关闭"
    @ok="createModal.close"
    @cancel="createModal.close"
  >
    <a-empty description="新增订单表单将在下一阶段接入" />
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import TableView from '@/components/page/TableView.vue'
import { getOrderList } from '@/api/order'
import { ORDER_STATUS_OPTIONS } from '@/constants/order'
import { useModal } from '@/composables/useModal'
import { useOrderStore } from '@/stores/order'
import type { BaseTableColumn, TableResult } from '@/types/table'
import type { Order, PageResult } from '@/types/order'

/**
 * 路由级页面：负责提供表头、响应格式、查询条件和页面级弹窗。
 * 表格的数据获取和渲染由 TableView + BusinessTable 负责。
 */
const orderStore = useOrderStore()
const createModal = useModal()
const isSearching = computed(
  () => orderStore.loading && orderStore.loadingAction === 'search',
)
const isResetting = computed(
  () => orderStore.loading && orderStore.loadingAction === 'reset',
)

const orderColumns: BaseTableColumn[] = [
  { key: 'serial', field: 'serial', title: '序号', width: 70, align: 'center' },
  { key: 'orderNo', field: 'orderNo', title: '订单号', width: 160 },
  { key: 'customerName', field: 'customerName', title: '客户', width: 105 },
  { key: 'customerPhone', field: 'customerPhone', title: '联系电话', width: 135 },
  { key: 'productName', field: 'productName', title: '商品', minWidth: 200 },
  { key: 'quantity', field: 'quantity', title: '数量', width: 74, align: 'right' },
  { key: 'totalAmount', field: 'totalAmount', title: '金额', width: 130, align: 'right' },
  { key: 'status', field: 'status', title: '状态', width: 104, align: 'center' },
  { key: 'createdAt', field: 'createdAt', title: '下单时间', width: 160 },
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
  createModal.open()
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
