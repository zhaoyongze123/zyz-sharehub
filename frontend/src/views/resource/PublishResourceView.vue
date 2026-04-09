<template>
  <div class="page-shell form-grid">
    <section class="glass-panel form-panel">
      <div class="section-heading">
        <h1>发布资料</h1>
        <p>按实施文档要求覆盖信息录入、上传、分类标签、封面和提交审核。</p>
      </div>

      <div class="form-stack">
        <BaseInput v-model="form.title" label="资料标题" placeholder="例如：RAG 评测 checklist" />
        <BaseSelect v-model="form.category" label="资料分类" :options="categoryOptions" />
        <BaseInput v-model="form.tags" label="标签" placeholder="用逗号分隔多个标签" />
        <BaseTabs v-model="uploadMode" :items="uploadModeItems" />
        <BaseUploader v-if="uploadMode === 'file'" hint="上传资料文件、压缩包或封面图" />
        <BaseInput v-else v-model="form.url" label="外链地址" placeholder="https://..." />
        <BaseTextarea v-model="form.summary" label="资料简介" placeholder="说明适用人群、内容范围和使用方式" />
      </div>

      <div class="form-actions">
        <BaseButton variant="secondary" @click="saveDraft">保存草稿</BaseButton>
        <BaseButton @click="submitReview">提交审核</BaseButton>
      </div>
    </section>

    <aside class="side-stack">
      <BaseEmpty title="预览" :description="previewText" />
      <BaseEmpty title="状态说明" description="当标题为空时会给出校验提示；联调后这里接上传进度和提交结果。" />
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTabs from '@/components/base/BaseTabs.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import BaseUploader from '@/components/base/BaseUploader.vue'
import { resourceCategoryOptions } from '@/api/resources'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const uploadMode = ref('file')
const categoryOptions = computed(() =>
  Array.from(
    new Set(
      resourceCategoryOptions
        .map((item) => item?.trim())
        .filter((item): item is string => Boolean(item))
    )
  )
    .filter((item) => item !== '全部')
    .map((item) => ({ label: item, value: item }))
)
const defaultCategory = computed(() => categoryOptions.value[0]?.value || '')
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

const previewText = computed(() => form.title ? `${form.title} · ${form.category} · ${form.tags || '未填标签'}` : '填写标题后，这里会展示发布预览摘要。')

function saveDraft() {
  appStore.showToast('草稿已保存', form.title || '可以继续补完资料内容')
}

function submitReview() {
  if (!form.title.trim()) {
    appStore.showToast('校验失败', '资料标题不能为空', 'error')
    return
  }
  appStore.showToast('提交成功', '已进入待审核队列')
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

@media (max-width: 64rem) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
