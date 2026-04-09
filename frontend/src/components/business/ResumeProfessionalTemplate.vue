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
  basicSectionClass: 'pb-3',
  basicHeaderClass: 'flex flex-col gap-4',
  basicTitleWrapClass: 'flex flex-col items-center gap-2 text-center',
  nameClass: 'text-3xl font-normal tracking-[0.25em] text-[#3775a6]',
  intentClass: 'text-sm text-gray-500',
  metaGridClass: 'grid grid-cols-2 gap-x-4 gap-y-2 text-sm',
  headingWrapClass: 'mb-4 mt-2 flex h-7 items-stretch bg-[#e9f2f9]',
  headingClass: 'flex items-center bg-[#3775a6] px-4 text-sm font-bold tracking-wide text-white'
}
</script>
