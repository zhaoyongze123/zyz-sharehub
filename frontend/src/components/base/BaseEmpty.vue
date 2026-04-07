<template>
  <div class="state-panel glass-panel">
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<style scoped lang="scss">
.state-panel {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-6);
}

.state-panel p {
  margin: 0;
  color: var(--color-text-soft);
}
</style>
