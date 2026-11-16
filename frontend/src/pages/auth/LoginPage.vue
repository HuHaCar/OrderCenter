<template>
  <div class="login-page">
    <div class="login-page__glow login-page__glow--one"></div>
    <div class="login-page__glow login-page__glow--two"></div>

    <div class="login-page__brand">
      <span class="login-page__mark">OC</span>
      <span>OrderCenter</span>
    </div>

    <a-card class="login-card" :bordered="false">
      <div class="login-card__header">
        <div class="login-card__eyebrow">ORDER OPERATIONS</div>
        <h1 class="login-card__title">登录订单中心</h1>
        <p class="login-card__subtitle">请使用系统分配的账号登录</p>
      </div>

      <a-form
        :model="form"
        :rules="rules"
        layout="vertical"
        class="login-form"
        @finish="handleSubmit"
      >
        <a-form-item label="账号" name="username">
          <a-input
            v-model:value="form.username"
            size="large"
            autocomplete="username"
            placeholder="请输入账号"
          >
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="form.password"
            size="large"
            autocomplete="current-password"
            placeholder="请输入密码"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-button
          type="primary"
          html-type="submit"
          size="large"
          block
          :loading="loading"
        >
          登录
        </a-button>
      </a-form>

      <div class="login-card__hint">
        默认账号：admin / admin123
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function safeRedirect(value: unknown) {
  return typeof value === 'string'
    && value.startsWith('/')
    && !value.startsWith('//')
    ? value
    : '/orders'
}

async function handleSubmit() {
  if (loading.value) return

  loading.value = true
  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password,
    })
    message.success(`欢迎回来，${authStore.displayName}`)
    await router.replace(safeRedirect(route.query.redirect))
  } catch (error) {
    message.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 32px 20px;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 20%, rgba(22, 119, 255, 0.16), transparent 34%),
    radial-gradient(circle at 85% 80%, rgba(9, 88, 217, 0.12), transparent 30%),
    #f4f7fc;
}

.login-page__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
  pointer-events: none;
}

.login-page__glow--one {
  top: -120px;
  left: -80px;
  width: 360px;
  height: 360px;
  background: rgba(22, 119, 255, 0.08);
}

.login-page__glow--two {
  right: -100px;
  bottom: -140px;
  width: 420px;
  height: 420px;
  background: rgba(9, 88, 217, 0.06);
}

.login-page__brand {
  position: absolute;
  top: 28px;
  left: 32px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #162033;
  font-size: 16px;
  font-weight: 700;
}

.login-page__mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #0958d9);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.03em;
  box-shadow: 0 6px 14px rgba(22, 119, 255, 0.28);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  border-radius: 18px;
  box-shadow: 0 24px 70px rgba(32, 51, 84, 0.14);
}

.login-card :deep(.ant-card-body) {
  padding: 38px 38px 30px;
}

.login-card__eyebrow {
  color: #98a3b3;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.login-card__title {
  margin: 8px 0 6px;
  color: #162033;
  font-size: 28px;
  font-weight: 750;
  letter-spacing: -0.03em;
}

.login-card__subtitle {
  margin: 0;
  color: #7c889b;
  font-size: 13px;
}

.login-form {
  margin-top: 28px;
}

.login-form :deep(.ant-form-item-label > label) {
  color: #4b5567;
  font-size: 13px;
}

.login-form :deep(.ant-input-affix-wrapper) {
  border-radius: 9px;
}

.login-form :deep(.ant-btn) {
  margin-top: 6px;
  border-radius: 9px;
  font-weight: 600;
}

.login-card__hint {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
  color: #98a3b3;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 520px) {
  .login-page__brand {
    top: 20px;
    left: 20px;
  }

  .login-card :deep(.ant-card-body) {
    padding: 30px 24px 24px;
  }

  .login-card__title {
    font-size: 24px;
  }
}
</style>
