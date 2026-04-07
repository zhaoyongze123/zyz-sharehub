<template>
  <div class="uploader glass-panel">
    <input type="file" />
    <p>{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    hint?: string
  }>(),
  {
    hint: '拖拽文件到这里，或点击选择本地文件'
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
</style>
