<template>
  <div class="page-shell form-grid">
    <section class="glass-panel form-panel">
      <div class="section-heading">
        <h1>创建路线</h1>
        <p>按实施文档要求覆盖路线创建、节点追加、草稿保存与公开发布。</p>
      </div>

      <div class="form-stack">
        <BaseInput
          v-model="title"
          label="路线标题"
          placeholder="例如：Agent 工程师 30 天路线"
          data-testid="publish-roadmap-title"
        />
        <BaseTextarea
          v-model="summary"
          label="路线简介"
          placeholder="说明学习对象、阶段目标和完成结果"
          data-testid="publish-roadmap-summary"
        />

        <div class="node-stack">
          <div v-for="(node, index) in nodes" :key="node.id" class="glass-panel node-card">
            <BaseInput
              v-model="node.title"
              :label="`节点 ${index + 1}`"
              placeholder="阶段标题"
              :data-testid="`publish-roadmap-node-title-${index}`"
            />
          </div>
        </div>

        <p v-if="validationMessage" class="form-feedback form-feedback--error" data-testid="publish-roadmap-error">
          {{ validationMessage }}
        </p>

        <div class="form-actions">
          <BaseButton
            variant="secondary"
            :disabled="isSubmitting"
            data-testid="publish-roadmap-add-node"
            @click="addNode"
          >
            新增节点
          </BaseButton>
          <BaseButton
            variant="secondary"
            :disabled="isSubmitting"
            data-testid="publish-roadmap-save"
            @click="saveDraft"
          >
            {{ isSubmitting ? '提交中...' : '保存草稿' }}
          </BaseButton>
          <BaseButton
            :disabled="isSubmitting"
            data-testid="publish-roadmap-submit"
            @click="publishRoadmap"
          >
            {{ isSubmitting ? '提交中...' : '发布路线' }}
          </BaseButton>
        </div>
      </div>
    </section>

    <aside class="side-stack">
      <BaseEmpty title="关联资料" description="当前批次先收口路线创建与节点顺序写入，资料绑定后续再补。" />
      <BaseEmpty title="发布校验" :description="statusDescription" />
      <div v-if="lastCreatedRoadmapId" class="glass-panel publish-result" data-testid="publish-roadmap-result">
        <p class="publish-result__title">真实路线写入已完成</p>
        <p>路线 ID：{{ lastCreatedRoadmapId }}</p>
        <p>节点数：{{ lastCreatedNodeCount }}</p>
        <RouterLink :to="`/roadmaps/${lastCreatedRoadmapId}`">查看详情页</RouterLink>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios'
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import { addRoadmapNode, createRoadmap } from '@/api/roadmaps'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const title = ref('')
const summary = ref('')
const isSubmitting = ref(false)
const validationMessage = ref('')
const lastCreatedRoadmapId = ref<number | null>(null)
const lastCreatedNodeCount = ref(0)
const nodes = reactive([
  { id: 1, title: '阶段 1：协议与接入' },
  { id: 2, title: '阶段 2：工作流编排' }
])

const statusDescription = computed(() => {
  if (lastCreatedRoadmapId.value) {
    return `已通过真实接口创建路线 #${lastCreatedRoadmapId.value}，并写入 ${lastCreatedNodeCount.value} 个节点。`
  }
  return `${nodes.length} 个节点待提交；当前真实接口仅写入节点标题和顺序。`
})

function addNode() {
  nodes.push({ id: Date.now(), title: '' })
}

function validateForm() {
  if (!title.value.trim()) {
    return '路线标题不能为空'
  }
  if (!summary.value.trim()) {
    return '路线简介不能为空'
  }
  const hasBlankNode = nodes.some((node) => !node.title.trim())
  if (hasBlankNode) {
    return '每个节点都需要填写阶段标题'
  }
  return ''
}

function resetForm() {
  title.value = ''
  summary.value = ''
  validationMessage.value = ''
  nodes.splice(0, nodes.length, { id: Date.now(), title: '阶段 1：协议与接入' })
}

function resolveSubmitError(error: unknown, fallbackMessage: string) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.msg || error.response?.data?.message || fallbackMessage
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return fallbackMessage
}

async function submitRoadmap(status: 'DRAFT' | 'PUBLISHED') {
  validationMessage.value = ''
  const message = validateForm()
  if (message) {
    validationMessage.value = message
    appStore.showToast('校验失败', message, 'error')
    return
  }

  isSubmitting.value = true
  try {
    const created = await createRoadmap({
      title: title.value.trim(),
      description: summary.value.trim(),
      visibility: 'PUBLIC',
      status
    })

    for (const [index, node] of nodes.entries()) {
      await addRoadmapNode(created.id, {
        title: node.title.trim(),
        orderNo: index + 1
      })
    }

    lastCreatedRoadmapId.value = created.id
    lastCreatedNodeCount.value = nodes.length
    appStore.showToast(status === 'DRAFT' ? '草稿已保存' : '发布成功', `路线 #${created.id} 已完成真实写入`)
    resetForm()
  } catch (error) {
    validationMessage.value = resolveSubmitError(
      error,
      status === 'DRAFT' ? '路线草稿保存失败，请稍后重试' : '路线发布失败，请稍后重试'
    )
  } finally {
    isSubmitting.value = false
  }
}

async function saveDraft() {
  await submitRoadmap('DRAFT')
}

async function publishRoadmap() {
  await submitRoadmap('PUBLISHED')
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

.form-feedback {
  margin: 0;
  font-size: var(--font-size-sm);
}

.form-feedback--error {
  color: var(--color-danger);
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

.publish-result {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-4);
}

.publish-result__title {
  margin: 0;
  font-weight: 600;
}

@media (max-width: 64rem) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
