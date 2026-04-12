<template>
  <div class="timeline">
    <article
      v-for="(item, index) in items"
      :key="item.title"
      class="timeline__item"
      @click="$emit('node-click', item, index)"
    >
      <div class="timeline__dot"></div>
      <div class="timeline__content glass-panel" role="button" tabindex="0">
        <div class="timeline__header">
          <h4>{{ item.title }}</h4>
          <div class="i-carbon-chevron-right timeline__arrow"></div>
        </div>
        <p>{{ item.description || item.summary }}</p>
        <ul class="timeline__tasks">
          <li v-for="task in item.tasks" :key="task">{{ task }}</li>
        </ul>
        <p v-if="item.attachments?.length" class="timeline__attachments">附件 {{ item.attachments.length }} 个</p>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ title: string; summary?: string; description?: string; tasks?: string[]; attachments?: Array<{ filename: string }> }>
}>()

defineEmits<{
  (e: 'node-click', item: any, index: number): void
}>()
</script>

<style scoped lang="scss">
.timeline {
  display: grid;
  gap: var(--space-4);
}

.timeline__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: var(--space-4);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.timeline__item:hover .timeline__content {
  border-color: var(--color-primary);
  background: var(--color-surface-hover);
  transform: translateX(4px);
}

.timeline__dot {
  width: 1rem;
  height: 1rem;
  margin-top: var(--space-5);
  border-radius: 50%;
  background: var(--color-primary);
  animation: pulse-glow 2.2s infinite;
  flex-shrink: 0;
}

.timeline__content {
  padding: var(--space-5);
  transition: all 0.2s ease;
  outline: none;
}

.timeline__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.timeline__header h4 {
  margin: 0;
  color: var(--color-text-primary);
}

.timeline__arrow {
  color: var(--color-text-muted);
  font-size: 1.2rem;
  opacity: 0.5;
  transition: opacity 0.2s ease;
}

.timeline__item:hover .timeline__arrow {
  opacity: 1;
  color: var(--color-primary);
}

.timeline__content p {
  margin: 0;
  color: var(--color-text-secondary);
}

.timeline__tasks {
  padding-left: 1.2rem;
  margin-top: var(--space-3);
  margin-bottom: 0;
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.timeline__tasks li {
  margin-bottom: var(--space-1);
}

.timeline__attachments {
  margin: var(--space-3) 0 0;
  color: var(--color-text-soft);
  font-size: 0.9rem;
}
</style>
