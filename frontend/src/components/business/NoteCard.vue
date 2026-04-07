<template>
  <article class="note-card glass-panel">
    <div class="note-card__head">
      <BaseTag>{{ item.status }}</BaseTag>
      <span>{{ item.updatedAt }}</span>
    </div>
    <h3>{{ item.title }}</h3>
    <p>{{ item.summary }}</p>
    <div class="note-card__tags">
      <BaseTag v-for="tag in item.tags" :key="tag">{{ tag }}</BaseTag>
    </div>
    <RouterLink :to="`/notes/${item.id}`">阅读笔记</RouterLink>
  </article>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import BaseTag from '@/components/base/BaseTag.vue'

defineProps<{
  item: {
    id: number
    title: string
    summary: string
    updatedAt: string
    status: string
    tags: string[]
  }
}>()
</script>

<style scoped lang="scss">
.note-card {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-6);
}

.note-card__head,
.note-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: space-between;
}

.note-card h3,
.note-card p {
  margin: 0;
}

.note-card p,
.note-card__head span {
  color: var(--color-text-soft);
}
</style>
