<template>
  <section class="card"><slot /></section>
</template>

<style scoped>
.card {
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}
</style>
