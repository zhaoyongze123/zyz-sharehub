<template>
  <Teleport to="body">
    <Transition name="fade-up">
      <div v-if="visible" class="dialog-mask" @click.self="$emit('close')">
        <div class="dialog-panel glass-panel">
          <div class="section-heading">
            <h3>{{ title }}</h3>
            <p>{{ description }}</p>
          </div>
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  title: string
  description: string
}>()

defineEmits<{
  close: []
}>()
</script>

<style scoped lang="scss">
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: var(--z-dialog);
  display: grid;
  place-items: center;
  background: rgba(20, 32, 51, 0.4);
}

.dialog-panel {
  width: min(32rem, calc(100vw - 2rem));
  padding: var(--space-6);
}
</style>
