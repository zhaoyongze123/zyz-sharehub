<template>
  <div class="meta">
    <span>{{ author }}</span>
    <span>{{ category }}</span>
    <span>{{ updatedAt }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  author: string
  category: string
  updatedAt: string
}>()
</script>

<style scoped lang="scss">
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
</style>
