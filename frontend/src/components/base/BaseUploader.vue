<template>
  <div class="uploader glass-panel">
    <input
      ref="fileInput"
      type="file"
      :accept="accept"
      :disabled="disabled"
      :data-testid="inputTestid"
      @change="onSelect"
    />
    <p>{{ fileLabel }}</p>
    <p class="hint">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    hint?: string
    file?: File | null
    accept?: string
    disabled?: boolean
    inputTestid?: string
  }>(),
  {
    hint: '拖拽文件到这里，或点击选择本地文件',
    file: null,
    accept: '',
    disabled: false,
    inputTestid: undefined
  }
)

const emit = defineEmits<{
  (e: 'update:file', file: File | null): void
  (e: 'select', file: File | null): void
}>()

const fileInput = ref<HTMLInputElement | null>(null)

const fileLabel = computed(() => props.file?.name ?? '尚未选择文件')

function onSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null
  emit('update:file', file)
  emit('select', file)
}

function openFileDialog() {
  fileInput.value?.click()
}

defineExpose({
  openFileDialog
})

watch(
  () => props.file,
  (next) => {
    if (!next && fileInput.value) {
      fileInput.value.value = ''
    }
  }
)
</script>

<style scoped lang="scss">
.uploader {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-6);
  border-style: dashed;
}

.uploader p {
  margin: 0;
  color: var(--color-text-soft);
}

.hint {
  font-size: var(--font-size-sm);
}
</style>
