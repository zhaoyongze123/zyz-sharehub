<template>
  <div v-if="loading" class="page-shell study-layout">
    <BaseSkeleton height="14rem" />
    <BaseSkeleton height="26rem" />
    <BaseSkeleton height="20rem" />
  </div>

  <div v-else-if="roadmap" class="page-shell study-layout">
    <section class="study-main">
      <HeroBanner kicker="路线学习" :title="activeNode?.title || roadmap.title" :description="heroDescription">
        <template #actions>
          <BaseButton variant="secondary" @click="goBackToDetail">返回路线详情</BaseButton>
          <BaseButton v-if="selectedResource" @click="openResourceDetail(selectedResource.id)">查看关联资料</BaseButton>
          <BaseButton v-else-if="selectedNote" @click="openNoteDetail(selectedNote.id)">查看关联笔记</BaseButton>
        </template>
      </HeroBanner>

      <div class="glass-panel panel markdown-panel">
        <div class="section-heading">
          <h2>Markdown 学习视图</h2>
          <p>当前阶段与整条路线已整理为可阅读的 Markdown 预览格式。</p>
        </div>
        <article class="markdown-body" v-html="renderedMarkdown"></article>
      </div>

      <div v-if="selectedNote" class="glass-panel panel markdown-panel">
        <div class="section-heading">
          <h2>关联笔记预览</h2>
          <p>当前阶段绑定的笔记内容已接入真实详情接口。</p>
        </div>
        <article class="markdown-body" v-html="renderNoteMarkdown(selectedNote.contentMd)"></article>
      </div>

      <div v-else-if="selectedResource" class="glass-panel panel">
        <div class="section-heading">
          <h2>关联资料预览</h2>
          <p>当前阶段绑定了资料，可直接继续深挖阅读。</p>
        </div>
        <div class="linked-resource-card">
          <div>
            <h3>{{ selectedResource.title }}</h3>
            <p>{{ selectedResource.summary }}</p>
            <div class="meta-row">
              <span>{{ selectedResource.fileType }}</span>
              <span>下载 {{ selectedResource.downloadCount }}</span>
              <span>收藏 {{ selectedResource.favorites }}</span>
            </div>
          </div>
          <div class="action-row">
            <BaseButton variant="secondary" @click="openResourceDetail(selectedResource.id)">资料详情</BaseButton>
            <BaseButton :disabled="!selectedResource.previewUrl" @click="openResourceFile">打开附件</BaseButton>
          </div>
        </div>
      </div>
    </section>

    <aside class="study-side">
      <section class="glass-panel panel">
        <div class="section-heading">
          <h2>学习阶段</h2>
          <p>切换阶段后，Markdown 预览和附件面板会同步更新。</p>
        </div>
        <div class="stage-list">
          <button
            v-for="item in roadmap.timeline"
            :key="item.id"
            class="stage-item"
            :class="{ 'stage-item--active': item.id === activeNode?.id }"
            @click="selectNode(item.id)"
          >
            <span class="stage-title">{{ item.title }}</span>
            <span class="stage-summary">{{ item.summary }}</span>
          </button>
        </div>
      </section>

      <section class="glass-panel panel">
        <div class="section-heading">
          <h2>节点附件</h2>
          <p>支持图片与 PDF 在线预览，其他类型保留下载入口。</p>
        </div>

        <div v-if="activeNode?.attachments.length" class="attachment-stack">
          <div class="attachment-tabs">
            <button
              v-for="attachment in activeNode.attachments"
              :key="attachment.id"
              class="attachment-tab"
              :class="{ 'attachment-tab--active': attachment.id === selectedAttachment?.id }"
              @click="selectedAttachmentId = String(attachment.id)"
            >
              {{ attachment.filename }}
            </button>
          </div>

          <div v-if="selectedAttachment" class="attachment-preview">
            <div class="attachment-meta-card">
              <div>
                <h3>{{ selectedAttachment.filename }}</h3>
                <p>{{ selectedAttachment.contentType || '未知类型' }} · {{ formatAttachmentSize(selectedAttachment.size) }}</p>
              </div>
              <p class="attachment-hint">进入学习页和切换节点时不会自动加载附件，需手动点击预览或下载。</p>
            </div>

            <img
              v-if="attachmentPreviewActivated && attachmentPreviewMode === 'image'"
              :src="selectedAttachment.downloadUrl"
              :alt="selectedAttachment.filename"
              class="attachment-image"
            />
            <iframe
              v-else-if="attachmentPreviewActivated && (attachmentPreviewMode === 'pdf' || attachmentPreviewMode === 'text')"
              :src="selectedAttachment.downloadUrl"
              class="attachment-frame"
              :title="selectedAttachment.filename"
            />
            <BaseEmpty
              v-else-if="attachmentPreviewActivated"
              title="该附件暂不支持页面内预览"
              description="请点击下载后在本地查看。"
            />
            <BaseEmpty
              v-else
              title="附件尚未加载"
              description="点击下方按钮后再预览，避免进入页面时自动触发下载。"
            />
            <div class="action-row">
              <BaseButton variant="secondary" @click="activateAttachmentPreview">预览附件</BaseButton>
              <BaseButton @click="openAttachment">下载 / 打开附件</BaseButton>
            </div>
          </div>
        </div>

        <BaseEmpty
          v-else
          title="暂无节点附件"
          description="当前阶段还没有上传附件。"
        />
      </section>
    </aside>
  </div>

  <div v-else class="page-shell">
    <BaseErrorState title="路线加载失败" description="请返回路线详情后重试。" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchNoteDetail, type NoteDTO } from '@/api/notes'
import { fetchResourceDetail, type ResourceItem } from '@/api/resources'
import {
  calculateRoadmapProgressPercent,
  fetchRoadmapDetail,
  getVisitedRoadmapNodeIds,
  markRoadmapNodeVisited,
  type RoadmapDetail,
  type RoadmapNodeAttachment,
  type RoadmapTimelineItem
} from '@/api/roadmaps'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const roadmap = ref<RoadmapDetail | null>(null)
const selectedNodeId = ref<number | null>(null)
const selectedAttachmentId = ref<string | null>(null)
const selectedResource = ref<ResourceItem | null>(null)
const selectedNote = ref<NoteDTO | null>(null)
const attachmentPreviewActivated = ref(false)
const visitedNodeIds = ref<number[]>([])

const activeNode = computed(() => {
  if (!roadmap.value?.timeline.length) return null
  return roadmap.value.timeline.find((item) => item.id === selectedNodeId.value) ?? roadmap.value.timeline[0]
})

const selectedAttachment = computed(() => {
  if (!activeNode.value?.attachments.length) return null
  return activeNode.value.attachments.find((item) => String(item.id) === selectedAttachmentId.value) ?? activeNode.value.attachments[0]
})

const attachmentPreviewMode = computed(() => {
  if (!selectedAttachment.value) return 'none'
  const contentType = selectedAttachment.value.contentType?.toLowerCase() || ''
  if (contentType.startsWith('image/')) return 'image'
  if (contentType.includes('pdf')) return 'pdf'
  if (contentType.startsWith('text/') || contentType.includes('json') || contentType.includes('markdown')) return 'text'
  return 'unsupported'
})

const heroDescription = computed(() => {
  if (!activeNode.value) {
    return roadmap.value?.summary || '已切换到路线学习视图。'
  }
  return activeNode.value.description || activeNode.value.summary || '当前阶段已进入学习状态。'
})

const progressPercent = computed(() => {
  if (!roadmap.value) return 0
  return calculateRoadmapProgressPercent(roadmap.value.timeline.length, visitedNodeIds.value.length)
})

const roadmapMarkdown = computed(() => {
  if (!roadmap.value) return ''
  const currentNode = activeNode.value
  const lines: string[] = [
    `# ${roadmap.value.title}`,
    '',
    `> ${roadmap.value.summary}`,
    '',
    `- 状态：${roadmap.value.status}`,
    `- 可见性：${roadmap.value.visibility}`,
    `- 总阶段数：${roadmap.value.timeline.length}`,
    `- 当前学习进度：${progressPercent.value}%`,
    ''
  ]

  if (currentNode) {
    lines.push(`## 当前阶段：${currentNode.title}`, '')
    if (currentNode.description) {
      lines.push(currentNode.description, '')
    }
    if (currentNode.tasks.length) {
      lines.push('### 当前阶段任务')
      currentNode.tasks.forEach((task) => lines.push(`- ${task}`))
      lines.push('')
    }
    if (currentNode.attachments.length) {
      lines.push('### 当前阶段附件')
      currentNode.attachments.forEach((attachment) => lines.push(`- [${attachment.filename}](${attachment.downloadUrl})`))
      lines.push('')
    }
  }

  lines.push('## 全部阶段')
  roadmap.value.timeline.forEach((node, index) => {
    lines.push('', `### ${index + 1}. ${node.title}`)
    if (node.description) {
      lines.push(node.description)
    }
    if (node.tasks.length) {
      node.tasks.forEach((task) => lines.push(`- ${task}`))
    }
    if (node.attachments.length) {
      node.attachments.forEach((attachment) => lines.push(`- 附件：[${attachment.filename}](${attachment.downloadUrl})`))
    }
  })

  return lines.join('\n')
})

const renderedMarkdown = computed(() => renderNoteMarkdown(roadmapMarkdown.value))

async function loadRoadmap() {
  loading.value = true
  selectedResource.value = null
  selectedNote.value = null
  attachmentPreviewActivated.value = false
  try {
    const detail = await fetchRoadmapDetail(String(route.params.id))
    roadmap.value = detail

    const routeNodeId = Number(route.params.nodeId)
    const initialNode = detail.timeline.find((item) => item.id === routeNodeId) ?? detail.timeline[0] ?? null
    selectedNodeId.value = initialNode?.id ?? null
    selectedAttachmentId.value = initialNode?.attachments[0] ? String(initialNode.attachments[0].id) : null
    syncVisitedProgress(initialNode?.id ?? null)
    await loadLinkedContent()
  } finally {
    loading.value = false
  }
}

function syncVisitedProgress(nodeId: number | null) {
  if (!roadmap.value) {
    visitedNodeIds.value = []
    return
  }

  const validNodeIds = roadmap.value.timeline.map((item) => item.id)
  visitedNodeIds.value = nodeId === null
    ? getVisitedRoadmapNodeIds(roadmap.value.id, validNodeIds)
    : markRoadmapNodeVisited(roadmap.value.id, nodeId, validNodeIds)
}

async function loadLinkedContent() {
  const node = activeNode.value
  selectedResource.value = null
  selectedNote.value = null

  if (!node) return

  const tasks: Promise<void>[] = []
  if (typeof node.resourceId === 'number') {
    tasks.push(
      fetchResourceDetail(node.resourceId).then((data) => {
        selectedResource.value = data
      }).catch(() => {
        selectedResource.value = null
      })
    )
  }
  if (typeof node.noteId === 'number') {
    tasks.push(
      fetchNoteDetail(node.noteId).then((data) => {
        selectedNote.value = data
      }).catch(() => {
        selectedNote.value = null
      })
    )
  }
  await Promise.all(tasks)
}

function selectNode(nodeId: number) {
  selectedNodeId.value = nodeId
  selectedAttachmentId.value = activeNode.value?.attachments[0] ? String(activeNode.value.attachments[0].id) : null
  attachmentPreviewActivated.value = false
  syncVisitedProgress(nodeId)
  void loadLinkedContent()
  router.replace({ name: 'roadmap-study', params: { id: route.params.id, nodeId } })
}

function goBackToDetail() {
  router.push({ name: 'roadmap-detail', params: { id: route.params.id } })
}

function openResourceDetail(id: number) {
  router.push({ name: 'resource-detail', params: { id } })
}

function openNoteDetail(id: number) {
  router.push({ name: 'note-detail', params: { id } })
}

function openAttachment() {
  if (!selectedAttachment.value) return
  window.open(selectedAttachment.value.downloadUrl, '_blank', 'noopener')
}

function activateAttachmentPreview() {
  attachmentPreviewActivated.value = true
}

function openResourceFile() {
  if (!selectedResource.value?.previewUrl) return
  window.open(selectedResource.value.previewUrl, '_blank', 'noopener')
}

function formatAttachmentSize(size: number) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function renderInlineMarkdown(value: string) {
  return escapeHtml(value)
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

function renderNoteMarkdown(content: string) {
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
      listBuffer.push(`<li>${renderInlineMarkdown(line.slice(2))}</li>`)
      continue
    }

    flushList()

    if (line.startsWith('# ')) {
      chunks.push(`<h1>${renderInlineMarkdown(line.slice(2))}</h1>`)
    } else if (line.startsWith('## ')) {
      chunks.push(`<h2>${renderInlineMarkdown(line.slice(3))}</h2>`)
    } else if (line.startsWith('### ')) {
      chunks.push(`<h3>${renderInlineMarkdown(line.slice(4))}</h3>`)
    } else if (line.startsWith('> ')) {
      chunks.push(`<blockquote>${renderInlineMarkdown(line.slice(2))}</blockquote>`)
    } else {
      chunks.push(`<p>${renderInlineMarkdown(line)}</p>`)
    }
  }

  flushList()
  return chunks.join('')
}

watch(() => [route.params.id, route.params.nodeId], () => {
  void loadRoadmap()
}, { immediate: true })
</script>

<style scoped lang="scss">
.study-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(18rem, 0.9fr);
  gap: var(--space-6);
}

.study-main,
.study-side {
  display: grid;
  gap: var(--space-5);
}

.study-main :deep(.hero h1) {
  font-size: clamp(1.8rem, 3vw, 2.6rem);
  line-height: 1.15;
  letter-spacing: -0.035em;
}

.panel {
  padding: var(--space-6);
}

.markdown-panel {
  overflow: hidden;
}

.markdown-body {
  color: #111827;
  line-height: 1.75;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 0 0 1rem;
  letter-spacing: -0.03em;
}

.markdown-body :deep(p),
.markdown-body :deep(blockquote),
.markdown-body :deep(ul) {
  margin: 0 0 1rem;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #cbd5e1;
  padding: 0.75rem 1rem;
  background: #f8fafc;
  border-radius: 0 0.75rem 0.75rem 0;
  color: #475569;
}

.markdown-body :deep(ul) {
  padding-left: 1.25rem;
}

.markdown-body :deep(li) {
  margin-bottom: 0.5rem;
}

.markdown-body :deep(a) {
  color: var(--color-primary);
}

.markdown-body :deep(code) {
  padding: 0.1rem 0.35rem;
  background: #e2e8f0;
  border-radius: 0.35rem;
}

.stage-list,
.attachment-stack {
  display: grid;
  gap: 0.75rem;
}

.stage-item,
.attachment-tab {
  width: 100%;
  border: 1px solid var(--color-border);
  background: white;
  border-radius: 0.9rem;
  padding: 0.9rem 1rem;
  text-align: left;
}

.stage-item--active,
.attachment-tab--active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.18);
}

.stage-title {
  display: block;
  font-weight: 700;
  color: #0f172a;
}

.stage-summary {
  display: block;
  margin-top: 0.35rem;
  color: #64748b;
  font-size: 0.92rem;
}

.attachment-tabs {
  display: grid;
  gap: 0.5rem;
}

.attachment-preview {
  display: grid;
  gap: 0.9rem;
}

.attachment-meta-card {
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 1rem;
  background: #fff;
}

.attachment-meta-card h3,
.attachment-meta-card p {
  margin: 0;
}

.attachment-meta-card h3 {
  margin-bottom: 0.4rem;
}

.attachment-hint {
  margin-top: 0.75rem !important;
  color: #64748b;
  font-size: 0.92rem;
}

.attachment-image,
.attachment-frame {
  width: 100%;
  min-height: 18rem;
  border: 1px solid var(--color-border);
  border-radius: 1rem;
  background: white;
}

.attachment-image {
  object-fit: contain;
  padding: 0.5rem;
}

.linked-resource-card {
  display: grid;
  gap: 1rem;
}

.linked-resource-card h3 {
  margin: 0 0 0.75rem;
}

.linked-resource-card p {
  margin: 0;
  color: #475569;
}

.meta-row,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.meta-row {
  margin-top: 0.9rem;
  color: #64748b;
  font-size: 0.92rem;
}

@media (max-width: 1024px) {
  .study-layout {
    grid-template-columns: 1fr;
  }

  .study-main :deep(.hero h1) {
    font-size: clamp(1.5rem, 6vw, 2rem);
  }
}
</style>
