<template>
  <TableView :fetch-api="getOrderList">
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
              <a-button type="primary" @click="orderStore.search">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="orderStore.reset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <a-button type="primary" class="order-filters__create" @click="handleCreate">
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
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import TableView from '@/components/page/TableView.vue'
import { getOrderList } from '@/api/order'
import { ORDER_STATUS_OPTIONS } from '@/constants/order'
import { useOrderStore } from '@/stores/order'

/**
 * 路由级页面：只负责提供页面级 TableView 所需的插槽内容。
 * 查询条件和批量操作通过插槽传入，业务表格由 TableView 负责组装。
 */
const orderStore = useOrderStore()

function handleCreate() {
  message.info('新增订单表单将在下一阶段接入')
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
