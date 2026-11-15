<template>
  <BaseModal
    :open="open"
    title="操作日志"
    width="580px"
    :closable="!loading"
    @cancel="handleClose"
  >
    <div v-if="order" class="order-log__meta">
      订单号 <strong>{{ order.orderNo }}</strong>
    </div>

    <a-spin :spinning="loading">
      <a-alert
        v-if="errorMessage"
        class="order-log__error"
        type="error"
        show-icon
        :message="errorMessage"
      />

      <a-empty
        v-else-if="!logs.length && !loading"
        description="暂无操作记录"
      />

      <a-timeline v-else class="order-log__timeline">
        <a-timeline-item
          v-for="log in logs"
          :key="log.id"
        >
          <div class="order-log__item">
            <div class="order-log__item-head">
              <strong>{{ log.actionLabel }}</strong>
              <span>{{ formatDateTime(log.createdAt) }}</span>
            </div>
            <p>{{ log.description }}</p>
            <small>操作人：{{ log.operator }}</small>
          </div>
        </a-timeline-item>
      </a-timeline>
    </a-spin>

    <template #footer>
      <a-button :disabled="loading" @click="handleClose">关闭</a-button>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { formatDateTime } from '@/utils/format'
import type { Order, OrderLog } from '@/types/order'

const props = defineProps<{
  open: boolean
  order?: Order | null
  fetchApi: (orderId: number) => Promise<OrderLog[]>
}>()

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
}>()

const logs = ref<OrderLog[]>([])
const loading = ref(false)
const errorMessage = ref('')
let requestSeq = 0

async function loadLogs() {
  const order = props.order
  if (!order) return

  const seq = ++requestSeq
  loading.value = true
  errorMessage.value = ''
  logs.value = []

  try {
    const result = await props.fetchApi(order.id)
    if (seq !== requestSeq) return
    logs.value = result
  } catch (error) {
    if (seq !== requestSeq) return
    errorMessage.value = error instanceof Error ? error.message : '操作日志加载失败'
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

function handleClose() {
  if (loading.value) return
  emit('update:open', false)
}

watch(
  [() => props.open, () => props.order],
  ([open]) => {
    if (open) {
      void loadLogs()
    }
  },
)
</script>

<style scoped>
.order-log__meta {
  margin-bottom: 16px;
  padding: 9px 12px;
  border-radius: 8px;
  background: #f7f9fc;
  color: #657186;
  font-size: 13px;
}

.order-log__meta strong {
  color: #162033;
  font-weight: 600;
}

.order-log__error {
  margin-bottom: 10px;
}

.order-log__timeline {
  padding: 8px 2px 0;
}

.order-log__item {
  padding-bottom: 2px;
}

.order-log__item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-log__item-head strong {
  color: #162033;
  font-size: 14px;
  font-weight: 600;
}

.order-log__item-head span {
  color: #98a3b3;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.order-log__item p {
  margin: 4px 0;
  color: #5e6b7f;
  font-size: 13px;
  line-height: 1.7;
}

.order-log__item small {
  color: #a1abba;
  font-size: 12px;
}
</style>
