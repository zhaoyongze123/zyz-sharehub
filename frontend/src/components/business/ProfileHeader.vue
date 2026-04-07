<template>
  <section class="profile-header glass-panel">
    <div>
      <BaseTag tone="primary">{{ role }}</BaseTag>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>
    <div class="profile-header__stats">
      <div v-for="item in stats" :key="item.label" class="profile-header__stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import BaseTag from '@/components/base/BaseTag.vue'

defineProps<{
  role: string
  title: string
  description: string
  stats: Array<{ label: string; value: string }>
}>()
</script>

<style scoped lang="scss">
.profile-header {
  display: grid;
  gap: var(--space-6);
  padding: var(--space-8);
}

.profile-header h1,
.profile-header p {
  margin: 0;
}

.profile-header__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.profile-header__stat {
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
}

.profile-header__stat span {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.profile-header__stat strong {
  display: block;
  margin-top: var(--space-2);
  font-size: var(--font-size-xl);
}

@media (max-width: 48rem) {
  .profile-header__stats {
    grid-template-columns: 1fr;
  }
}
</style>
