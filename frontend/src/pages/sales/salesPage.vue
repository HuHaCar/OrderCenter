<template>
  <TableView
    :query="salesStore"
    :fetch-api="fetchSalesList"
    :build-query="buildSalesQuery"
    :columns="salesColumns"
    :format-result="formatSalesResult"
    title="业务员列表"
    empty-text="暂无业务员数据"
    error-text="业务员加载失败"
    :selectable="false"
  >
    <template #filters>
      <div class="sales-filters">
        <a-form layout="inline" class="filter-form" :model="salesStore.filters">
          <a-form-item label="关键词">
            <a-input
              v-model:value="salesStore.filters.keyword"
              allow-clear
              placeholder="工号 / 业务员 / 手机号"
              class="filter-form__keyword"
              @press-enter="salesStore.search"
            />
          </a-form-item>

          <a-form-item label="级别">
            <a-select
              v-model:value="salesStore.filters.lever"
              allow-clear
              placeholder="全部级别"
              :options="SALES_LEVER_OPTIONS"
              class="filter-form__lever"
              @change="salesStore.search"
            />
          </a-form-item>

          <a-form-item class="filter-form__actions">
            <a-space>
              <a-button
                type="primary"
                :loading="isSearching"
                :disabled="salesStore.loading && !isSearching"
                @click="salesStore.search"
              >
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button
                :loading="isResetting"
                :disabled="salesStore.loading && !isResetting"
                @click="salesStore.reset"
              >
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
    </template>
  </TableView>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import TableView from '@/components/page/TableView.vue'
import { getSalesList } from '@/api/sales'
import { SALES_LEVER_OPTIONS, getGenderLabel, getSalesLeverLabel } from '@/constants/sales'
import { useSalesStore } from '@/stores/sales'
import type { BaseTableColumn, TableFetchParams, TableResult } from '@/types/table'
import type { Sales, SalesPageResult, SalesQuery } from '@/types/sales'
import { formatDateTime } from '@/utils/format'

/**
 * 业务员列表页：
 * 查询接口、筛选条件、展示字段都在页面层配置，TableView 只负责布局。
 */
const salesStore = useSalesStore()

const isSearching = computed(
  () => salesStore.loading && salesStore.loadingAction === 'search',
)
const isResetting = computed(
  () => salesStore.loading && salesStore.loadingAction === 'reset',
)

const salesColumns: BaseTableColumn<Sales>[] = [
  { key: 'serial', field: 'serial', title: '序号', width: 60, align: 'center' },
  { key: 'employeeNo', field: 'employeeNo', title: '工号', width: 130 },
  { key: 'employeeName', field: 'employeeName', title: '业务员', width: 120 },
  {
    key: 'lever',
    field: 'lever',
    title: '级别',
    width: 100,
    align: 'center',
    formatter: (value) => getSalesLeverLabel(value),
  },
  {
    key: 'entryDate',
    field: 'entryDate',
    title: '入司时间',
    width: 160,
    formatter: (value) => formatDateTime(value as string | null | undefined),
  },
  {
    key: 'gender',
    field: 'gender',
    title: '性别',
    width: 80,
    align: 'center',
    formatter: (value) => getGenderLabel(value),
  },
  { key: 'phone', field: 'phone', title: '手机号', width: 130 },
  { key: 'email', field: 'email', title: '邮箱', minWidth: 180 },
  { key: 'remark', field: 'remark', title: '备注', minWidth: 160 },
  {
    key: 'createdAt',
    field: 'createdAt',
    title: '创建时间',
    width: 160,
    formatter: (value) => formatDateTime(value as string | null | undefined),
  },
]

function formatSalesResult(result: unknown): TableResult<Sales> {
  const page = result as Partial<SalesPageResult<Sales>> | null | undefined
  return {
    data: page?.records ?? [],
    total: page?.total ?? 0,
    page: page?.page,
    size: page?.size,
  }
}

function buildSalesQuery(page: number, size: number): TableFetchParams {
  return {
    page,
    size,
    keyword: salesStore.filters.keyword.trim() || undefined,
    lever: salesStore.filters.lever,
  }
}

function fetchSalesList(params: TableFetchParams) {
  return getSalesList(params as unknown as SalesQuery)
}
</script>

<style scoped>
.sales-filters {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px 28px;
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

.filter-form__lever {
  width: 160px;
}

@media (max-width: 760px) {
  .filter-form,
  .filter-form :deep(.ant-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .filter-form__keyword,
  .filter-form__lever {
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
}
</style>
