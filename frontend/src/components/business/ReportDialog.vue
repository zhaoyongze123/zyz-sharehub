<template>
  <BaseDialog :visible="visible" title="举报内容" description="请填写举报原因，提交后将进入后台处理队列。" @close="$emit('close')">
    <BaseTextarea
      :model-value="reason"
      label="举报原因"
      placeholder="例如：外链失效、内容侵权、含不实信息"
      @update:model-value="$emit('update:reason', $event)"
    />
    <p v-if="errorMessage" class="dialog-error">{{ errorMessage }}</p>
    <div class="dialog-actions">
      <BaseButton variant="secondary" :disabled="submitting" @click="$emit('close')">取消</BaseButton>
      <BaseButton :loading="submitting" :disabled="submitting" @click="$emit('submit')">提交举报</BaseButton>
    </div>
  </BaseDialog>
</template>

<script setup lang="ts">
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'

defineProps<{
  visible: boolean
  reason: string
  submitting?: boolean
  errorMessage?: string
}>()

defineEmits<{
  close: []
  submit: []
  'update:reason': [value: string]
}>()
</script>

<style scoped lang="scss">
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-5);
}

.dialog-error {
  margin: var(--space-3) 0 0;
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}
</style>
