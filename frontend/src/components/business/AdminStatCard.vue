<template>
  <article class="admin-stat glass-panel">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <p>{{ description }}</p>
  </article>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: string
  description: string
}>()
</script>

<style scoped lang="scss">
.admin-stat {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
}

.admin-stat span,
.admin-stat p {
  color: var(--color-text-soft);
}

.admin-stat strong {
  font-size: var(--font-size-2xl);
}

.admin-stat p {
  margin: 0;
}
</style>
