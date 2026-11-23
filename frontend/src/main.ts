import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import { VxeLoading, VxeTooltip } from 'vxe-pc-ui'
import { VxeUI } from 'vxe-table'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'ant-design-vue/dist/reset.css'
import 'vxe-pc-ui/lib/style.css'
import 'vxe-table/lib/style.css'
import App from './App.vue'
import router from './router'
import './assets/styles/main.css'
import { setSessionExpiredHandler } from '@/auth/session'
import { useAuthStore } from '@/stores/auth'

dayjs.locale('zh-cn')

// vxe-table 的 tooltip/loading 不是 vxe-table 自带组件，需要注册到 VxeUI。
VxeUI.component(VxeTooltip)
VxeUI.component(VxeLoading)

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(Antd)

const authStore = useAuthStore(pinia)

setSessionExpiredHandler(() => {
  authStore.clear()

  if (router.currentRoute.value.path !== '/login') {
    void router.replace({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
  }
})

async function start() {
  await authStore.bootstrap()
  app.mount('#app')
}

void start()
