<template>
  <BaseModal
    :open="open"
    title="新增订单"
    width="640px"
    ok-text="保存订单"
    cancel-text="取消"
    :confirm-loading="loading"
    :cancel-button-props="{ disabled: loading }"
    :closable="!loading"
    @ok="handleOk"
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

      <div class="order-form__summary">
        <div>
          <span>预计金额</span>
          <small>最终金额以后端计算为准</small>
        </div>
        <strong>¥ {{ formatCurrency(estimatedAmount) }}</strong>
      </div>
    </a-form>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import BaseModal from '@/components/ui/BaseModal.vue'
import { COUNTRY_OPTIONS } from '@/constants/countries'
import { formatCurrency } from '@/utils/format'
import type { CreateOrderPayload, ShippingOption } from '@/types/order'

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
    shippingOptions?: ShippingOption[]
    shippingOptionsLoading?: boolean
  }>(),
  {
    loading: false,
    shippingOptions: () => [],
    shippingOptionsLoading: false,
  },
)

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
  (event: 'submit', payload: CreateOrderPayload): void
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

function resetForm() {
  Object.assign(form, emptyForm())
  nextTick(() => formRef.value?.clearValidate())
}

function handleShippingMethodChange() {
  form.recipientCountry = undefined
  nextTick(() => formRef.value?.clearValidate('recipientCountry'))
}

watch(
  () => props.open,
  (open) => {
    if (open) resetForm()
  },
)

async function handleOk() {
  if (props.loading || !formRef.value) return

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

  emit('submit', {
    shippingMethod: form.shippingMethod,
    recipientCountry: form.recipientCountry,
    customerName: form.customerName.trim(),
    customerPhone: form.customerPhone.trim(),
    productName: form.productName.trim(),
    quantity: form.quantity,
    unitPrice: form.unitPrice,
    remark: form.remark.trim() || undefined,
  })
}

function handleCancel() {
  if (props.loading) return
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
</style>
