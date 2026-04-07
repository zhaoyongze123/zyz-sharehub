<template>
  <button class="template-card glass-panel" :class="{ 'is-active': active }">
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
  </button>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description: string
  active?: boolean
}>()
</script>

<style scoped lang="scss">
.template-card {
  width: 100%;
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
  text-align: left;
  border: 1px solid transparent;
}

.template-card.is-active {
  border-color: rgba(29, 111, 220, 0.3);
}

.template-card p {
  margin: 0;
  color: var(--color-text-soft);
}
</style>
