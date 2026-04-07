<template>
  <div class="page-shell form-grid">
    <section class="glass-panel form-panel">
      <div class="section-heading">
        <h1>笔记编辑</h1>
        <p>覆盖 Markdown 编辑、实时预览、自动保存、目录提取与发布动作。</p>
      </div>

      <div class="form-stack">
        <BaseInput v-model="title" label="笔记标题" placeholder="例如：Agent 系统设计答题框架" />
        <BaseTabs v-model="mode" :items="modeItems" />
        <BaseTextarea v-if="mode === 'edit'" v-model="content" label="Markdown 内容" placeholder="# 标题" />
        <article v-else class="glass-panel preview-panel" v-html="previewHtml"></article>
      </div>

      <div class="form-actions">
        <BaseButton variant="secondary" @click="saveDraft">保存草稿</BaseButton>
        <BaseButton variant="secondary" @click="simulateConflict">模拟冲突</BaseButton>
        <BaseButton @click="publishNote">发布 / 更新</BaseButton>
      </div>
    </section>

    <aside class="side-stack">
      <NoteOutline :items="outline" />
      <BaseEmpty :title="saveState.title" :description="saveState.description" />
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseTabs from '@/components/base/BaseTabs.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import NoteOutline from '@/components/business/NoteOutline.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const title = ref('Agent 系统设计答题框架')
const content = ref('# 目标\n\n明确问题边界。\n\n## 架构\n\n规划计划、执行、复盘。\n\n## 风险\n\n控制时延和失败率。')
const mode = ref('edit')
const stateKey = ref<'idle' | 'saving' | 'conflict'>('idle')

const modeItems = [
  { label: '编辑', value: 'edit' },
  { label: '预览', value: 'preview' }
]

const previewHtml = computed(() =>
  content.value
    .split('\n')
    .map((line) => {
      if (line.startsWith('# ')) return `<h1>${line.slice(2)}</h1>`
      if (line.startsWith('## ')) return `<h2>${line.slice(3)}</h2>`
      if (!line.trim()) return '<br />'
      return `<p>${line}</p>`
    })
    .join('')
)

const outline = computed(() =>
  content.value
    .split('\n')
    .filter((line) => line.startsWith('#'))
    .map((line) => line.replace(/^#+\s*/, ''))
)

const saveState = computed(() => {
  if (stateKey.value === 'saving') {
    return { title: '保存中', description: '最近一次变更正在同步。' }
  }
  if (stateKey.value === 'conflict') {
    return { title: '检测到冲突', description: '云端版本已更新，请合并后再提交。' }
  }
  return { title: '自动保存正常', description: '当前内容已进入本地草稿状态。' }
})

function saveDraft() {
  stateKey.value = 'saving'
  appStore.showToast('草稿已保存', '可稍后继续编辑')
  window.setTimeout(() => {
    stateKey.value = 'idle'
  }, 800)
}

function simulateConflict() {
  stateKey.value = 'conflict'
  appStore.showToast('发现编辑冲突', '请先处理云端版本差异', 'error')
}

function publishNote() {
  if (!title.value.trim()) {
    appStore.showToast('校验失败', '笔记标题不能为空', 'error')
    return
  }
  stateKey.value = 'saving'
  appStore.showToast('发布成功', '笔记已进入公开列表')
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

.preview-panel {
  padding: var(--space-6);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-3);
}

@media (max-width: 64rem) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
