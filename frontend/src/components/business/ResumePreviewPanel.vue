<template>
  <section class="preview glass-panel">
    <header>
      <h3>{{ profile.name }}</h3>
      <p>{{ profile.target }}</p>
    </header>
    <div>
      <h4>亮点</h4>
      <ul>
        <li v-for="item in highlights" :key="item">{{ item }}</li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  profile: { name: string; target: string }
  highlights: string[]
}>()
</script>

<style scoped lang="scss">
.preview {
  display: grid;
  gap: var(--space-5);
  padding: var(--space-6);
}

.preview h3,
.preview h4,
.preview p,
.preview ul {
  margin: 0;
}

.preview p,
.preview ul {
  color: var(--color-text-soft);
}

.preview ul {
  padding-left: 1rem;
}
</style>
