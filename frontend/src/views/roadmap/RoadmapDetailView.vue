
<template>
  <div class="page-shell" v-if="status === 'loading'">
    <BaseSkeleton height="18rem" />
    <div class="detail-grid" style="margin-top: var(--space-5);">
      <BaseSkeleton height="14rem" />
      <BaseSkeleton height="14rem" />
    </div>
  </div>

  <div class="page-shell detail-grid" v-else-if="status === 'ready' && roadmap">
    <section class="detail-main">
      <HeroBanner kicker="路线详情" :title="roadmap.title" :description="roadmap.description || ''">
        <template #actions>
          <BaseButton :loading="updating" @click="markComplete">标记本阶段完成</BaseButton>
          <BaseButton variant="secondary" @click="favoriteRoadmap">收藏路线</BaseButton>
        </template>
      </HeroBanner>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>节点进度结构</h2>
          <p>点击每个阶段节点，即可查看具体的内容、学习资源与讨论。</p>
        </div>
        <RoadmapTimeline :items="timelineItems" @node-click="openNodeDetail" />
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>关联资料</h2>
          <p>路线中的关键节点可以直接跳到相关资料或笔记。</p>
        </div>
        <div class="related-grid">
          <ResourceCard v-for="item in resources.slice(0, 2)" :key="item.id" :item="item" />
        </div>
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>评论互动</h2>
          <p>路线讨论和问题记录统一沉淀。</p>
        </div>
        <CommentList :items="resourceComments" />
      </div>
    </section>

    <aside class="detail-side">
      <BaseEmpty title="阶段进度" :description="`当前进度 ${progress}%`" />
      <BaseEmpty title="联动" description="后续可接已完成节点、路线收藏和评论接口。" />
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
            <p>{{ selectedNode.summary }}</p>
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
            <div class="resource-preview" v-if="resources[selectedNodeIndex]">
              <div class="i-carbon-document resource-icon"></div>
              <div class="resource-info">
                <span class="resource-title">{{ resources[selectedNodeIndex].title }}</span>
                <span class="resource-meta">{{ resources[selectedNodeIndex].fileType }} · {{ resources[selectedNodeIndex].downloadCount }} 次下载</span>
              </div>
              <button class="view-btn" @click="goToResource(resources[selectedNodeIndex].id)">查看</button>
            </div>
            <p v-else class="text-muted text-sm">暂无专属资料关联</p>
          </div>
        </div>
        <div class="modal-footer">
          <BaseButton variant="secondary" @click="closeNodeDetail">关闭</BaseButton>
          <BaseButton @click="startLearningNode">开始学习本阶段</BaseButton>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="page-shell">
    <BaseErrorState v-if="loadError" title="加载失败" description="请稍后重试或检查网络。" />
    <BaseErrorState v-else title="路线不存在" description="请返回路线广场重新选择。" />
  </div>
</template>
 
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import CommentList from '@/components/business/CommentList.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import RoadmapTimeline from '@/components/business/RoadmapTimeline.vue'
import { fetchRoadmapDetail, updateRoadmapProgress, type RoadmapNode } from '@/api/roadmaps'
import { resourceComments, resources } from '@/mock/resources'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const roadmap = ref<{ id: number; title: string; description?: string | null } | null>(null)
const nodes = ref<RoadmapNode[]>([])
const progress = ref(0)
const loading = ref(false)
const loadError = ref(false)
const updating = ref(false)

const timelineItems = computed(() =>
  nodes.value.map((item) => ({
    title: item.title,
    summary: item.summary || item.description || '该阶段待补充说明',
    tasks: item.tasks || []
  }))
)

const status = computed(() => {
  if (loadError.value) return 'error'
  if (loading.value) return 'loading'
  if (roadmap.value) return 'ready'
  return 'error'
})

// Node modal state
const selectedNode = ref<any>(null)
const selectedNodeIndex = ref<number | null>(null)

async function loadDetail() {
  loading.value = true
  loadError.value = false
  try {
    const data = await fetchRoadmapDetail(route.params.id as string)
    roadmap.value = data?.roadmap || null
    nodes.value = data?.nodes || []
    progress.value = Number(data?.progress?.value ?? data?.progress?.progress ?? 0)
  } catch (error: any) {
    loadError.value = true
    appStore.showToast('路线详情加载失败', error?.response?.data?.message || '请稍后再试', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)

function markComplete() {
  if (!roadmap.value) return
  const next = Math.min(100, progress.value + 18)
  updating.value = true
  updateRoadmapProgress(roadmap.value.id, next)
    .then(() => {
      progress.value = next
      appStore.showToast('进度已更新', `当前路线完成度 ${progress.value}%`)
    })
    .catch((error: any) => {
      appStore.showToast('进度更新失败', error?.response?.data?.message || '请稍后重试', 'error')
    })
    .finally(() => {
      updating.value = false
    })
}

function favoriteRoadmap() {
  appStore.showToast('已收藏路线', '后续可在个人中心继续学习')
}

function openNodeDetail(node: any, index: number) {
  selectedNode.value = node
  selectedNodeIndex.value = index
}

function closeNodeDetail() {
  selectedNode.value = null
  selectedNodeIndex.value = null
}

function startLearningNode() {
  appStore.showToast('学习开启', `已为您加载《${selectedNode.value.title}》的课程资料`)
  if (selectedNodeIndex.value !== null && resources[selectedNodeIndex.value]) {
    router.push({ name: 'resource-detail', params: { id: resources[selectedNodeIndex.value].id } })
  }
  closeNodeDetail()
}

function goToResource(id: number) {
  router.push({ name: 'resource-detail', params: { id } })
  closeNodeDetail()
}
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

.related-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
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
}
</style>
