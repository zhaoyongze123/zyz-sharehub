<template>
  <div class="resume-physical-page bg-white mx-auto shadow-sm" ref="resumeRef">
    <div class="resume-wrapper" :class="theme.wrapperClass">
      <section
        v-for="section in visibleSections"
        :key="section.id"
        class="resume-module-block"
        :class="[theme.sectionClass, section.kind === 'basic' ? theme.basicSectionClass : '']"
        @click.stop="$emit('focus-field', section.id, '')"
      >
        <template v-if="section.kind === 'basic'">
          <div :class="theme.basicHeaderClass">
            <div class="flex-1 min-w-0">
              <div :class="theme.basicTitleWrapClass">
                <h1 :class="theme.nameClass">{{ basicPrimary.name?.value || document.name }}</h1>
                <div v-if="basicPrimary.intent" :class="theme.intentClass">{{ basicPrimary.intent.value }}</div>
              </div>
              <div :class="theme.metaGridClass">
                <button
                  v-for="field in basicMetaFields"
                  :key="field.id"
                  type="button"
                  class="resume-inline-field"
                  @click.stop="$emit('focus-field', section.id, field.id)"
                >
                  <span class="resume-inline-label">{{ field.label }}</span>
                  <span>{{ field.value }}</span>
                </button>
              </div>
              <div v-if="basicExtraFields.length" class="mt-4 flex flex-wrap gap-2">
                <button
                  v-for="field in basicExtraFields"
                  :key="field.id"
                  type="button"
                  class="resume-chip-field"
                  @click.stop="$emit('focus-field', section.id, field.id)"
                >
                  <span class="font-semibold">{{ field.label }}</span>
                  <span>{{ field.value }}</span>
                </button>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div :class="theme.headingWrapClass">
            <h2 :class="theme.headingClass">{{ section.name }}</h2>
          </div>

          <div v-if="section.layout === 'fields'" class="grid gap-3 sm:grid-cols-2">
            <button
              v-for="field in visibleFields(section.fields)"
              :key="field.id"
              type="button"
              class="resume-field-card"
              @click.stop="$emit('focus-field', section.id, field.id)"
            >
              <div class="resume-field-label">{{ field.label }}</div>
              <div class="resume-field-value">{{ field.value }}</div>
            </button>
            <div v-if="section.text" class="resume-text-block sm:col-span-2">{{ section.text }}</div>
          </div>

          <div v-else-if="section.layout === 'items'" class="space-y-4">
            <article
              v-for="item in visibleItems(section)"
              :key="item.id"
              class="resume-item-card"
              @click.stop="$emit('focus-field', section.id, item.id)"
            >
              <div v-if="item.fields.length" class="flex flex-wrap items-baseline justify-between gap-3">
                <div class="flex flex-wrap items-baseline gap-3">
                  <button
                    v-for="field in item.fields.slice(0, 2)"
                    :key="field.id"
                    type="button"
                    class="resume-item-primary"
                    @click.stop="$emit('focus-field', section.id, field.id)"
                  >
                    {{ field.value }}
                  </button>
                </div>
                <button
                  v-for="field in item.fields.slice(2)"
                  :key="field.id"
                  type="button"
                  class="resume-item-secondary"
                  @click.stop="$emit('focus-field', section.id, field.id)"
                >
                  <span class="font-semibold">{{ field.label }}</span>
                  <span>{{ field.value }}</span>
                </button>
              </div>
              <ul v-if="item.descriptions.length" class="mt-2 space-y-1">
                <li
                  v-for="description in item.descriptions.filter((entry) => entry.visible)"
                  :key="description.id"
                  class="resume-description-item"
                  @click.stop="$emit('focus-field', section.id, description.id)"
                >
                  {{ description.text }}
                </li>
              </ul>
            </article>
          </div>

          <div v-else class="resume-text-block" @click.stop="$emit('focus-field', section.id, 'text')">
            {{ section.text || '请输入内容' }}
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ResumeDocument, ResumeField, ResumeItem, ResumeSection } from '@/features/resume-parser'

interface ThemeConfig {
  wrapperClass: string
  sectionClass: string
  basicSectionClass: string
  basicHeaderClass: string
  basicTitleWrapClass: string
  nameClass: string
  intentClass: string
  metaGridClass: string
  headingWrapClass: string
  headingClass: string
}

const props = defineProps<{
  document: ResumeDocument
  theme: ThemeConfig
}>()

defineEmits<{
  (event: 'focus-field', sectionId: string, targetId: string): void
}>()

const resumeRef = ref<HTMLElement | null>(null)
defineExpose({ resumeRef })

const visibleSections = computed(() => props.document.sections.filter((section) => section.visible))
const basicSection = computed(() => visibleSections.value.find((section) => section.kind === 'basic') ?? null)

const basicPrimary = computed(() => {
  const fields = basicSection.value?.fields ?? []
  const byKey = (key: string) => fields.find((field) => field.visible && field.key === key)
  return {
    name: byKey('name') ?? fields.find((field) => field.visible && /姓名|名字/.test(field.label)),
    intent: byKey('intent') ?? fields.find((field) => field.visible && /意向|岗位/.test(field.label))
  }
})

const basicMetaKeys = ['phone', 'email', 'city', 'currentCity', 'experience', 'education']

const basicMetaFields = computed(() => {
  const fields = basicSection.value?.fields ?? []
  return fields.filter((field) => field.visible && basicMetaKeys.includes(field.key))
})

const basicExtraFields = computed(() => {
  const fields = basicSection.value?.fields ?? []
  const hiddenIds = new Set([basicPrimary.value.name?.id, basicPrimary.value.intent?.id, ...basicMetaFields.value.map((field) => field.id)].filter(Boolean))
  return fields.filter((field) => field.visible && !hiddenIds.has(field.id))
})

function visibleFields(fields: ResumeField[]) {
  return fields.filter((field) => field.visible)
}

function visibleItems(section: ResumeSection): ResumeItem[] {
  return section.items.filter((item) => item.visible)
}
</script>

<style scoped lang="scss">
.resume-physical-page {
  width: 794px;
  min-height: 1123px;
  background-color: white;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent,
    transparent 1122px,
    #ef4444 1122px,
    #ef4444 1123px
  );
}

.resume-wrapper {
  padding: 40px 48px;
}

.resume-module-block {
  margin-bottom: 20px;
  cursor: pointer;
  transition: outline-color 0.2s ease, background-color 0.2s ease;
}

.resume-module-block:hover {
  outline: 2px dashed rgba(59, 130, 246, 0.4);
  outline-offset: 4px;
}

.resume-inline-field,
.resume-chip-field,
.resume-field-card,
.resume-item-primary,
.resume-item-secondary {
  border: none;
  background: transparent;
  text-align: left;
  padding: 0;
  cursor: pointer;
}

.resume-inline-field {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  color: #475569;
}

.resume-inline-label,
.resume-field-label {
  font-weight: 700;
  color: #334155;
}

.resume-chip-field {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.85);
}

.resume-field-card {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.95);
}

.resume-field-value {
  margin-top: 4px;
  color: #0f172a;
  white-space: pre-wrap;
  line-height: 1.5;
}

.resume-item-card {
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.35);
}

.resume-item-primary {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.resume-item-secondary {
  display: inline-flex;
  gap: 6px;
  font-size: 13px;
  color: #475569;
}

.resume-description-item {
  position: relative;
  padding-left: 12px;
  color: #334155;
  line-height: 1.6;
}

.resume-description-item::before {
  content: '•';
  position: absolute;
  left: 0;
}

.resume-text-block {
  white-space: pre-wrap;
  color: #334155;
  line-height: 1.7;
}

.exporting-pdf {
  min-height: auto;
  background-image: none !important;
}

.exporting-pdf .resume-module-block:hover {
  outline: none !important;
  background-color: transparent !important;
}
</style>
