<template>
  <BaseModal
    :open="open"
    :title="isEdit ? '编辑订单' : '新增订单'"
    width="1000px"
    :closable="!loading && !draftLoading"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      class="order-form stable-form"
      layout="vertical"
      :model="form"
      :rules="rules"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <a-form-item label="运输方式" name="shippingMethod">
            <a-select
              v-model:value="form.shippingMethod"
              show-search
              allow-clear
              option-filter-prop="label"
              :options="shippingMethodOptions"
              :loading="shippingOptionsLoading"
              :disabled="shippingOptionsLoading || !shippingOptions.length"
              placeholder="请选择运输方式"
              @change="handleShippingMethodChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="收件国家" name="recipientCountry">
            <a-select
              v-model:value="form.recipientCountry"
              show-search
              allow-clear
              option-filter-prop="label"
              :options="countryOptions"
              :loading="shippingOptionsLoading"
              :disabled="!form.shippingMethod || shippingOptionsLoading"
              placeholder="请先选择运输方式"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <a-form-item label="客户姓名" name="customerName">
            <a-input
              v-model:value="form.customerName"
              allow-clear
              :maxlength="50"
              placeholder="请输入客户姓名"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="联系电话" name="customerPhone">
            <a-input
              v-model:value="form.customerPhone"
              allow-clear
              :maxlength="11"
              placeholder="请输入 11 位手机号"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="商品名称" name="productName">
        <a-input
          v-model:value="form.productName"
          allow-clear
          :maxlength="100"
          placeholder="请输入商品名称"
        />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <a-form-item label="数量" name="quantity">
            <a-input-number
              v-model:value="form.quantity"
              class="order-form__number"
              :min="1"
              :max="9999"
              :precision="0"
              placeholder="请输入数量"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="单价" name="unitPrice">
            <a-input-number
              v-model:value="form.unitPrice"
              class="order-form__number"
              :min="0.01"
              :precision="2"
              :step="0.01"
              placeholder="请输入单价"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark">
        <a-textarea
          v-model:value="form.remark"
          :maxlength="255"
          :rows="3"
          show-count
          placeholder="选填，最多 255 个字符"
        />
      </a-form-item>

      <a-form-item v-if="!isEdit" label="附件" class="order-form__attachment-item">
        <div class="attachment-grid">
          <div
            v-for="item in attachments"
            :key="item.uid"
            class="attachment-card"
          >
            <div
              class="attachment-card__preview"
              @click="handleAttachmentClick(item)"
            >
              <a-image
                v-if="item.image && item.previewUrl"
                :src="item.previewUrl"
                :width="64"
                :height="64"
                class="attachment-card__image"
              />
              <FileWordOutlined
                v-else-if="isWordFile(item.name)"
                class="attachment-card__icon attachment-card__icon--word"
              />
              <FileExcelOutlined
                v-else-if="isExcelFile(item.name)"
                class="attachment-card__icon attachment-card__icon--excel"
              />
              <FileZipOutlined
                v-else-if="isArchiveFile(item.name)"
                class="attachment-card__icon attachment-card__icon--archive"
              />
              <FileImageOutlined
                v-else-if="item.image"
                class="attachment-card__icon attachment-card__icon--image"
              />
              <FileOutlined
                v-else
                class="attachment-card__icon"
              />

              <a-spin
                v-if="item.status === 'uploading'"
                class="attachment-card__spinner"
                size="small"
              />
            </div>

            <div class="attachment-card__body">
              <div class="attachment-card__name" :title="item.name">
                {{ item.name }}
              </div>
              <div class="attachment-card__meta">
                <span>{{ formatFileSize(item.size) }}</span>
                <span v-if="item.status === 'uploading'">
                  {{ item.progress }}%
                </span>
                <span v-else-if="item.status === 'error'" class="attachment-card__error">
                  上传失败
                </span>
              </div>
            </div>

            <a-button
              type="text"
              size="small"
              danger
              class="attachment-card__remove"
              :disabled="item.status === 'uploading'"
              @click.stop="removeAttachment(item)"
            >
              <DeleteOutlined />
            </a-button>
          </div>

          <a-upload
            v-if="attachments.length < MAX_ATTACHMENT_COUNT"
            class="attachment-upload"
            accept=".jpg,.jpeg,.png,.gif,.webp,.bmp,.doc,.docx,.xls,.xlsx,.zip,.rar,.7z,.tar,.gz"
            :multiple="false"
            :show-upload-list="false"
            :disabled="hasUploadingAttachment"
            :before-upload="beforeAttachmentUpload"
            :custom-request="handleAttachmentUpload"
          >
            <div class="attachment-upload-card">
              <PlusOutlined class="attachment-upload-card__icon" />
              <span>上传附件</span>
              <small>最多 {{ MAX_ATTACHMENT_COUNT }} 个</small>
            </div>
          </a-upload>
        </div>
        <div class="order-form__attachment-hint">
          选填，支持图片、Word、Excel、压缩包；单个文件不超过 200MB，最多 3 个。
        </div>
      </a-form-item>

      <div class="order-form__summary">
        <div>
          <span>预计金额</span>
          <small>最终金额以后端计算为准</small>
        </div>
        <strong>¥ {{ formatCurrency(estimatedAmount) }}</strong>
      </div>
    </a-form>

    <template #footer>
      <div class="order-form__footer">
        <a-button :disabled="loading || draftLoading" @click="handleCancel">
          取消
        </a-button>
        <a-button
          v-if="!isEdit"
          :loading="draftLoading"
          :disabled="loading || hasUploadingAttachment"
          @click="handleSaveDraft"
        >
          保存草稿
        </a-button>
        <a-button
          type="primary"
          :loading="loading"
          :disabled="draftLoading || hasUploadingAttachment"
          @click="handleOk"
        >
          {{ isEdit ? '保存修改' : '保存订单' }}
        </a-button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import type { UploadRequestOption } from 'ant-design-vue/es/vc-upload/interface'
import {
  DeleteOutlined,
  FileExcelOutlined,
  FileImageOutlined,
  FileOutlined,
  FileWordOutlined,
  FileZipOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { downloadFile, uploadFile } from '@/api/file'
import { COUNTRY_OPTIONS } from '@/constants/countries'
import { downloadBlob } from '@/utils/download'
import { formatCurrency } from '@/utils/format'
import type {
  CreateDraftPayload,
  CreateOrderPayload,
  Order,
  ShippingOption,
} from '@/types/order'

interface OrderFormState {
  shippingMethod: string | undefined
  recipientCountry: string | undefined
  customerName: string
  customerPhone: string
  productName: string
  quantity: number | null
  unitPrice: number | null
  remark: string
}

const props = withDefaults(
  defineProps<{
    open: boolean
    loading?: boolean
    draftLoading?: boolean
    mode?: 'create' | 'edit'
    order?: Order | null
    shippingOptions?: ShippingOption[]
    shippingOptionsLoading?: boolean
  }>(),
  {
    loading: false,
    draftLoading: false,
    mode: 'create',
    order: null,
    shippingOptions: () => [],
    shippingOptionsLoading: false,
  },
)

const isEdit = computed(() => props.mode === 'edit')

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
  (event: 'submit', payload: CreateOrderPayload): void
  (event: 'save-draft', payload: CreateDraftPayload): void
}>()

const formRef = ref<FormInstance>()

function emptyForm(): OrderFormState {
  return {
    shippingMethod: undefined,
    recipientCountry: undefined,
    customerName: '',
    customerPhone: '',
    productName: '',
    quantity: 1,
    unitPrice: null,
    remark: '',
  }
}

const form = reactive<OrderFormState>(emptyForm())

const MAX_ATTACHMENT_COUNT = 3
const MAX_ATTACHMENT_SIZE = 200 * 1024 * 1024
const ALLOWED_ATTACHMENT_EXTENSIONS = new Set([
  'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp',
  'doc', 'docx', 'xls', 'xlsx',
  'zip', 'rar', '7z', 'tar', 'gz',
])

interface AttachmentItem {
  uid: string
  id?: number
  name: string
  size: number
  image: boolean
  status: 'uploading' | 'done' | 'error'
  progress: number
  previewUrl?: string
  rawFile: File
}

const attachments = ref<AttachmentItem[]>([])
const hasUploadingAttachment = computed(
  () => attachments.value.some((item) => item.status === 'uploading'),
)

const shippingMethodOptions = computed(() =>
  props.shippingOptions.map((method) => ({
    value: method.code,
    label: `${method.name} (${method.code})`,
  })),
)

const selectedShippingMethod = computed(() =>
  props.shippingOptions.find((method) => method.code === form.shippingMethod),
)

const countryOptions = computed(() => {
  const method = selectedShippingMethod.value
  if (!method) return []

  const allowed = new Set(method.countryCodes)
  const countries = method.allCountries
    ? COUNTRY_OPTIONS
    : COUNTRY_OPTIONS.filter((country) => allowed.has(country.code))

  return countries.map((country) => ({
    value: country.code,
    label: `${country.name} (${country.code})`,
  }))
})

const rules: Record<string, Rule[]> = {
  shippingMethod: [
    { required: true, message: '请选择运输方式', trigger: 'change' },
  ],
  recipientCountry: [
    { required: true, message: '请选择收件国家', trigger: 'change' },
  ],
  customerName: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
  ],
  customerPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的 11 位手机号',
      trigger: 'blur',
    },
  ],
  productName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
  ],
  quantity: [
    {
      required: true,
      type: 'number',
      min: 1,
      message: '数量必须大于 0',
      trigger: 'change',
    },
  ],
  unitPrice: [
    {
      required: true,
      type: 'number',
      min: 0.01,
      message: '单价必须大于 0',
      trigger: 'change',
    },
  ],
}

const estimatedAmount = computed(() => {
  const quantity = Number(form.quantity ?? 0)
  const unitPrice = Number(form.unitPrice ?? 0)
  return quantity > 0 && unitPrice > 0 ? quantity * unitPrice : 0
})

function extensionOf(fileName: string) {
  const dot = fileName.lastIndexOf('.')
  return dot < 0 ? '' : fileName.slice(dot + 1).toLowerCase()
}

function isImageFile(fileName: string, contentType = '') {
  return contentType.startsWith('image/')
    || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(extensionOf(fileName))
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

function releaseAttachmentPreview(item: AttachmentItem) {
  if (item.previewUrl) {
    URL.revokeObjectURL(item.previewUrl)
  }
}

function resetAttachments() {
  attachments.value.forEach(releaseAttachmentPreview)
  attachments.value = []
}

function beforeAttachmentUpload(file: File) {
  if (attachments.value.length >= MAX_ATTACHMENT_COUNT) {
    message.warning(`最多只能上传 ${MAX_ATTACHMENT_COUNT} 个附件`)
    return false
  }
  if (file.size > MAX_ATTACHMENT_SIZE) {
    message.error('单个附件不能超过 200MB')
    return false
  }
  const extension = extensionOf(file.name)
  if (!ALLOWED_ATTACHMENT_EXTENSIONS.has(extension)) {
    message.error('只支持图片、Word、Excel 和压缩包')
    return false
  }
  return true
}

async function handleAttachmentUpload(options: UploadRequestOption) {
  const rawFile = options.file as File
  const image = isImageFile(rawFile.name, rawFile.type)
  const item = reactive<AttachmentItem>({
    uid: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    name: rawFile.name,
    size: rawFile.size,
    image,
    status: 'uploading',
    progress: 0,
    previewUrl: image ? URL.createObjectURL(rawFile) : undefined,
    rawFile,
  })

  attachments.value.push(item)

  try {
    const result = await uploadFile(rawFile, (percent) => {
      item.progress = percent
      options.onProgress?.({ percent })
    })
    item.id = result.id
    item.image = result.image
    item.status = 'done'
    item.progress = 100
    if (item.image && !item.previewUrl) {
      item.previewUrl = URL.createObjectURL(rawFile)
    }
    options.onSuccess?.(result)
  } catch (error) {
    item.status = 'error'
    options.onError?.(error as Error)
    message.error(error instanceof Error ? error.message : '附件上传失败')
  }
}

function removeAttachment(item: AttachmentItem) {
  if (item.status === 'uploading') return
  releaseAttachmentPreview(item)
  attachments.value = attachments.value.filter((current) => current.uid !== item.uid)
}

async function handleAttachmentClick(item: AttachmentItem) {
  // 图片交给 a-image 的预览功能；其他格式点击下载
  if (item.image) return

  if (item.rawFile) {
    downloadBlob(item.rawFile, item.name)
    return
  }

  if (!item.id) return
  try {
    const blob = await downloadFile(item.id)
    downloadBlob(blob, item.name)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '文件下载失败')
  }
}

function resetForm() {
  Object.assign(form, emptyForm())
  nextTick(() => formRef.value?.clearValidate())
}

function handleShippingMethodChange() {
  form.recipientCountry = undefined
  nextTick(() => formRef.value?.clearValidate('recipientCountry'))
}

function fillForm(order: Order) {
  Object.assign(form, {
    shippingMethod: order.shippingMethod,
    recipientCountry: order.recipientCountry,
    customerName: order.customerName ?? '',
    customerPhone: order.customerPhone ?? '',
    productName: order.productName ?? '',
    quantity: order.quantity ?? null,
    unitPrice: order.unitPrice ?? null,
    remark: order.remark ?? '',
  })
  nextTick(() => formRef.value?.clearValidate())
}

watch(
  [() => props.open, () => props.order],
  ([open]) => {
    if (!open) {
      resetAttachments()
      return
    }
    resetAttachments()
    if (isEdit.value && props.order) {
      fillForm(props.order)
    } else {
      resetForm()
    }
  },
)

onBeforeUnmount(resetAttachments)

function currentAttachmentIds() {
  return attachments.value
    .filter((item) => item.status === 'done' && item.id)
    .map((item) => item.id as number)
}

async function handleOk() {
  if (props.loading || !formRef.value) return
  if (hasUploadingAttachment.value) {
    message.warning('附件正在上传，请稍候')
    return
  }

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (
    !form.shippingMethod ||
    !form.recipientCountry ||
    form.quantity == null ||
    form.unitPrice == null
  ) {
    return
  }

  const attachmentIds = currentAttachmentIds()

  emit('submit', {
    shippingMethod: form.shippingMethod,
    recipientCountry: form.recipientCountry,
    customerName: form.customerName.trim(),
    customerPhone: form.customerPhone.trim(),
    productName: form.productName.trim(),
    quantity: form.quantity,
    unitPrice: form.unitPrice,
    remark: form.remark.trim() || undefined,
    attachmentIds: attachmentIds.length ? attachmentIds : undefined,
  })
}

async function handleSaveDraft() {
  if (isEdit.value || props.loading || props.draftLoading || !formRef.value) return
  if (hasUploadingAttachment.value) {
    message.warning('附件正在上传，请稍候')
    return
  }

  try {
    await formRef.value.validateFields(['shippingMethod', 'recipientCountry'])
  } catch {
    return
  }

  if (!form.shippingMethod || !form.recipientCountry) return

  const attachmentIds = currentAttachmentIds()

  emit('save-draft', {
    shippingMethod: form.shippingMethod,
    recipientCountry: form.recipientCountry,
    customerName: form.customerName.trim() || undefined,
    customerPhone: form.customerPhone.trim() || undefined,
    productName: form.productName.trim() || undefined,
    quantity: form.quantity ?? undefined,
    unitPrice: form.unitPrice ?? undefined,
    remark: form.remark.trim() || undefined,
    attachmentIds: attachmentIds.length ? attachmentIds : undefined,
  })
}

function handleCancel() {
  if (props.loading || props.draftLoading) return
  emit('update:open', false)
}
</script>

<style scoped>
.order-form {
  padding-top: 4px;
}

.order-form__number {
  width: 100%;
}

.order-form__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 2px;
  padding: 12px 16px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #f8fafd;
}

.order-form__summary span {
  display: block;
  color: #657186;
  font-size: 13px;
}

.order-form__summary small {
  display: block;
  margin-top: 2px;
  color: #a1abba;
  font-size: 11px;
}

.order-form__summary strong {
  color: #162033;
  font-size: 20px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.order-form__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.order-form__attachment-item :deep(.ant-form-item-control-input-content) {
  width: 100%;
}

.attachment-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.attachment-card {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 150px;
  min-height: 112px;
  padding: 8px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fbfcff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.attachment-card:hover {
  border-color: #b9d3ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.1);
}

.attachment-card__preview {
  position: relative;
  display: grid;
  place-items: center;
  height: 64px;
  cursor: pointer;
}

.attachment-card__image {
  overflow: hidden;
  border-radius: 6px;
  object-fit: cover;
}

.attachment-card__icon {
  color: #7c889b;
  font-size: 30px;
}

.attachment-card__icon--word {
  color: #2b579a;
}

.attachment-card__icon--excel {
  color: #217346;
}

.attachment-card__icon--archive {
  color: #d08a21;
}

.attachment-card__icon--image {
  color: #7a5af8;
}

.attachment-card__spinner {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.65);
}

.attachment-card__body {
  min-width: 0;
  margin-top: 4px;
}

.attachment-card__name {
  overflow: hidden;
  color: #2c3648;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-top: 2px;
  color: #98a3b3;
  font-size: 11px;
}

.attachment-card__error {
  color: #ff4d4f;
}

.attachment-card__remove {
  position: absolute;
  top: 2px;
  right: 2px;
  padding: 2px;
  color: #98a3b3;
}

.attachment-upload-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 150px;
  min-height: 112px;
  border: 1px dashed #cbd7e8;
  border-radius: 10px;
  background: #fafcff;
  color: #7c889b;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease;
}

.attachment-upload-card:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.attachment-upload-card__icon {
  margin-bottom: 5px;
  font-size: 20px;
}

.attachment-upload-card span {
  font-size: 12px;
  font-weight: 600;
}

.attachment-upload-card small {
  margin-top: 2px;
  color: #a1abba;
  font-size: 10px;
}

.order-form__attachment-hint {
  margin-top: 6px;
  color: #a1abba;
  font-size: 11px;
  line-height: 1.5;
}

</style>
