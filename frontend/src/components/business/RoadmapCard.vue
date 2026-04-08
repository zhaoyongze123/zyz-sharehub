<template>
  <article class="roadmap-card glass-panel">
    <BaseTag tone="accent">{{ item.status ?? item.level ?? 'PUBLISHED' }}</BaseTag>
    <h3>{{ item.title }}</h3>
    <p>{{ item.summary ?? item.description ?? '暂无简介' }}</p>
    <div class="roadmap-card__meta">
      <span v-if="item.author">{{ item.author }}</span>
      <span>可见性：{{ item.visibility ?? 'PUBLIC' }}</span>
      <span v-if="item.stageCount">{{ item.stageCount }} 个阶段</span>
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
    summary?: string
    description?: string
    author?: string
    level?: string
    stageCount?: number
    followers?: string
    visibility?: string
    status?: string
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
