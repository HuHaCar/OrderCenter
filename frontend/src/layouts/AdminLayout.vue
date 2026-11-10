<template>
  <a-layout class="admin-layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      :width="224"
      :collapsed-width="72"
      class="admin-sider"
    >
      <div class="admin-brand">
        <span class="admin-brand__mark">OC</span>
        <span v-show="!collapsed" class="admin-brand__name">OrderCenter</span>
      </div>

      <a-menu
        mode="inline"
        theme="light"
        :selected-keys="selectedKeys"
        class="admin-menu"
        @click="handleMenuClick"
      >
        <a-menu-item key="/orders">
          <ShoppingCartOutlined />
          <span>订单中心</span>
        </a-menu-item>
        <a-menu-item key="/dashboard" disabled>
          <AppstoreOutlined />
          <span>数据看板</span>
        </a-menu-item>
        <a-menu-item key="/settings" disabled>
          <SettingOutlined />
          <span>系统设置</span>
        </a-menu-item>
      </a-menu>

      <div v-show="!collapsed" class="admin-sider__footer">
        <span class="admin-sider__dot"></span>
        <span>本地开发 · localhost:8080</span>
      </div>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="admin-header">
        <div class="admin-header__left">
          <a-button type="text" class="admin-header__trigger" @click="collapsed = !collapsed">
            <MenuUnfoldOutlined v-if="collapsed" />
            <MenuFoldOutlined v-else />
          </a-button>
          <div>
            <div class="admin-header__eyebrow">ORDER OPERATIONS</div>
            <div class="admin-header__title">{{ pageTitle }}</div>
          </div>
        </div>

        <div class="admin-header__right">
          <span class="admin-header__env">DEV</span>
          <a-avatar class="admin-header__avatar">OC</a-avatar>
        </div>
      </a-layout-header>

      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AppstoreOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const selectedKeys = computed(() => [route.path])
const pageTitle = computed(() =>
  typeof route.meta.title === 'string' ? route.meta.title : '订单中心',
)

function handleMenuClick({ key }: { key: string }) {
  if (key === '/orders') {
    router.push(key)
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--app-bg);
}

.admin-sider {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: hidden;
  background: #fff;
  border-right: 1px solid var(--app-border);
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 72px;
  padding: 0 22px;
  border-bottom: 1px solid var(--app-border);
}

.admin-brand__mark {
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

.admin-brand__name {
  color: #162033;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.admin-menu {
  padding: 14px 10px;
  border-inline-end: 0 !important;
}

.admin-menu :deep(.ant-menu-item) {
  height: 44px;
  line-height: 44px;
  margin-block: 4px;
  border-radius: 10px;
  color: #526079;
}

.admin-menu :deep(.ant-menu-item-selected) {
  color: #0958d9;
  font-weight: 600;
  background: #eaf2ff;
}

.admin-sider__footer {
  position: absolute;
  right: 20px;
  bottom: 22px;
  left: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid var(--app-border);
  color: #9aa6b8;
  font-size: 11px;
  white-space: nowrap;
}

.admin-sider__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid var(--app-border);
  backdrop-filter: blur(12px);
}

.admin-header__left,
.admin-header__right {
  display: flex;
  align-items: center;
}

.admin-header__left {
  gap: 14px;
}

.admin-header__trigger {
  color: #5d6b82;
}

.admin-header__eyebrow {
  color: #9aa6b8;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
  line-height: 1.4;
}

.admin-header__title {
  color: #162033;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.3;
}

.admin-header__right {
  gap: 14px;
}

.admin-header__env {
  padding: 3px 8px;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
  background: #f0f6ff;
  color: #1677ff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.admin-header__avatar {
  background: #162033;
  color: #fff;
  font-size: 11px;
}

.admin-content {
  min-width: 0;
  padding: 26px 28px 40px;
}

@media (max-width: 900px) {
  .admin-sider {
    display: none;
  }

  .admin-header {
    padding: 0 16px;
  }

  .admin-header__trigger {
    display: none;
  }

  .admin-content {
    padding: 18px 14px 30px;
  }
}
</style>
