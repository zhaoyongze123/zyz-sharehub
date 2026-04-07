<template>
  <div class="skeleton" :style="{ height, width }"></div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    width?: string
    height?: string
  }>(),
  {
    width: '100%',
    height: '1rem'
  }
)
</script>

<style scoped lang="scss">
.skeleton {
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, rgba(20, 32, 51, 0.06), rgba(20, 32, 51, 0.12), rgba(20, 32, 51, 0.06));
  background-size: 200% 100%;
  animation: loading 1.2s linear infinite;
}

@keyframes loading {
  from {
    background-position: 200% 0;
  }

  to {
    background-position: -200% 0;
  }
}
</style>
