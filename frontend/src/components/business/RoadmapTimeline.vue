<template>
  <div class="timeline">
    <article v-for="item in items" :key="item.title" class="timeline__item">
      <div class="timeline__dot"></div>
      <div class="timeline__content glass-panel">
        <h4>{{ item.title }}</h4>
        <p>{{ item.summary }}</p>
        <ul>
          <li v-for="task in item.tasks" :key="task">{{ task }}</li>
        </ul>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ title: string; summary: string; tasks: string[] }>
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
}

.timeline__dot {
  width: 1rem;
  height: 1rem;
  margin-top: var(--space-4);
  border-radius: 50%;
  background: var(--color-primary);
  animation: pulse-glow 2.2s infinite;
}

.timeline__content {
  padding: var(--space-5);
}

.timeline__content h4,
.timeline__content p,
.timeline__content ul {
  margin: 0;
}

.timeline__content ul {
  padding-left: 1rem;
  margin-top: var(--space-3);
  color: var(--color-text-soft);
}
</style>
