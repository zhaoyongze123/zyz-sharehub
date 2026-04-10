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
  sectionClass: 'border-t border-gray-200 pt-5',
  basicSectionClass: 'bg-[#1a7db1] text-white -mx-12 -mt-10 px-12 pt-10 pb-8 border-none',
  basicHeaderClass: 'flex items-start gap-6',
  basicTitleWrapClass: 'flex flex-wrap items-end gap-3',
  nameClass: 'text-3xl font-extrabold tracking-wider text-white',
  intentClass: 'text-lg font-medium text-white/80 border-l-2 border-white/30 pl-3',
  metaGridClass: 'mt-4 flex flex-wrap gap-x-4 gap-y-2 text-sm text-blue-100',
  headingWrapClass: 'mb-3',
  headingClass: 'text-lg font-bold tracking-wide text-[#1a7db1]'
}
</script>
