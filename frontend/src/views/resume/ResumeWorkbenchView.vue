<template>
  <div class="flex h-full min-h-0 flex-col bg-slate-100">
    <header class="flex h-[68px] shrink-0 items-center justify-between border-b bg-white px-6 shadow-sm">
      <div class="flex items-center gap-3">
        <span class="text-sm text-slate-500">草稿</span>
        <select v-model="currentDraftId" class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm">
          <option v-for="draft in drafts" :key="draft.id" :value="draft.id">{{ draft.name }}</option>
        </select>
        <BaseButton variant="secondary" size="sm" @click="createNewDraft">新建草稿</BaseButton>
      </div>

      <div class="flex items-center gap-2">
        <span v-if="saving" class="text-xs text-slate-400">保存中...</span>
        <span v-else-if="saveTime" class="text-xs text-emerald-600">已自动保存 {{ saveTime }}</span>
      </div>

      <div class="flex items-center gap-2">
        <select v-model="currentTemplate" class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
          <option value="classic">经典基础</option>
          <option value="professional">沉稳商务</option>
          <option value="modern">现代左右</option>
        </select>
        <input ref="fileInputRef" type="file" accept="application/pdf" class="hidden" @change="handlePdfImport" />
        <BaseButton variant="secondary" size="sm" @click="triggerPdfImport">导入简历 PDF</BaseButton>
        <BaseButton variant="secondary" size="sm" :disabled="!currentDraft?.sourceText || parsing" @click="reparseCurrentDraft">
          {{ parsing ? '解析中...' : '重新解析' }}
        </BaseButton>
        <BaseButton variant="secondary" size="sm" @click="addSection">添加模块</BaseButton>
        <BaseButton variant="secondary" size="sm" @click="resetCurrentDraft">重置</BaseButton>
        <BaseButton size="sm" :disabled="exporting" @click="exportPdf">{{ exporting ? '导出中...' : '导出 PDF' }}</BaseButton>
      </div>
    </header>

    <main class="flex min-h-0 flex-1">
      <aside class="flex w-[280px] shrink-0 flex-col border-r bg-white">
        <div class="border-b px-4 py-3">
          <input
            v-if="currentDraft"
            v-model="currentDraft.name"
            class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold"
            placeholder="草稿名称"
          />
        </div>
        <div class="min-h-0 flex-1 overflow-y-auto p-4">
          <draggable v-model="currentSections" item-key="id" handle=".drag-handle" class="space-y-3">
            <template #item="{ element }">
              <div class="rounded-2xl border border-slate-200 bg-slate-50 p-3 shadow-sm">
                <div class="flex items-start gap-2">
                  <button type="button" class="drag-handle mt-2 cursor-grab rounded border border-slate-200 bg-white px-2 py-1 text-xs text-slate-400">≡</button>
                  <div class="min-w-0 flex-1">
                    <input
                      :id="getSectionFocusId(element.id)"
                      v-model="element.name"
                      class="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium"
                      placeholder="模块名称"
                    />
                    <div class="mt-2 flex items-center gap-2">
                      <select v-model="element.layout" class="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-2 py-2 text-xs">
                        <option value="fields">字段模块</option>
                        <option value="items">条目模块</option>
                        <option value="text">文本模块</option>
                      </select>
                      <button type="button" class="rounded-lg border px-2 py-2 text-xs" :class="element.visible ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-slate-100 text-slate-500'" @click="element.visible = !element.visible">
                        {{ element.visible ? '显示' : '隐藏' }}
                      </button>
                      <button type="button" class="rounded-lg border border-rose-200 bg-rose-50 px-2 py-2 text-xs text-rose-600" @click="removeSection(element.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </draggable>
        </div>
      </aside>

      <section class="flex w-[460px] shrink-0 flex-col border-r bg-white">
        <div class="border-b px-5 py-4">
          <h2 class="text-sm font-semibold text-slate-800">结构化编辑区</h2>
          <p class="mt-1 text-xs text-slate-500">支持模块名、字段名、字段值、条目描述和顺序调整。</p>
        </div>
        <div class="min-h-0 flex-1 overflow-y-auto p-5">
          <div v-if="currentDraft" class="space-y-5">
            <article v-for="section in currentDraft.document.sections" :key="section.id" class="rounded-3xl border border-slate-200 bg-slate-50 p-4 shadow-sm">
              <div class="mb-4 flex items-center justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <input
                    v-model="section.name"
                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold"
                    placeholder="模块名称"
                  />
                  <p class="mt-1 text-xs text-slate-500">类型：{{ section.layout }} / 标准模块：{{ section.kind }}</p>
                </div>
                <div class="flex items-center gap-2">
                  <BaseButton variant="secondary" size="sm" @click="focusSection(section.id)">定位</BaseButton>
                  <BaseButton variant="danger" size="sm" @click="removeSection(section.id)">删除</BaseButton>
                </div>
              </div>

              <div v-if="section.layout === 'fields'" class="space-y-3">
                <div v-for="field in section.fields" :key="field.id" class="grid gap-2 rounded-2xl border border-slate-200 bg-white p-3">
                  <div class="flex items-center gap-2">
                    <input
                      :id="getFieldFocusId(section.id, field.id)"
                      v-model="field.label"
                      class="min-w-0 flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium"
                      placeholder="字段名"
                    />
                    <input v-model="field.key" class="w-[120px] rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-500" placeholder="字段 key" />
                    <button type="button" class="rounded-lg border px-2 py-2 text-xs" :class="field.visible ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-slate-100 text-slate-500'" @click="field.visible = !field.visible">
                      {{ field.visible ? '显示' : '隐藏' }}
                    </button>
                    <button type="button" class="rounded-lg border border-rose-200 bg-rose-50 px-2 py-2 text-xs text-rose-600" @click="removeField(section.id, field.id)">删除</button>
                  </div>
                  <textarea
                    v-model="field.value"
                    class="min-h-[74px] w-full rounded-xl border border-slate-200 px-3 py-3 text-sm leading-relaxed"
                    placeholder="字段内容"
                  />
                </div>
                <BaseButton variant="secondary" size="sm" block @click="addField(section.id)">新增字段</BaseButton>
              </div>

              <div v-else-if="section.layout === 'items'" class="space-y-4">
                <div v-for="item in section.items" :key="item.id" :id="getItemFocusId(section.id, item.id)" class="rounded-2xl border border-slate-200 bg-white p-4">
                  <div class="mb-3 flex items-center justify-between gap-2">
                    <div class="text-sm font-semibold text-slate-700">条目</div>
                    <div class="flex items-center gap-2">
                      <button type="button" class="rounded-lg border px-2 py-2 text-xs" :class="item.visible ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-slate-100 text-slate-500'" @click="item.visible = !item.visible">
                        {{ item.visible ? '显示' : '隐藏' }}
                      </button>
                      <button type="button" class="rounded-lg border border-rose-200 bg-rose-50 px-2 py-2 text-xs text-rose-600" @click="removeItem(section.id, item.id)">删除条目</button>
                    </div>
                  </div>

                  <div class="space-y-3">
                    <div v-for="field in item.fields" :key="field.id" class="grid gap-2 rounded-2xl border border-slate-100 bg-slate-50 p-3">
                      <div class="flex items-center gap-2">
                        <input
                          :id="getFieldFocusId(section.id, field.id)"
                          v-model="field.label"
                          class="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium"
                          placeholder="字段名"
                        />
                        <input v-model="field.key" class="w-[120px] rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-500" placeholder="字段 key" />
                        <button type="button" class="rounded-lg border px-2 py-2 text-xs" :class="field.visible ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-slate-100 text-slate-500'" @click="field.visible = !field.visible">
                          {{ field.visible ? '显示' : '隐藏' }}
                        </button>
                        <button type="button" class="rounded-lg border border-rose-200 bg-rose-50 px-2 py-2 text-xs text-rose-600" @click="removeItemField(section.id, item.id, field.id)">删除</button>
                      </div>
                      <input v-model="field.value" class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" placeholder="字段值" />
                    </div>
                    <BaseButton variant="secondary" size="sm" @click="addItemField(section.id, item.id)">新增字段</BaseButton>
                  </div>

                  <div class="mt-4 space-y-3">
                    <div v-for="description in item.descriptions" :key="description.id" class="flex gap-2">
                      <textarea
                        :id="getDescriptionFocusId(section.id, description.id)"
                        v-model="description.text"
                        class="min-h-[68px] flex-1 rounded-xl border border-slate-200 px-3 py-3 text-sm leading-relaxed"
                        placeholder="描述内容"
                      />
                      <div class="flex flex-col gap-2">
                        <button type="button" class="rounded-lg border px-2 py-2 text-xs" :class="description.visible ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-slate-100 text-slate-500'" @click="description.visible = !description.visible">
                          {{ description.visible ? '显示' : '隐藏' }}
                        </button>
                        <button type="button" class="rounded-lg border border-rose-200 bg-rose-50 px-2 py-2 text-xs text-rose-600" @click="removeDescription(section.id, item.id, description.id)">删除</button>
                      </div>
                    </div>
                    <BaseButton variant="secondary" size="sm" @click="addDescription(section.id, item.id)">新增描述</BaseButton>
                  </div>
                </div>
                <BaseButton variant="secondary" size="sm" block @click="addItem(section.id)">新增条目</BaseButton>
              </div>

              <div v-else>
                <textarea
                  :id="getTextFocusId(section.id)"
                  v-model="section.text"
                  class="min-h-[160px] w-full rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm leading-relaxed"
                  placeholder="输入正文"
                />
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="min-h-0 flex-1 overflow-auto bg-[#d8dce2] p-8">
        <div class="sticky top-4 z-10 mb-6 inline-flex rounded-full bg-slate-900 px-4 py-2 text-xs font-medium text-white shadow-xl">
          红线代表 A4 分页线，导出时会自动去除
        </div>
        <div class="mx-auto w-[794px] min-w-[794px]">
          <component :is="templateComponents[currentTemplate]" ref="resumeTemplateRef" :document="currentDocument" @focus-field="handleFocusField" />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import html2pdf from 'html2pdf.js'
import BaseButton from '@/components/base/BaseButton.vue'
import ResumeClassicTemplate from '@/components/business/ResumeClassicTemplate.vue'
import ResumeProfessionalTemplate from '@/components/business/ResumeProfessionalTemplate.vue'
import ResumeModernTemplate from '@/components/business/ResumeModernTemplate.vue'
import { useAppStore } from '@/stores/app'
import {
  buildResumeDocumentFromLines,
  buildResumeDocumentFromText,
  createEmptyDocument,
  createId,
  extractPdfLines,
  type ResumeDocument,
  type ResumeField,
  type ResumeItem,
  type ResumeSection
} from '@/features/resume-parser'

type TemplateKey = ResumeDocument['templateKey']

interface StoredResumeDraft {
  id: string
  name: string
  templateKey: TemplateKey
  updatedAt: string
  sourceFileName?: string
  sourceText?: string
  document: ResumeDocument
}

const STORAGE_KEY = 'resume_documents_v2'
const LEGACY_STORAGE_KEY = 'resume_versions'

const appStore = useAppStore()
const resumeTemplateRef = ref<any>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const drafts = ref<StoredResumeDraft[]>([])
const currentDraftId = ref('')
const saving = ref(false)
const saveTime = ref('')
const exporting = ref(false)
const parsing = ref(false)

const templateComponents: Record<TemplateKey, any> = {
  classic: ResumeClassicTemplate,
  professional: ResumeProfessionalTemplate,
  modern: ResumeModernTemplate
}

const currentDraft = computed(() => drafts.value.find((draft) => draft.id === currentDraftId.value) ?? null)
const currentDocument = computed(() => currentDraft.value?.document ?? createEmptyDocument())

const currentTemplate = computed<TemplateKey>({
  get() {
    return currentDraft.value?.templateKey ?? 'classic'
  },
  set(value) {
    if (!currentDraft.value) {
      return
    }
    currentDraft.value.templateKey = value
    currentDraft.value.document.templateKey = value
  }
})

const currentSections = computed<ResumeSection[]>({
  get() {
    return currentDraft.value?.document.sections ?? []
  },
  set(value) {
    if (currentDraft.value) {
      currentDraft.value.document.sections = value
    }
  }
})

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function nowLabel() {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
}

function createDraft(templateKey: TemplateKey = 'classic', name = '默认简历'): StoredResumeDraft {
  const document = createEmptyDocument(templateKey)
  document.name = name
  return {
    id: createId('draft'),
    name,
    templateKey,
    updatedAt: new Date().toISOString(),
    document
  }
}

function createField(label = '新字段', value = ''): ResumeField {
  return {
    id: createId('field'),
    key: label,
    label,
    value,
    visible: true
  }
}

function createItem(): ResumeItem {
  return {
    id: createId('item'),
    visible: true,
    fields: [],
    descriptions: []
  }
}

function createSection(layout: ResumeSection['layout'] = 'text', name = '自定义模块'): ResumeSection {
  return {
    id: createId('section'),
    kind: 'custom',
    name,
    visible: true,
    layout,
    fields: layout === 'fields' ? [createField()] : [],
    items: layout === 'items' ? [createItem()] : [],
    text: ''
  }
}

function getSectionFocusId(sectionId: string) {
  return `resume-focus-section-${sectionId}`
}

function getFieldFocusId(sectionId: string, fieldId: string) {
  return `resume-focus-field-${sectionId}-${fieldId}`
}

function getItemFocusId(sectionId: string, itemId: string) {
  return `resume-focus-item-${sectionId}-${itemId}`
}

function getDescriptionFocusId(sectionId: string, descriptionId: string) {
  return `resume-focus-description-${sectionId}-${descriptionId}`
}

function getTextFocusId(sectionId: string) {
  return `resume-focus-text-${sectionId}`
}

function migrateLegacyData() {
  const raw = window.localStorage.getItem(LEGACY_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const legacy = JSON.parse(raw) as Array<{ id: string; name: string; data: any[]; template?: TemplateKey }>
    if (!Array.isArray(legacy) || !legacy.length) {
      return null
    }

    return legacy.map<StoredResumeDraft>((item) => {
      const document = createEmptyDocument(item.template ?? 'classic')
      document.name = item.name || '迁移简历'
      document.sections = item.data.map((module) => {
        if (module.type === 'basic') {
          return {
            id: module.id ?? createId('section'),
            kind: 'basic',
            name: module.title || '基本信息',
            visible: module.visible !== false,
            layout: 'fields',
            fields: Object.entries(module.data ?? {})
              .filter(([key]) => !key.endsWith('Visible') && key !== 'avatar')
              .map(([key, value]) => createField(String(key), String(value ?? ''))),
            items: [],
            text: ''
          } satisfies ResumeSection
        }
        if (module.type === 'education' || module.type === 'experience' || module.type === 'project') {
          return {
            id: module.id ?? createId('section'),
            kind: module.type,
            name: module.title || '迁移模块',
            visible: module.visible !== false,
            layout: 'items',
            fields: [],
            items: (module.data?.items ?? []).map((legacyItem: Record<string, any>) => ({
              id: createId('item'),
              visible: legacyItem.visible !== false,
              fields: Object.entries(legacyItem)
                .filter(([key, value]) => key !== 'visible' && key !== 'descriptions' && !key.endsWith('Visible') && typeof value === 'string')
                .map(([key, value]) => createField(String(key), String(value))),
              descriptions: Array.isArray(legacyItem.descriptions)
                ? legacyItem.descriptions.map((entry: any) => ({
                    id: createId('desc'),
                    text: typeof entry === 'string' ? entry : String(entry.text ?? ''),
                    visible: typeof entry === 'string' ? true : entry.visible !== false
                  }))
                : []
            })),
            text: ''
          } satisfies ResumeSection
        }
        if (module.type === 'skills') {
          const fields = Object.entries(module.data ?? {})
            .filter(([key, value]) => key !== 'bars' && !key.endsWith('Visible') && typeof value === 'string')
            .map(([key, value]) => createField(String(key), String(value)))

          const barFields = (module.data?.bars ?? []).map((bar: any) => createField(String(bar.name ?? '技能'), `${bar.level ?? ''} ${bar.percent ?? ''}%`.trim()))

          return {
            id: module.id ?? createId('section'),
            kind: 'skills',
            name: module.title || '技能特长',
            visible: module.visible !== false,
            layout: 'fields',
            fields: [...fields, ...barFields],
            items: [],
            text: ''
          } satisfies ResumeSection
        }
        if (module.type === 'textList') {
          return {
            id: module.id ?? createId('section'),
            kind: 'custom',
            name: module.title || '自定义模块',
            visible: module.visible !== false,
            layout: 'items',
            fields: [],
            items: (module.data?.items ?? []).map((entry: any) => ({
              id: createId('item'),
              visible: entry.visible !== false,
              fields: [],
              descriptions: [{ id: createId('desc'), text: String(entry.text ?? entry ?? ''), visible: true }]
            })),
            text: ''
          } satisfies ResumeSection
        }
        return {
          id: module.id ?? createId('section'),
          kind: 'custom',
          name: module.title || '自定义模块',
          visible: module.visible !== false,
          layout: 'text',
          fields: [],
          items: [],
          text: String(module.data?.content ?? '')
        } satisfies ResumeSection
      })

      return {
        id: item.id || createId('draft'),
        name: item.name || '迁移简历',
        templateKey: item.template ?? 'classic',
        updatedAt: new Date().toISOString(),
        document
      }
    })
  } catch {
    return null
  }
}

function persistDrafts() {
  saving.value = true
  drafts.value.forEach((draft) => {
    draft.updatedAt = new Date().toISOString()
    draft.document.templateKey = draft.templateKey
    draft.document.name = draft.name
  })
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(drafts.value))
  saveTime.value = nowLabel()
  window.setTimeout(() => {
    saving.value = false
  }, 300)
}

function loadDrafts() {
  const stored = window.localStorage.getItem(STORAGE_KEY)
  if (stored) {
    drafts.value = JSON.parse(stored)
    currentDraftId.value = drafts.value[0]?.id ?? ''
    return
  }

  const migrated = migrateLegacyData()
  if (migrated?.length) {
    drafts.value = migrated
    currentDraftId.value = migrated[0].id
    persistDrafts()
    appStore.showToast('迁移完成', '已将旧版本地简历迁移到新工作区。', 'success')
    return
  }

  const initial = createDraft()
  drafts.value = [initial]
  currentDraftId.value = initial.id
  persistDrafts()
}

function createNewDraft() {
  const base = currentDraft.value ? deepClone(currentDraft.value) : createDraft()
  const nextDraft: StoredResumeDraft = {
    ...base,
    id: createId('draft'),
    name: `新建草稿 ${drafts.value.length + 1}`,
    updatedAt: new Date().toISOString(),
    document: {
      ...deepClone(base.document),
      id: createId('resume'),
      name: `新建草稿 ${drafts.value.length + 1}`
    }
  }
  drafts.value.push(nextDraft)
  currentDraftId.value = nextDraft.id
}

function resetCurrentDraft() {
  if (!currentDraft.value) {
    return
  }
  const nextDocument = createEmptyDocument(currentDraft.value.templateKey)
  nextDocument.name = currentDraft.value.name
  currentDraft.value.document = nextDocument
  currentDraft.value.sourceFileName = undefined
  currentDraft.value.sourceText = undefined
  appStore.showToast('已重置', '当前草稿已恢复为空白结构。', 'info')
}

function addSection() {
  currentDraft.value?.document.sections.push(createSection())
}

function removeSection(sectionId: string) {
  if (!currentDraft.value) {
    return
  }
  currentDraft.value.document.sections = currentDraft.value.document.sections.filter((section) => section.id !== sectionId)
}

function addField(sectionId: string) {
  const section = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)
  section?.fields.push(createField())
}

function removeField(sectionId: string, fieldId: string) {
  const section = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)
  if (section) {
    section.fields = section.fields.filter((field) => field.id !== fieldId)
  }
}

function addItem(sectionId: string) {
  const section = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)
  section?.items.push(createItem())
}

function removeItem(sectionId: string, itemId: string) {
  const section = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)
  if (section) {
    section.items = section.items.filter((item) => item.id !== itemId)
  }
}

function addItemField(sectionId: string, itemId: string) {
  const item = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)?.items.find((entry) => entry.id === itemId)
  item?.fields.push(createField())
}

function removeItemField(sectionId: string, itemId: string, fieldId: string) {
  const item = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)?.items.find((entry) => entry.id === itemId)
  if (item) {
    item.fields = item.fields.filter((field) => field.id !== fieldId)
  }
}

function addDescription(sectionId: string, itemId: string) {
  const item = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)?.items.find((entry) => entry.id === itemId)
  item?.descriptions.push({
    id: createId('desc'),
    text: '',
    visible: true
  })
}

function removeDescription(sectionId: string, itemId: string, descriptionId: string) {
  const item = currentDraft.value?.document.sections.find((entry) => entry.id === sectionId)?.items.find((entry) => entry.id === itemId)
  if (item) {
    item.descriptions = item.descriptions.filter((entry) => entry.id !== descriptionId)
  }
}

function focusSection(sectionId: string) {
  const element = document.getElementById(getSectionFocusId(sectionId))
  element?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  element?.focus()
}

function handleFocusField(sectionId: string, targetId: string) {
  nextTick(() => {
    const candidates = [
      targetId === 'text' ? getTextFocusId(sectionId) : '',
      targetId ? getFieldFocusId(sectionId, targetId) : '',
      targetId ? getDescriptionFocusId(sectionId, targetId) : '',
      targetId ? getItemFocusId(sectionId, targetId) : '',
      getSectionFocusId(sectionId)
    ].filter(Boolean)

    for (const id of candidates) {
      const element = document.getElementById(id)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' })
        if ('focus' in element) {
          ;(element as HTMLElement).focus()
        }
        return
      }
    }
  })
}

function triggerPdfImport() {
  fileInputRef.value?.click()
}

async function handlePdfImport(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !currentDraft.value) {
    return
  }

  parsing.value = true

  try {
    const lines = await extractPdfLines(file)
    const result = buildResumeDocumentFromLines(lines, currentTemplate.value, file.name)
    currentDraft.value.document = result.document
    currentDraft.value.name = result.document.name
    currentDraft.value.sourceFileName = file.name
    currentDraft.value.sourceText = result.sourceText
    result.warnings.forEach((warning) => appStore.showToast('解析提示', warning, 'info'))
    appStore.showToast('导入完成', '简历 PDF 已解析到本地工作区。', 'success')
  } catch (error: any) {
    appStore.showToast('导入失败', error?.message || '无法解析该 PDF，请确认文件不是扫描件或加密文件。', 'error')
  } finally {
    parsing.value = false
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
}

function reparseCurrentDraft() {
  if (!currentDraft.value?.sourceText) {
    appStore.showToast('无法重解析', '当前草稿没有可用的原始文本。', 'error')
    return
  }

  parsing.value = true
  try {
    const result = buildResumeDocumentFromText(currentDraft.value.sourceText, currentTemplate.value, currentDraft.value.sourceFileName)
    currentDraft.value.document = result.document
    currentDraft.value.name = result.document.name
    result.warnings.forEach((warning) => appStore.showToast('解析提示', warning, 'info'))
    appStore.showToast('重新解析完成', '已基于原始文本重建结构化模块。', 'success')
  } finally {
    parsing.value = false
  }
}

async function exportPdf() {
  if (!resumeTemplateRef.value?.resumeRef) {
    return
  }

  exporting.value = true
  const element = resumeTemplateRef.value.resumeRef as HTMLElement
  element.classList.add('exporting-pdf')

  try {
    await html2pdf()
      .set({
        margin: 0,
        filename: `${currentDraft.value?.name || '简历'}-${Date.now()}.pdf`,
        image: { type: 'jpeg', quality: 1.0 },
        html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff' },
        jsPDF: { unit: 'px', format: [794, 1123], orientation: 'portrait', hotfixes: ['px_scaling'] }
      })
      .from(element)
      .save()

    appStore.showToast('导出完成', '本地 PDF 已生成。', 'success')
  } catch (error: any) {
    appStore.showToast('导出失败', error?.message || 'PDF 导出失败。', 'error')
  } finally {
    element.classList.remove('exporting-pdf')
    exporting.value = false
  }
}

watch(
  drafts,
  () => {
    if (!drafts.value.length) {
      return
    }
    persistDrafts()
  },
  { deep: true }
)

watch(
  currentDraft,
  (draft) => {
    if (draft) {
      draft.document.templateKey = draft.templateKey
      draft.document.name = draft.name
    }
  },
  { deep: true }
)

onMounted(() => {
  loadDrafts()
})
</script>
