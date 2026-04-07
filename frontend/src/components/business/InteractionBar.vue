<template>
  <div class="interaction-bar glass-panel">
    <BaseButton size="sm" variant="secondary" @click="$emit('like')">点赞 {{ likes }}</BaseButton>
    <BaseButton size="sm" variant="secondary" @click="$emit('favorite')">收藏 {{ favorites }}</BaseButton>
    <BaseButton size="sm" variant="secondary" @click="$emit('report')">举报</BaseButton>
  </div>
</template>

<script setup lang="ts">
import BaseButton from '@/components/base/BaseButton.vue'

defineProps<{
  likes: number
  favorites: number
}>()

defineEmits<{
  like: []
  favorite: []
  report: []
}>()
</script>

<style scoped lang="scss">
.interaction-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  padding: var(--space-4);
}
</style>
