<template>
  <div class="page-shell form-grid">
    <section class="glass-panel form-panel">
      <div class="section-heading">
        <h1>发布资料</h1>
        <p>按实施文档要求覆盖信息录入、附件上传、外链引用和提交审核。</p>
      </div>

      <div class="form-stack">
        <BaseInput
          v-model="form.title"
          label="资料标题"
          placeholder="例如：RAG 评测 checklist"
          data-testid="publish-resource-title"
        />
        <BaseSelect v-model="form.category" label="资料分类" :options="categoryOptions" />
        <BaseInput
          v-model="form.tags"
          label="标签"
          placeholder="用逗号分隔多个标签"
          data-testid="publish-resource-tags"
        />
        <BaseTabs v-model="uploadMode" :items="uploadModeItems" />
        <BaseUploader
          v-if="uploadMode === 'file'"
          v-model:file="selectedFile"
          hint="上传资料文件、压缩包或封面图"
          input-testid="publish-resource-file"
          :disabled="isSubmitting"
        />
        <BaseInput
          v-else
          v-model="form.url"
          label="外链地址"
          placeholder="https://..."
          data-testid="publish-resource-url"
        />
        <BaseTextarea
          v-model="form.summary"
          label="资料简介"
          placeholder="说明适用人群、内容范围和使用方式"
          data-testid="publish-resource-summary"
        />
        <p v-if="validationMessage" class="form-feedback form-feedback--error" data-testid="publish-resource-error">
          {{ validationMessage }}
        </p>
      </div>

      <div class="form-actions">
        <BaseButton
          variant="secondary"
          :disabled="isSubmitting"
          data-testid="publish-resource-save"
          @click="saveDraft"
        >
          {{ isSubmitting ? '提交中...' : '保存草稿' }}
        </BaseButton>
        <BaseButton
          :disabled="isSubmitting"
          data-testid="publish-resource-submit"
          @click="submitReview"
        >
          {{ isSubmitting ? '提交中...' : '提交审核' }}
        </BaseButton>
      </div>
    </section>

    <aside class="side-stack">
      <BaseEmpty title="预览" :description="previewText" />
      <BaseEmpty title="状态说明" :description="statusDescription" />
      <div v-if="lastPublishedId" class="glass-panel publish-result" data-testid="publish-resource-result">
        <p class="publish-result__title">真实发布已完成</p>
        <p>资源 ID：{{ lastPublishedId }}</p>
        <p v-if="uploadedFileName">附件：{{ uploadedFileName }}</p>
        <RouterLink :to="`/resources/${lastPublishedId}`">查看详情页</RouterLink>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTabs from '@/components/base/BaseTabs.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import BaseUploader from '@/components/base/BaseUploader.vue'
import { createResource, publishResource, resourceCategoryOptions, uploadResourceAttachment } from '@/api/resources'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const uploadMode = ref('file')
const categoryOptions = resourceCategoryOptions
  .filter((item) => item !== '全部')
  .map((item) => ({ label: item, value: item }))
const defaultCategory = computed(() => categoryOptions[0]?.value || '')
const selectedFile = ref<File | null>(null)
const isSubmitting = ref(false)
const validationMessage = ref('')
const lastPublishedId = ref<number | null>(null)
const uploadedFileName = ref('')
const form = reactive({
  title: '',
  category: defaultCategory.value,
  tags: '',
  url: '',
  summary: ''
})

const uploadModeItems = [
  { label: '本地上传', value: 'file' },
  { label: '外链引用', value: 'link' }
]

const previewText = computed(() => {
  if (!form.title) {
    return '填写标题后，这里会展示发布预览摘要。'
  }

  const source = uploadMode.value === 'file'
    ? (selectedFile.value?.name || '待上传附件')
    : (form.url.trim() || '待填写外链')

  return `${form.title} · ${form.category} · ${form.tags || '未填标签'} · ${source}`
})

const statusDescription = computed(() => {
  if (lastPublishedId.value) {
    return '资料已通过真实接口创建、上传并提交发布，可继续进入详情页复核。'
  }
  if (uploadMode.value === 'file') {
    return selectedFile.value ? `待上传附件：${selectedFile.value.name}` : '当前为附件模式，提交时会先创建草稿再上传附件。'
  }
  return form.url.trim() ? `当前为外链模式：${form.url.trim()}` : '当前为外链模式，提交时会直接写入资源记录并发布。'
})

function normalizeTags() {
  return form.tags
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function validateForm() {
  if (!form.title.trim()) {
    return '资料标题不能为空'
  }
  if (!form.summary.trim()) {
    return '资料简介不能为空'
  }
  if (uploadMode.value === 'file' && !selectedFile.value) {
    return '请先选择要上传的资料附件'
  }
  if (uploadMode.value === 'link' && !form.url.trim()) {
    return '请填写外链地址'
  }
  return ''
}

function resetForm() {
  form.title = ''
  form.category = defaultCategory.value
  form.tags = ''
  form.url = ''
  form.summary = ''
  selectedFile.value = null
  validationMessage.value = ''
}

async function saveDraft() {
  validationMessage.value = ''
  const message = validateForm()
  if (message) {
    validationMessage.value = message
    appStore.showToast('校验失败', message, 'error')
    return
  }

  isSubmitting.value = true
  try {
    const created = await createResource({
      title: form.title.trim(),
      type: form.category,
      category: form.category,
      summary: form.summary.trim(),
      tags: normalizeTags(),
      visibility: 'PUBLIC',
      status: 'DRAFT',
      externalUrl: uploadMode.value === 'link' ? form.url.trim() : undefined
    })

    if (uploadMode.value === 'file' && selectedFile.value) {
      const attachment = await uploadResourceAttachment(created.id, selectedFile.value)
      uploadedFileName.value = attachment.file.filename
    } else {
      uploadedFileName.value = ''
    }

    lastPublishedId.value = created.id
    appStore.showToast('草稿已保存', `资源 #${created.id} 已通过真实接口创建`)
  } finally {
    isSubmitting.value = false
  }
}

async function submitReview() {
  validationMessage.value = ''
  const message = validateForm()
  if (message) {
    validationMessage.value = message
    appStore.showToast('校验失败', message, 'error')
    return
  }

  isSubmitting.value = true
  try {
    const created = await createResource({
      title: form.title.trim(),
      type: form.category,
      category: form.category,
      summary: form.summary.trim(),
      tags: normalizeTags(),
      visibility: 'PUBLIC',
      status: 'DRAFT',
      externalUrl: uploadMode.value === 'link' ? form.url.trim() : undefined
    })

    if (uploadMode.value === 'file' && selectedFile.value) {
      const attachment = await uploadResourceAttachment(created.id, selectedFile.value)
      uploadedFileName.value = attachment.file.filename
    } else {
      uploadedFileName.value = ''
    }

    const published = await publishResource(created.id)
    lastPublishedId.value = published.id
    appStore.showToast('提交成功', `资源 #${published.id} 已进入公开展示`)
    resetForm()
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-6);
}

.form-panel,
.side-stack {
  display: grid;
  gap: var(--space-5);
}

.form-panel {
  padding: var(--space-6);
}

.form-stack {
  display: grid;
  gap: var(--space-4);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.form-feedback {
  margin: 0;
  font-size: var(--font-size-sm);
}

.form-feedback--error {
  color: #c0392b;
}

.publish-result {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
}

.publish-result p {
  margin: 0;
}

.publish-result__title {
  font-weight: 600;
  color: var(--color-text-main);
}

@media (max-width: 64rem) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
