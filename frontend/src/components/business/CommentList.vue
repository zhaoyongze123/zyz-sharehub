<template>
  <div class="comments">
    <article v-for="item in items" :key="item.author + item.createdAt" class="comments__item glass-panel">
      <strong>{{ item.author }}</strong>
      <span>{{ item.createdAt }}</span>
      <p>{{ item.content }}</p>
    </article>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ author: string; createdAt: string; content: string }>
}>()
</script>

<style scoped lang="scss">
.comments {
  display: grid;
  gap: var(--space-4);
}

.comments__item {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
}

.comments__item span,
.comments__item p {
  margin: 0;
  color: var(--color-text-soft);
}
</style>
