<template>
  <div class="search-bar glass-panel">
    <input :value="modelValue" :placeholder="placeholder" @input="onInput" />
    <button @click="$emit('submit')">搜索</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  submit: []
}>()

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}
</script>

<style scoped lang="scss">
.search-bar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--space-3);
  padding: var(--space-3);
}

.search-bar input {
  min-height: 3rem;
  border: 0;
  background: transparent;
}

.search-bar button {
  padding: var(--space-2) var(--space-4);
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: white;
}
</style>
