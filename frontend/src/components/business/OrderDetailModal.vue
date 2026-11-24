<template>
  <BaseModal
    :open="open"
    title="订单详情"
    width="760px"
    :closable="!loading"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <a-alert
        v-if="errorMessage"
        class="order-detail__error"
        type="error"
        show-icon
        :message="errorMessage"
      />

      <template v-else-if="detail">
        <div class="order-detail__head">
          <div>
            <span class="order-detail__eyebrow">ORDER DETAIL</span>
            <strong class="order-detail__no">{{ detail.orderNo }}</strong>
          </div>
          <a-tag :color="statusMeta?.color">
            {{ statusMeta?.label ?? detail.statusLabel }}
          </a-tag>
        </div>

        <a-descriptions bordered :column="2" size="small" class="order-detail__descriptions">
          <a-descriptions-item label="运输方式">
            {{ detail.shippingMethodLabel || detail.shippingMethod }}
          </a-descriptions-item>
          <a-descriptions-item label="收件国家">
            {{ getCountryName(detail.recipientCountry) }}
          </a-descriptions-item>
          <a-descriptions-item label="客户姓名">
            {{ detail.customerName || '—' }}
          </a-descriptions-item>
          <a-descriptions-item label="联系电话">
            {{ detail.customerPhone || '—' }}
          </a-descriptions-item>
          <a-descriptions-item label="商品名称" :span="2">
            {{ detail.productName || '—' }}
          </a-descriptions-item>
          <a-descriptions-item label="数量">
            {{ detail.quantity ?? '—' }}
          </a-descriptions-item>
          <a-descriptions-item label="单价">
            {{ detail.unitPrice == null ? '—' : `¥ ${formatCurrency(detail.unitPrice)}` }}
          </a-descriptions-item>
          <a-descriptions-item label="总金额">
            {{ detail.totalAmount == null ? '—' : `¥ ${formatCurrency(detail.totalAmount)}` }}
          </a-descriptions-item>
          <a-descriptions-item label="创建人用户 ID">
            {{ detail.createBy ?? '—' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDateTime(detail.createdAt) }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ formatDateTime(detail.updatedAt) }}
          </a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">
            {{ detail.remark || '—' }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="order-detail__attachments">
          <div class="order-detail__section-title">
            附件
            <span>{{ detail.attachments.length }} 个</span>
          </div>

          <a-empty
            v-if="!detail.attachments.length"
            description="暂无附件"
          />

          <div v-else class="order-detail__attachment-grid">
            <div
              v-for="file in detail.attachments"
              :key="file.id"
              class="order-detail__attachment"
              :title="file.originalName"
              @click="handleFileClick(file)"
            >
              <div class="order-detail__attachment-preview">
                <a-image
                  v-if="file.image && previewUrls[file.id]"
                  :src="previewUrls[file.id]"
                  :width="72"
                  :height="72"
                  class="order-detail__attachment-image"
                  @click.stop
                />
                <FileWordOutlined
                  v-else-if="isWordFile(file.originalName)"
                  class="order-detail__attachment-icon order-detail__attachment-icon--word"
                />
                <FileExcelOutlined
                  v-else-if="isExcelFile(file.originalName)"
                  class="order-detail__attachment-icon order-detail__attachment-icon--excel"
                />
                <FileZipOutlined
                  v-else-if="isArchiveFile(file.originalName)"
                  class="order-detail__attachment-icon order-detail__attachment-icon--archive"
                />
                <FileImageOutlined
                  v-else-if="file.image"
                  class="order-detail__attachment-icon order-detail__attachment-icon--image"
                />
                <FileOutlined
                  v-else
                  class="order-detail__attachment-icon"
                />
              </div>
              <div class="order-detail__attachment-name">
                {{ file.originalName }}
              </div>
              <div class="order-detail__attachment-size">
                {{ formatFileSize(file.fileSize) }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </a-spin>

    <template #footer>
      <a-button :disabled="loading" @click="handleClose">关闭</a-button>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  FileExcelOutlined,
  FileImageOutlined,
  FileOutlined,
  FileWordOutlined,
  FileZipOutlined,
} from '@ant-design/icons-vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { downloadFile, previewFile } from '@/api/file'
import { getCountryName } from '@/constants/countries'
import { ORDER_STATUS_META } from '@/constants/order'
import type { FileAsset, OrderDetail } from '@/types/order'
import { downloadBlob } from '@/utils/download'
import { formatCurrency, formatDateTime } from '@/utils/format'

const props = defineProps<{
  open: boolean
  orderId?: number | null
  fetchApi: (id: number) => Promise<OrderDetail>
}>()

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
}>()

const detail = ref<OrderDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const previewUrls = reactive<Record<number, string>>({})
let requestSeq = 0

const statusMeta = computed(() =>
  detail.value ? ORDER_STATUS_META[detail.value.status] : undefined,
)

function extensionOf(fileName: string) {
  const dot = fileName.lastIndexOf('.')
  return dot < 0 ? '' : fileName.slice(dot + 1).toLowerCase()
}

function isWordFile(fileName: string) {
  return ['doc', 'docx'].includes(extensionOf(fileName))
}

function isExcelFile(fileName: string) {
  return ['xls', 'xlsx'].includes(extensionOf(fileName))
}

function isArchiveFile(fileName: string) {
  return ['zip', 'rar', '7z', 'tar', 'gz'].includes(extensionOf(fileName))
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function clearPreviewUrls() {
  Object.values(previewUrls).forEach((url) => URL.revokeObjectURL(url))
  Object.keys(previewUrls).forEach((key) => delete previewUrls[Number(key)])
}

async function loadPreviewImages(files: FileAsset[], seq: number) {
  await Promise.all(
    files
      .filter((file) => file.image)
      .map(async (file) => {
        try {
          const blob = await previewFile(file.id)
          if (seq === requestSeq) {
            previewUrls[file.id] = URL.createObjectURL(blob)
          }
        } catch {
          // 预览失败时保留文件图标，不阻断详情展示
        }
      }),
  )
}

async function loadDetail() {
  const orderId = props.orderId
  if (!orderId) return

  const seq = ++requestSeq
  loading.value = true
  errorMessage.value = ''
  detail.value = null
  clearPreviewUrls()

  try {
    const result = await props.fetchApi(orderId)
    if (seq !== requestSeq) return
    detail.value = result
    await loadPreviewImages(result.attachments, seq)
  } catch (error) {
    if (seq !== requestSeq) return
    errorMessage.value = error instanceof Error ? error.message : '订单详情加载失败'
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

async function handleFileClick(file: FileAsset) {
  if (file.image) return

  try {
    const blob = await downloadFile(file.id)
    downloadBlob(blob, file.originalName)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '文件下载失败')
  }
}

function handleClose() {
  if (loading.value) return
  emit('update:open', false)
}

watch(
  [() => props.open, () => props.orderId],
  ([open]) => {
    if (open) {
      void loadDetail()
    } else {
      requestSeq += 1
      clearPreviewUrls()
      detail.value = null
      errorMessage.value = ''
    }
  },
)

onBeforeUnmount(() => {
  requestSeq += 1
  clearPreviewUrls()
})
</script>

<style scoped>
.order-detail__error {
  margin-bottom: 12px;
}

.order-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.order-detail__eyebrow {
  display: block;
  color: #98a3b3;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.order-detail__no {
  display: block;
  margin-top: 4px;
  color: #162033;
  font-size: 20px;
  font-weight: 700;
}

.order-detail__descriptions :deep(.ant-descriptions-item-label) {
  color: #7c889b;
  font-size: 12px;
}

.order-detail__descriptions :deep(.ant-descriptions-item-content) {
  color: #2c3648;
  font-size: 13px;
}

.order-detail__attachments {
  margin-top: 20px;
}

.order-detail__section-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
  color: #162033;
  font-size: 14px;
  font-weight: 600;
}

.order-detail__section-title span {
  color: #98a3b3;
  font-size: 11px;
  font-weight: 400;
}

.order-detail__attachment-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.order-detail__attachment {
  width: 150px;
  padding: 9px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fbfcff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.order-detail__attachment:hover {
  border-color: #b9d3ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.1);
}

.order-detail__attachment-preview {
  display: grid;
  place-items: center;
  height: 76px;
}

.order-detail__attachment-image {
  overflow: hidden;
  border-radius: 6px;
  object-fit: cover;
}

.order-detail__attachment-icon {
  color: #7c889b;
  font-size: 32px;
}

.order-detail__attachment-icon--word {
  color: #2b579a;
}

.order-detail__attachment-icon--excel {
  color: #217346;
}

.order-detail__attachment-icon--archive {
  color: #d08a21;
}

.order-detail__attachment-icon--image {
  color: #7a5af8;
}

.order-detail__attachment-name {
  overflow: hidden;
  color: #2c3648;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-detail__attachment-size {
  margin-top: 2px;
  color: #98a3b3;
  font-size: 11px;
}
</style>
