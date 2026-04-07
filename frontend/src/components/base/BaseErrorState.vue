<template>
  <BaseEmpty :title="title" :description="description">
    <slot />
  </BaseEmpty>
</template>

<script setup lang="ts">
import BaseEmpty from './BaseEmpty.vue'

withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '请求失败',
    description: '服务暂时不可用，请稍后重试。'
  }
)
</script>
