<template>
  <div class="page-shell detail-grid" v-if="status === 'ready' && roadmap">
    <section class="detail-main">
      <HeroBanner kicker="路线详情" :title="roadmap.title" :description="roadmap.summary">
        <template #actions>
          <BaseButton variant="secondary" @click="openStudyView()">Markdown 学习视图</BaseButton>
          <BaseButton @click="markComplete">标记本阶段完成</BaseButton>
          <BaseButton variant="secondary" @click="favoriteRoadmap">收藏路线</BaseButton>
        </template>
      </HeroBanner>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>节点进度结构</h2>
          <p>点击每个阶段节点，即可查看具体的内容、学习资源与讨论。</p>
        </div>
        <RoadmapTimeline :items="roadmap.timeline" @node-click="openNodeDetail" />
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>关联资料</h2>
          <p>路线中的关键节点可以直接跳到相关资料或笔记。</p>
        </div>
        <div v-if="roadmap.relatedResources.length" class="related-grid">
          <ResourceCard v-for="item in roadmap.relatedResources.slice(0, 2)" :key="item.id" :item="item" />
        </div>
        <BaseEmpty v-else title="暂无关联资料" description="当前路线节点还没有绑定公开资料。" />
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>Markdown 预览</h2>
          <p>路线详情已整理成适合连续阅读的 Markdown 预览格式。</p>
        </div>
        <article class="markdown-preview" v-html="renderedRoadmapMarkdown"></article>
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>路线状态</h2>
          <p>优先展示真实路线状态与节点数量，评论互动后续再接入。</p>
        </div>
        <BaseEmpty
          :title="`当前状态 ${roadmap.status}`"
          :description="`公开范围 ${roadmap.visibility}，共 ${roadmap.timeline.length} 个节点。`"
        />
      </div>
    </section>

    <aside class="detail-side">
      <section class="progress-panel glass-panel">
        <div class="progress-panel__header">
          <p class="progress-panel__eyebrow">阶段进度</p>
          <span class="progress-panel__badge">{{ progressStatusLabel }}</span>
        </div>

        <div class="progress-panel__meter">
          <div
            class="progress-panel__ring"
            :style="{ '--progress': `${progress}%` }"
            :aria-label="`当前真实进度 ${progress}%`"
          >
            <div class="progress-panel__ring-core">
              <strong>{{ progress }}%</strong>
              <span>已完成</span>
            </div>
          </div>
        </div>

        <div class="progress-panel__summary">
          <strong>{{ progressHeadline }}</strong>
          <p>{{ progressDescription }}</p>
        </div>

        <dl class="progress-panel__stats">
          <div>
            <dt>已点亮阶段</dt>
            <dd>{{ completedStageCount }}</dd>
          </div>
          <div>
            <dt>总阶段数</dt>
            <dd>{{ roadmap.timeline.length }}</dd>
          </div>
        </dl>
      </section>
    </aside>

    <!-- Node Detail Modal -->
    <div class="modal-overlay" v-if="selectedNode" @click.self="closeNodeDetail">
      <div class="node-modal glass-panel">
        <div class="modal-header">
          <h3>{{ selectedNode.title }} 详情</h3>
          <button class="close-btn" @click="closeNodeDetail">
            <div class="i-carbon-close"></div>
          </button>
        </div>
        <div class="modal-body">
          <div class="node-summary">
            <strong>阶段目标：</strong>
            <p>{{ selectedNode.description || selectedNode.summary }}</p>
          </div>
          
          <div class="node-tasks">
            <strong>核心任务：</strong>
            <ul>
              <li v-for="task in selectedNode.tasks" :key="task">
                <div class="i-carbon-checkmark-outline task-icon"></div>
                <span>{{ task }}</span>
              </li>
            </ul>
          </div>
          
          <div class="node-resources" v-if="selectedNodeIndex !== null">
            <strong>相关学习资料：</strong>
            <div class="resource-preview" v-if="selectedResource">
              <div class="i-carbon-document resource-icon"></div>
              <div class="resource-info">
                <span class="resource-title">{{ selectedResource.title }}</span>
                <span class="resource-meta">{{ selectedResource.fileType }} · {{ selectedResource.downloadCount }} 下载</span>
              </div>
              <button class="view-btn" @click="goToResource(selectedResource.id)">查看</button>
            </div>
            <p v-else class="text-muted text-sm">当前节点未绑定公开资料</p>
          </div>

          <div class="node-attachments" v-if="selectedNode.attachments.length">
            <strong>节点附件：</strong>
            <ul class="attachment-list">
              <li v-for="attachment in selectedNode.attachments" :key="attachment.id">
                <a :href="attachment.downloadUrl" target="_blank" rel="noreferrer">{{ attachment.filename }}</a>
              </li>
            </ul>
          </div>
        </div>
        <div class="modal-footer">
          <BaseButton variant="secondary" @click="closeNodeDetail">关闭</BaseButton>
          <BaseButton @click="startLearningNode">开始学习本阶段</BaseButton>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="status === 'error'" class="page-shell">
    <BaseErrorState title="路线不存在" description="请返回路线广场重新选择。" />
  </div>
  <div v-else class="page-shell">
    <BaseSkeleton height="24rem" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import RoadmapTimeline from '@/components/business/RoadmapTimeline.vue'
import {
  calculateRoadmapProgressPercent,
  fetchRoadmapDetail,
  getVisitedRoadmapNodeIds,
  markRoadmapNodeVisited,
  type RoadmapDetail,
  type RoadmapTimelineItem
} from '@/api/roadmaps'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const roadmap = ref<RoadmapDetail | null>(null)
const progress = ref(0)
const loading = ref(true)
const loadError = ref(false)
const status = computed(() => {
  if (loading.value) return 'loading'
  if (loadError.value || !roadmap.value) return 'error'
  return 'ready'
})

const selectedNode = ref<RoadmapTimelineItem | null>(null)
const selectedNodeIndex = ref<number | null>(null)
const selectedResource = computed(() => {
  if (selectedNodeIndex.value === null) return null
  return roadmap.value?.relatedResources.find((item) => item.id === selectedNode.value?.resourceId) ?? null
})
const completedStageCount = computed(() => {
  if (!roadmap.value?.timeline.length) return 0
  return Math.round((progress.value / 100) * roadmap.value.timeline.length)
})
const progressStatusLabel = computed(() => {
  if (progress.value >= 100) return '已完成'
  if (progress.value >= 60) return '推进中'
  if (progress.value > 0) return '已开始'
  return '未开始'
})
const progressHeadline = computed(() => {
  if (progress.value >= 100) return '当前路线已经全部点亮'
  if (progress.value >= 60) return '学习进度已经进入后半程'
  if (progress.value > 0) return '继续点击后续节点推进学习'
  return '先从第一个节点开始学习'
})
const progressDescription = computed(() => {
  if (!roadmap.value) return ''
  return `进度基于当前浏览器中已点击的路线节点实时计算，本路线共 ${roadmap.value.timeline.length} 个阶段。`
})
const roadmapMarkdown = computed(() => {
  if (!roadmap.value) return ''
  const lines = [
    `# ${roadmap.value.title}`,
    '',
    `> ${roadmap.value.summary}`,
    '',
    `- 状态：${roadmap.value.status}`,
    `- 可见性：${roadmap.value.visibility}`,
    `- 总阶段数：${roadmap.value.timeline.length}`,
    `- 当前学习进度：${progress.value}%`,
    ''
  ]

  roadmap.value.timeline.forEach((node, index) => {
    lines.push(`## ${index + 1}. ${node.title}`)
    if (node.description) {
      lines.push(node.description)
    }
    if (node.tasks.length) {
      node.tasks.forEach((task) => lines.push(`- ${task}`))
    }
    if (node.attachments.length) {
      node.attachments.forEach((attachment) => lines.push(`- 附件：${attachment.filename}`))
    }
    lines.push('')
  })

  return lines.join('\n')
})
const renderedRoadmapMarkdown = computed(() => renderMarkdown(roadmapMarkdown.value))

async function loadRoadmapDetail() {
  loading.value = true
  loadError.value = false

  try {
    const detail = await fetchRoadmapDetail(String(route.params.id))
    roadmap.value = detail
    syncProgress()
  } catch {
    roadmap.value = null
    progress.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function markComplete() {
  if (!selectedNode.value) {
    appStore.showToast('请先选择阶段', '点击时间线节点后再更新学习进度')
    return
  }

  syncProgress(selectedNode.value.id)
  appStore.showToast('进度已更新', `已按点击节点记录，当前路线完成度 ${progress.value}%`)
}

function favoriteRoadmap() {
  appStore.showToast('已收藏路线', '后续可在个人中心继续学习')
}

function openNodeDetail(node: RoadmapTimelineItem, index: number) {
  selectedNode.value = node
  selectedNodeIndex.value = index
  syncProgress(node.id)
}

function closeNodeDetail() {
  selectedNode.value = null
  selectedNodeIndex.value = null
}

function startLearningNode() {
  if (!selectedNode.value) return
  syncProgress(selectedNode.value.id)
  router.push({ name: 'roadmap-study', params: { id: route.params.id, nodeId: selectedNode.value.id } })
  closeNodeDetail()
}

function openStudyView(nodeId?: number) {
  const targetNodeId = nodeId ?? selectedNode.value?.id
  if (typeof targetNodeId === 'number') {
    syncProgress(targetNodeId)
  }
  router.push({ name: 'roadmap-study', params: { id: route.params.id, nodeId: targetNodeId } })
}

function goToResource(id: number) {
  router.push({ name: 'resource-detail', params: { id } })
  closeNodeDetail()
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function renderMarkdown(content: string) {
  const lines = content.split('\n')
  const chunks: string[] = []
  let listBuffer: string[] = []

  const flushList = () => {
    if (!listBuffer.length) return
    chunks.push(`<ul>${listBuffer.join('')}</ul>`)
    listBuffer = []
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      flushList()
      continue
    }
    if (line.startsWith('- ')) {
      listBuffer.push(`<li>${escapeHtml(line.slice(2))}</li>`)
      continue
    }

    flushList()

    if (line.startsWith('# ')) {
      chunks.push(`<h1>${escapeHtml(line.slice(2))}</h1>`)
    } else if (line.startsWith('## ')) {
      chunks.push(`<h2>${escapeHtml(line.slice(3))}</h2>`)
    } else if (line.startsWith('> ')) {
      chunks.push(`<blockquote>${escapeHtml(line.slice(2))}</blockquote>`)
    } else {
      chunks.push(`<p>${escapeHtml(line)}</p>`)
    }
  }

  flushList()
  return chunks.join('')
}

function syncProgress(nodeId?: number) {
  if (!roadmap.value) {
    progress.value = 0
    return
  }

  const validNodeIds = roadmap.value.timeline.map((item) => item.id)
  const visitedNodeIds = typeof nodeId === 'number'
    ? markRoadmapNodeVisited(roadmap.value.id, nodeId, validNodeIds)
    : getVisitedRoadmapNodeIds(roadmap.value.id, validNodeIds)

  progress.value = calculateRoadmapProgressPercent(roadmap.value.timeline.length, visitedNodeIds.length)
}

watch(() => route.params.id, () => {
  void loadRoadmapDetail()
}, { immediate: true })
</script>

<style scoped lang="scss">
.detail-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-6);
  position: relative;
}

.detail-main,
.detail-side {
  display: grid;
  gap: var(--space-5);
}

.panel {
  padding: var(--space-6);
}

.progress-panel {
  display: grid;
  gap: 1.25rem;
  padding: 1.4rem;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(14, 165, 233, 0.14), transparent 58%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(241, 245, 249, 0.92));
}

.progress-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.progress-panel__eyebrow {
  margin: 0;
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0369a1;
}

.progress-panel__badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.3rem 0.7rem;
  background: rgba(14, 165, 233, 0.12);
  color: #0f172a;
  font-size: 0.82rem;
  font-weight: 700;
}

.progress-panel__meter {
  display: flex;
  justify-content: center;
}

.progress-panel__ring {
  --progress-angle: calc(var(--progress) * 3.6deg);
  position: relative;
  display: grid;
  place-items: center;
  width: 10.5rem;
  height: 10.5rem;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(255, 255, 255, 0.98) 0 58%, transparent 59%),
    conic-gradient(#0ea5e9 0deg, #38bdf8 var(--progress-angle), rgba(148, 163, 184, 0.18) var(--progress-angle), rgba(148, 163, 184, 0.18) 360deg);
  box-shadow:
    inset 0 0 0 1px rgba(148, 163, 184, 0.14),
    0 18px 40px rgba(14, 165, 233, 0.12);
}

.progress-panel__ring::after {
  content: '';
  position: absolute;
  inset: 0.9rem;
  border-radius: 50%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
}

.progress-panel__ring-core {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.15rem;
  justify-items: center;
}

.progress-panel__ring-core strong {
  font-size: 2rem;
  line-height: 1;
  color: #020617;
}

.progress-panel__ring-core span {
  font-size: 0.82rem;
  color: #475569;
}

.progress-panel__summary strong,
.progress-panel__summary p {
  margin: 0;
}

.progress-panel__summary strong {
  display: block;
  font-size: 1rem;
  color: #0f172a;
}

.progress-panel__summary p {
  margin-top: 0.45rem;
  color: #475569;
  line-height: 1.65;
  font-size: 0.92rem;
}

.progress-panel__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.progress-panel__stats div {
  padding: 0.95rem 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.progress-panel__stats dt {
  margin: 0;
  font-size: 0.8rem;
  color: #64748b;
}

.progress-panel__stats dd {
  margin: 0.35rem 0 0;
  font-size: 1.5rem;
  line-height: 1;
  font-weight: 700;
  color: #0f172a;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.attachment-list {
  margin: var(--space-3) 0 0;
  padding-left: 1.2rem;
}

.attachment-list a {
  color: var(--color-primary);
}

.markdown-preview {
  color: #111827;
  line-height: 1.7;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(p),
.markdown-preview :deep(blockquote),
.markdown-preview :deep(ul) {
  margin: 0 0 1rem;
}

.markdown-preview :deep(blockquote) {
  padding: 0.75rem 1rem;
  border-left: 4px solid #cbd5e1;
  background: #f8fafc;
  border-radius: 0 0.75rem 0.75rem 0;
}

.markdown-preview :deep(ul) {
  padding-left: 1.25rem;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

.node-modal {
  width: 90%;
  max-width: 600px;
  background: var(--color-surface);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  animation: slideUp 0.3s ease-out;
  max-height: 85vh;
  overflow: hidden;
}

.modal-header {
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--color-text-primary);
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  color: var(--color-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: var(--color-surface-hover);
  color: var(--color-text-primary);
}

.modal-body {
  padding: var(--space-6);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.node-summary p {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.node-tasks ul {
  margin-top: var(--space-3);
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.node-tasks li {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  background: var(--color-surface-hover);
  padding: var(--space-3) var(--space-4);
  border-radius: 8px;
  color: var(--color-text-primary);
}

.task-icon {
  color: var(--color-primary);
  font-size: 1.2rem;
  margin-top: 2px;
}

.node-resources {
  margin-top: var(--space-2);
}

.resource-preview {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  transition: border-color 0.2s ease;
}

.resource-preview:hover {
  border-color: var(--color-primary);
}

.resource-icon {
  font-size: 1.5rem;
  color: var(--color-primary);
}

.resource-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.resource-title {
  font-weight: 500;
  color: var(--color-text-primary);
}

.resource-meta {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

.view-btn {
  background: transparent;
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-btn:hover {
  background: var(--color-primary);
  color: white;
}

.modal-footer {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-4);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@media (max-width: 64rem) {
  .detail-grid,
  .related-grid {
    grid-template-columns: 1fr;
  }

  .progress-panel__ring {
    width: 9rem;
    height: 9rem;
  }
}
</style>
