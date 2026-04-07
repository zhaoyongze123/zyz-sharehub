<template>
  <span class="base-tag" :class="`base-tag--${tone}`"><slot /></span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    tone?: 'default' | 'primary' | 'accent' | 'danger'
  }>(),
  {
    tone: 'default'
  }
)
</script>

<style scoped lang="scss">
.base-tag {
  display: inline-flex;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-pill);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.base-tag--default {
  background: rgba(20, 32, 51, 0.06);
  color: var(--color-text-soft);
}

.base-tag--primary {
  background: rgba(29, 111, 220, 0.08);
  color: var(--color-primary);
}

.base-tag--accent {
  background: rgba(0, 184, 148, 0.1);
  color: var(--color-accent);
}

.base-tag--danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}
</style>
