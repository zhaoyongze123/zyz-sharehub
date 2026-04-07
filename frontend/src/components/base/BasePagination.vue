<template>
  <div class="pagination">
    <button :disabled="page <= 1" @click="$emit('update:page', page - 1)">上一页</button>
    <span>第 {{ page }} / {{ totalPages }} 页</span>
    <button :disabled="page >= totalPages" @click="$emit('update:page', page + 1)">下一页</button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  total: number
  pageSize: number
}>()

defineEmits<{
  'update:page': [page: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
</script>

<style scoped lang="scss">
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding-top: var(--space-4);
}

.pagination button {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.78);
}
</style>
