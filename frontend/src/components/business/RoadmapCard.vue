<template>
  <article class="roadmap-card glass-panel">
    <BaseTag tone="accent">{{ item.level }}</BaseTag>
    <h3>{{ item.title }}</h3>
    <p>{{ item.summary }}</p>
    <div class="roadmap-card__meta">
      <span>{{ item.author }}</span>
      <span>{{ item.stageCount }} 个阶段</span>
      <span>{{ item.followers }} 人跟学</span>
    </div>
    <RouterLink :to="`/roadmaps/${item.id}`">进入路线</RouterLink>
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
    author: string
    level: string
    stageCount: number
    followers: string
  }
}>()
</script>

<style scoped lang="scss">
.roadmap-card {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-6);
}

.roadmap-card h3,
.roadmap-card p {
  margin: 0;
}

.roadmap-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
</style>
