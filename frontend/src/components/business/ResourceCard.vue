<template>
  <article class="resource-card glass-panel">
    <div class="resource-card__head">
      <BaseTag tone="primary">{{ item.category }}</BaseTag>
      <BaseTag>{{ item.fileType }}</BaseTag>
    </div>
    <h3>{{ item.title }}</h3>
    <p>{{ item.summary }}</p>
    <ResourceMeta :author="item.author" :category="item.category" :updated-at="item.updatedAt" />
    <div class="resource-card__footer">
      <div class="resource-card__tags">
        <BaseTag v-for="tag in item.tags" :key="tag">{{ tag }}</BaseTag>
      </div>
      <RouterLink :to="`/resources/${item.id}`">查看详情</RouterLink>
    </div>
  </article>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import BaseTag from '@/components/base/BaseTag.vue'
import ResourceMeta from './ResourceMeta.vue'

defineProps<{
  item: {
    id: number
    title: string
    summary: string
    author: string
    updatedAt: string
    category: string
    fileType: string
    tags: string[]
  }
}>()
</script>

<style scoped lang="scss">
.resource-card {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-6);
}

.resource-card h3,
.resource-card p {
  margin: 0;
}

.resource-card p {
  color: var(--color-text-soft);
}

.resource-card__head,
.resource-card__footer,
.resource-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: space-between;
}
</style>
