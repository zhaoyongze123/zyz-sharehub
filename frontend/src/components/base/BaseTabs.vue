<template>
  <div class="tabs">
    <button
      v-for="item in items"
      :key="item.value"
      class="tabs__item"
      :class="{ 'is-active': item.value === modelValue }"
      @click="$emit('update:modelValue', item.value)"
    >
      {{ item.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  items: Array<{ label: string; value: string }>
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<style scoped lang="scss">
.tabs {
  display: inline-flex;
  gap: var(--space-2);
  padding: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.7);
}

.tabs__item {
  border: 0;
  border-radius: var(--radius-pill);
  padding: var(--space-2) var(--space-4);
  background: transparent;
  color: var(--color-text-soft);
  cursor: pointer;
}

.tabs__item.is-active {
  background: var(--color-surface-solid);
  color: var(--color-text-main);
  box-shadow: var(--shadow-soft);
}
</style>
