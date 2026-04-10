<template>
  <button
    class="base-button"
    :class="[`base-button--${variant}`, `base-button--${size}`, { 'is-block': block, 'is-disabled': disabled }]"
    :type="nativeType"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
    size?: 'sm' | 'md' | 'lg'
    block?: boolean
    nativeType?: 'button' | 'submit' | 'reset'
    disabled?: boolean
  }>(),
  {
    variant: 'primary',
    size: 'md',
    block: false,
    nativeType: 'button',
    disabled: false
  }
)
</script>

<style scoped lang="scss">
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: transform var(--duration-fast) var(--easing-standard), box-shadow var(--duration-fast) var(--easing-standard), background var(--duration-fast) var(--easing-standard);
}

.base-button:disabled,
.is-disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none !important;
  box-shadow: none !important;
}

.base-button:hover {
  transform: translateY(-0.0625rem);
}

.base-button--sm {
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-xs);
}

.base-button--md {
  padding: var(--space-3) var(--space-4);
  font-size: var(--font-size-sm);
}

.base-button--lg {
  padding: var(--space-4) var(--space-6);
  font-size: var(--font-size-md);
}

.base-button--primary {
  background: var(--color-primary);
  color: white;
  box-shadow: var(--shadow-primary);
}

.base-button--secondary {
  background: rgba(255, 255, 255, 0.72);
  border-color: var(--color-border-soft);
  color: var(--color-text-main);
}

.base-button--ghost {
  background: transparent;
  color: var(--color-text-soft);
}

.base-button--danger {
  background: var(--color-danger);
  color: white;
}

.is-block {
  width: 100%;
}
</style>
