<template>
  <div class="page-shell form-grid">
    <section class="glass-panel form-panel">
      <div class="section-heading">
        <h1>简历工作台</h1>
        <p>选择模板、编辑基础信息与项目经历，并在右侧实时预览。</p>
      </div>

      <div class="template-grid">
        <ResumeTemplateCard v-for="item in templates" :key="item.value" :title="item.label" :description="item.description" :active="template === item.value" @click="template = item.value" />
      </div>

      <div class="form-stack">
        <BaseInput v-model="name" label="姓名" placeholder="Alex Chen" />
        <BaseInput v-model="target" label="目标方向" placeholder="Agent / RAG 平台工程" />
        <BaseTextarea v-model="projectSummary" label="项目亮点" placeholder="描述你的代表项目和结果指标" />
      </div>

      <div class="form-actions">
        <BaseButton variant="secondary" @click="previewResume">生成预览</BaseButton>
        <BaseButton @click="exportPdf">导出 PDF</BaseButton>
      </div>
    </section>

    <aside class="side-stack">
      <ResumePreviewPanel :profile="{ name, target }" :highlights="highlights" />
      <BaseEmpty :title="exportState.title" :description="exportState.description" />
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import ResumePreviewPanel from '@/components/business/ResumePreviewPanel.vue'
import ResumeTemplateCard from '@/components/business/ResumeTemplateCard.vue'
import { resumeHighlights } from '@/mock/user'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const template = ref('aurora')
const name = ref('Alex Chen')
const target = ref('Agent / RAG 平台工程')
const projectSummary = ref('主导构建多 Agent 工作流，成功率提升 17%，并推动 RAG 评测体系落地。')
const exporting = ref(false)

const templates = [
  { label: 'Aurora', value: 'aurora', description: '更适合强调技术深度和项目闭环。' },
  { label: 'Grid', value: 'grid', description: '更适合信息密度高的工程履历。' }
]

const highlights = computed(() => [projectSummary.value, ...resumeHighlights.slice(1)])
const exportState = computed(() =>
  exporting.value
    ? { title: '生成中', description: '正在整理模板和预览数据。' }
    : { title: '待导出', description: '确认信息无误后即可生成 PDF。' }
)

function previewResume() {
  appStore.showToast('预览已刷新', '右侧已同步最新内容')
}

function exportPdf() {
  exporting.value = true
  appStore.showToast('导出任务开始', '联调后这里会接真实 PDF 导出接口')
  window.setTimeout(() => {
    exporting.value = false
    appStore.showToast('导出完成', '当前为前端模拟成功态')
  }, 1200)
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

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
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
  .form-grid,
  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>
