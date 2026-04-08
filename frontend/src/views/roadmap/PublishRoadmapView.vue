<template>
  <div class="page-shell form-grid">
    <section class="glass-panel form-panel">
      <div class="section-heading">
        <h1>创建路线</h1>
        <p>覆盖标题、简介、节点增删改排序、关联资料、草稿保存与发布。</p>
      </div>

      <div class="form-stack">
        <BaseInput v-model="title" label="路线标题" placeholder="例如：Agent 工程师 30 天路线" />
        <BaseTextarea v-model="summary" label="路线简介" placeholder="说明学习对象、阶段目标和完成结果" />

        <div class="node-stack">
          <div v-for="(node, index) in nodes" :key="node.id" class="glass-panel node-card">
            <BaseInput v-model="node.title" :label="`节点 ${index + 1}`" placeholder="阶段标题" />
            <BaseTextarea v-model="node.summary" label="节点说明" placeholder="阶段任务和里程碑" />
          </div>
        </div>

        <div class="form-actions">
          <BaseButton variant="secondary" @click="addNode" :disabled="submitting">新增节点</BaseButton>
          <BaseButton variant="secondary" @click="saveDraft" :disabled="submitting">保存草稿</BaseButton>
          <BaseButton @click="publishRoadmap" :disabled="submitting">发布路线</BaseButton>
        </div>
      </div>
    </section>

    <aside class="side-stack">
      <BaseEmpty title="关联资料" description="联调后这里接资源搜索与多选绑定。" />
      <BaseEmpty title="发布校验" :description="`${nodes.length} 个节点待发布`" />
    </aside>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import { addRoadmapNode, createRoadmap } from '@/api/roadmaps'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const router = useRouter()
const title = ref('')
const summary = ref('')
const submitting = ref(false)
const nodes = reactive([
  { id: 1, title: '阶段 1：协议与接入', summary: '' },
  { id: 2, title: '阶段 2：工作流编排', summary: '' }
])

function addNode() {
  nodes.push({ id: Date.now(), title: '', summary: '' })
}

function saveDraft() {
  appStore.showToast('草稿已保存', '路线节点和简介都已临时保留')
}

async function publishRoadmap() {
  if (!title.value.trim()) {
    appStore.showToast('校验失败', '路线标题不能为空', 'error')
    return
  }

  submitting.value = true
  try {
    const roadmap = await createRoadmap({
      title: title.value,
      summary: summary.value
    })
    await Promise.all(
      nodes.map((node, index) =>
        addRoadmapNode(roadmap.id, {
          title: node.title || `节点 ${index + 1}`,
          orderNo: index + 1,
          parentId: null,
          description: node.summary || ''
        })
      )
    )
    appStore.showToast('发布成功', '路线已进入公开展示流程')
    title.value = ''
    summary.value = ''
    nodes.splice(0, nodes.length, { id: 1, title: '阶段 1：协议与接入', summary: '' })
    router.push({ name: 'roadmap-detail', params: { id: roadmap.id } })
  } catch (error: any) {
    appStore.showToast('发布失败', error?.message ?? '请稍后再试', 'error')
  } finally {
    submitting.value = false
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

.form-stack,
.node-stack {
  display: grid;
  gap: var(--space-4);
}

.node-card {
  padding: var(--space-4);
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
