<template>
  <div class="outline glass-panel" data-testid="note-outline">
    <h4>目录</h4>
    <ul>
      <li v-for="item in items" :key="item">{{ item }}</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: string[]
}>()
</script>

<style scoped lang="scss">
.outline {
  padding: var(--space-5);
}

.outline h4 {
  margin: 0 0 var(--space-3);
}

.outline ul {
  margin: 0;
  padding-left: 1rem;
  color: var(--color-text-soft);
}
</style>
