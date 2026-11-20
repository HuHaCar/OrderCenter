import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useAuthStore } from '@/stores/auth'

function safeRedirect(value: unknown) {
  return typeof value === 'string'
    && value.startsWith('/')
    && !value.startsWith('//')
    && !value.startsWith('/login')
    ? value
    : '/orders'
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/pages/auth/LoginPage.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/orders',
      children: [
        {
          path: 'orders',
          name: 'OrderCenter',
          // 路由层只负责引入“页面级组件”，页面内部的业务组件由页面自己组合。
          component: () => import('@/pages/order/OrderCenterPage.vue'),
          meta: { title: '订单中心' },
        },
        {
          path: 'sales',
          name: 'Sales',
          component: () => import('@/pages/sales/salesPage.vue'),
          meta: { title: '业务员' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/orders',
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    await authStore.bootstrap()
  }

  if (to.meta.public) {
    if (to.path === '/login' && authStore.isAuthenticated) {
      return safeRedirect(to.query.redirect)
    }
    return true
  }

  if (!authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '订单中心'
  document.title = `${title} · OrderCenter`
})

export default router
