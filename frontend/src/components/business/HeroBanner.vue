<template>
  <section class="hero glass-panel">
    <BaseTag tone="primary">{{ kicker }}</BaseTag>
    <h1>{{ title }}</h1>
    <p>{{ description }}</p>
    <div class="hero__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import BaseTag from '@/components/base/BaseTag.vue'

defineProps<{
  kicker: string
  title: string
  description: string
}>()
</script>

<style scoped lang="scss">
.hero {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-10);
}

.hero h1 {
  margin: 0;
  font-size: var(--font-size-3xl);
  line-height: 1.05;
  letter-spacing: -0.05em;
}

.hero p {
  margin: 0;
  max-width: 42rem;
  color: var(--color-text-soft);
  font-size: var(--font-size-lg);
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}
</style>
