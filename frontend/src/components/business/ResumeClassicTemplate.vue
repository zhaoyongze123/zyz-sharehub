<template>
  <ResumeTemplateRenderer ref="rendererRef" :document="document" :theme="theme" @focus-field="handleFocusField" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ResumeTemplateRenderer from './ResumeTemplateRenderer.vue'
import type { ResumeDocument } from '@/features/resume-parser'

defineProps<{
  document: ResumeDocument
}>()

const emit = defineEmits<{
  (event: 'focus-field', sectionId: string, targetId: string): void
}>()

function handleFocusField(sectionId: string, targetId: string) {
  emit('focus-field', sectionId, targetId)
}

const rendererRef = ref<InstanceType<typeof ResumeTemplateRenderer> | null>(null)

defineExpose({
  get resumeRef() {
    return rendererRef.value?.resumeRef ?? null
  }
})

const theme = {
  wrapperClass: '',
  sectionClass: '',
  basicSectionClass: 'border-b-2 border-gray-800 pb-6',
  basicHeaderClass: 'flex items-start gap-6',
  basicTitleWrapClass: 'flex flex-wrap items-end gap-3',
  nameClass: 'text-3xl font-extrabold tracking-wider text-gray-900',
  intentClass: 'text-lg font-medium text-gray-500 border-l-2 border-gray-300 pl-3',
  metaGridClass: 'mt-4 flex flex-wrap gap-x-4 gap-y-2 text-sm',
  headingWrapClass: 'mb-3 border-b border-gray-300 pb-1',
  headingClass: 'text-base font-bold uppercase tracking-wide text-gray-800'
}
</script>
