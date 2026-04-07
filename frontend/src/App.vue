<template>
  <RouterView />
  <div class="global-toast-stack">
    <TransitionGroup name="toast">
      <div
        v-for="toast in appStore.toasts"
        :key="toast.id"
        class="global-toast"
        :class="{ 'is-error': toast.type === 'error' }"
      >
        <strong>{{ toast.title }}</strong>
        <p>{{ toast.message }}</p>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style scoped lang="scss">
.global-toast-stack {
  position: fixed;
  right: var(--space-6);
  bottom: var(--space-6);
  z-index: var(--z-toast);
  display: grid;
  gap: var(--space-3);
}

.global-toast {
  min-width: 18rem;
  max-width: 24rem;
  padding: var(--space-4) var(--space-5);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-lg);
  background: var(--color-surface-strong);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(var(--blur-md));
}

.global-toast strong {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--color-text-main);
}

.global-toast p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.global-toast.is-error {
  border-color: var(--color-danger-soft);
}

.toast-enter-active,
.toast-leave-active {
  transition: all var(--duration-base) var(--easing-standard);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(0.75rem);
}
</style>
