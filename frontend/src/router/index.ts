import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
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
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/orders',
    },
  ],
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '订单中心'
  document.title = `${title} · OrderCenter`
})

export default router
