<template>
  <div class="review-panel glass-panel">
    <BaseTextarea :model-value="reason" label="处理说明" placeholder="填写驳回或通过备注" @update:model-value="$emit('update:reason', $event)" />
    <div class="review-panel__actions">
      <BaseButton variant="danger" :disabled="rejectDisabled" @click="$emit('reject')">{{ rejectLabel }}</BaseButton>
      <BaseButton :disabled="approveDisabled" @click="$emit('approve')">{{ approveLabel }}</BaseButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import BaseButton from '@/components/base/BaseButton.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'

withDefaults(
  defineProps<{
    reason: string
    approveLabel?: string
    rejectLabel?: string
    approveDisabled?: boolean
    rejectDisabled?: boolean
  }>(),
  {
    approveLabel: '通过',
    rejectLabel: '驳回',
    approveDisabled: false,
    rejectDisabled: false
  }
)

defineEmits<{
  'update:reason': [value: string]
  approve: []
  reject: []
}>()
</script>

<style scoped lang="scss">
.review-panel {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5);
}

.review-panel__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>
