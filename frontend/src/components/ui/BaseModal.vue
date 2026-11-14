<template>
  <a-modal
    v-bind="$attrs"
    :open="open"
    :mask-closable="false"
  >
    <slot />
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </a-modal>
</template>

<script setup lang="ts">
/**
 * 弹窗基础组件。
 *
 * Ant Design Vue 的 ConfigProvider 没有提供全局的 Modal 默认 props，
 * 因此用这一层统一收口公共行为：
 * - 点击遮罩不关闭
 * - 其他 props / 事件继续透传给 a-modal
 *
 * 后续所有业务弹窗都使用 BaseModal，不要直接使用 a-modal。
 */
defineOptions({ name: 'BaseModal', inheritAttrs: false })

defineProps<{
  open: boolean
}>()
</script>
