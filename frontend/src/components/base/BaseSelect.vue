<template>
  <label class="field">
    <span v-if="label" class="field__label">{{ label }}</span>
    <select class="field__control" :value="modelValue" @change="onChange">
      <option v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</option>
    </select>
  </label>
</template>

<script setup lang="ts">
defineProps<{
  modelValue?: string
  label?: string
  options: Array<{ label: string; value: string }>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function onChange(event: Event) {
  emit('update:modelValue', (event.target as HTMLSelectElement).value)
}
</script>

<style scoped lang="scss">
.field {
  display: grid;
  gap: var(--space-2);
}

.field__label {
  color: var(--color-text-soft);
  font-size: var(--font-size-sm);
}

.field__control {
  min-height: 3rem;
  padding: 0 var(--space-4);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.82);
}
</style>
