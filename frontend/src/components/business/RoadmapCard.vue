
<template>
  <article class="roadmap-card glass-panel">
    <BaseTag tone="accent">{{ item.status ?? item.level ?? '路线' }}</BaseTag>
    <h3>{{ item.title }}</h3>
    <p>{{ item.summary ?? item.description ?? '暂无简介' }}</p>
    <div class="roadmap-card__meta">
      <span>{{ item.ownerName || item.author || '社区贡献者' }}</span>
      <span v-if="item.visibility">可见性：{{ item.visibility }}</span>
      <span>{{ item.nodeCount ?? item.stageCount ?? 0 }} 个阶段</span>
      <span>{{ item.followers ?? '0' }} 人跟学</span>
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
    summary?: string | null
    description?: string | null
    author?: string | null
    level?: string | null
    ownerName?: string | null
    nodeCount?: number | null
    stageCount?: number | null
    followers?: number | string | null
    visibility?: string | null
    status?: string | null
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
