<template>
  <table class="base-table">
    <slot />
  </table>
</template>

<style scoped lang="scss">
.base-table {
  width: 100%;
  border-collapse: collapse;
}

.base-table :deep(th),
.base-table :deep(td) {
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}
</style>
