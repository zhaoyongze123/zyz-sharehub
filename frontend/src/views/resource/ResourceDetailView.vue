<template>
  <div v-if="loading" class="page-shell detail-grid">
    <section class="detail-main">
      <BaseSkeleton height="14rem" />
      <BaseSkeleton height="10rem" />
      <BaseSkeleton height="4rem" />
    </section>
    <aside class="detail-side">
      <BaseSkeleton height="8rem" />
    </aside>
  </div>

  <div class="page-shell detail-grid" v-else-if="resource">
    <section class="detail-main">
      <HeroBanner kicker="资料详情" :title="resource.title" :description="resource.summary">
        <template #actions>
          <BaseButton @click="downloadResource">下载资源</BaseButton>
          <BaseButton variant="secondary" @click="favoriteResource">收藏</BaseButton>
        </template>
      </HeroBanner>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>资料概览</h2>
          <p>标题、作者、标签、简介、下载与互动全部在同一页闭合。</p>
        </div>
        <ResourceMeta :author="resource.author" :category="resource.category" :updated-at="resource.updatedAt" />
        <div class="tag-row">
          <BaseTag v-for="tag in resource.tags" :key="tag" tone="primary">{{ tag }}</BaseTag>
        </div>
        <div class="stats-row">
          <BaseTag>下载 {{ resource.downloadCount }}</BaseTag>
          <BaseTag>点赞 {{ resource.likes }}</BaseTag>
          <BaseTag>收藏 {{ resource.favorites }}</BaseTag>
        </div>
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>正文预览</h2>
          <p>展示资料正文要点，可直接浏览长文内容后再决定下载。</p>
        </div>
        <div class="content-preview">{{ resource.summary }}</div>
      </div>

      <InteractionBar :likes="likes" :favorites="favorites" @like="likeResource" @favorite="favoriteResource" @report="reportVisible = true" />

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>相关推荐</h2>
          <p>和当前主题相近的模板与资料，方便继续扩展学习。</p>
        </div>
        <div class="related-grid">
          <ResourceCard v-for="item in relatedResources" :key="item.id" :item="item" />
        </div>
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>评论区</h2>
          <p>欢迎交流使用体验与实践心得。</p>
        </div>
        <CommentList :items="resourceComments" />
      </div>
    </section>

    <aside class="detail-side">
      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>附件与来源</h2>
          <p>优先查看附件预览，其次跳转官方来源链接。</p>
        </div>
        <BaseButton v-if="resource.objectKey" variant="secondary" @click="previewAttachment">预览附件</BaseButton>
        <BaseButton v-if="resource.externalUrl" variant="secondary" @click="openSource">打开来源</BaseButton>
        <BaseEmpty v-if="!resource.objectKey && !resource.externalUrl" title="暂无附件" description="当前资料未配置附件或来源链接。" />
      </div>
      <BaseEmpty title="状态覆盖" description="若访问不存在的资料，请直接改 URL 为不存在 id，会显示 404 态。" />
    </aside>

    <ReportDialog
      v-model:reason="reportReason"
      :visible="reportVisible"
      :submitting="reporting"
      :error-message="reportErrorMessage"
      @close="closeReportDialog"
      @submit="submitReport"
    />
  </div>

  <div v-else-if="notFound" class="page-shell">
    <BaseErrorState title="资料不存在" description="当前资源可能已下架，或你访问的 ID 不存在。">
      <BaseButton @click="router.push('/resources')">返回资料广场</BaseButton>
    </BaseErrorState>
  </div>

  <div v-else class="page-shell">
    <BaseErrorState title="资料加载失败" description="服务暂时不可用，请稍后刷新重试。" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { createReport } from '@/api/reports'
import { fetchRelatedResources, fetchResourceDetail, type ResourceItem } from '@/api/resources'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTag from '@/components/base/BaseTag.vue'
import CommentList from '@/components/business/CommentList.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import InteractionBar from '@/components/business/InteractionBar.vue'
import ReportDialog from '@/components/business/ReportDialog.vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import ResourceMeta from '@/components/business/ResourceMeta.vue'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const reportVisible = ref(false)
const reportReason = ref('')
const reporting = ref(false)
const reportErrorMessage = ref('')
const loading = ref(true)
const notFound = ref(false)
const resource = ref<ResourceItem | null>(null)
const relatedResources = ref<ResourceItem[]>([])
const likes = ref(0)
const favorites = ref(0)

const resourceComments = [
  { author: 'Mina', createdAt: '2 小时前', content: '内容结构很清晰，按步骤实践就能快速落地。' },
  { author: 'Alex', createdAt: '昨天', content: '对新手很友好，关键流程和注意点都讲到了。' }
]

async function loadResource() {
  loading.value = true
  notFound.value = false
  resource.value = null
  relatedResources.value = []

  try {
    const detail = await fetchResourceDetail(String(route.params.id))
    resource.value = detail
    likes.value = detail.likes
    favorites.value = detail.favorites
    relatedResources.value = await fetchRelatedResources(detail.id)
  } catch (error) {
    const status = axios.isAxiosError(error) ? (error.response?.status ?? 0) : 0
    if ([404, 410].includes(status)) {
      notFound.value = true
    }
  } finally {
    loading.value = false
  }
}

function likeResource() {
  likes.value += 1
  appStore.showToast('已点赞', '这个资料已加入你的正反馈记录')
}

function favoriteResource() {
  favorites.value += 1
  appStore.showToast('已收藏', '稍后可在个人中心里查看')
}

function downloadResource() {
  appStore.showToast('下载任务已启动', '准备完成后将自动开始下载')
}

function previewAttachment() {
  if (!resource.value?.objectKey) {
    return
  }
  appStore.showToast('附件预览', `附件键：${resource.value.objectKey}`)
}

function openSource() {
  if (!resource.value?.externalUrl) {
    return
  }
  window.open(resource.value.externalUrl, '_blank', 'noopener,noreferrer')
}

function closeReportDialog() {
  if (reporting.value) {
    return
  }
  reportReason.value = ''
  reportErrorMessage.value = ''
  reportVisible.value = false
}

async function submitReport() {
  if (!resource.value || reporting.value) {
    return
  }

  if (!reportReason.value.trim()) {
    reportErrorMessage.value = '请填写举报原因'
    return
  }

  reporting.value = true
  reportErrorMessage.value = ''

  try {
    const report = await createReport({
      targetType: 'RESOURCE',
      resourceId: resource.value.id,
      reason: reportReason.value.trim()
    })
    closeReportDialog()
    appStore.showToast('举报已提交', `举报 #${report.id} 已进入后台处理队列`)
  } catch (error) {
    if (axios.isAxiosError(error)) {
      reportErrorMessage.value = error.response?.data?.msg || error.response?.data?.message || '举报提交失败，请稍后重试'
    } else {
      reportErrorMessage.value = error instanceof Error && error.message.trim()
        ? error.message
        : '举报提交失败，请稍后重试'
    }
  } finally {
    reporting.value = false
  }
}

watch(() => route.params.id, () => {
  void loadResource()
})

onMounted(() => {
  void loadResource()
})
</script>

<style scoped lang="scss">
.detail-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-6);
}

.detail-main,
.detail-side {
  display: grid;
  gap: var(--space-5);
}

.panel {
  padding: var(--space-6);
}

.tag-row,
.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.content-preview {
  white-space: pre-wrap;
  line-height: 1.7;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  background: color-mix(in srgb, var(--surface-elevated) 75%, transparent);
}

@media (max-width: 64rem) {
  .detail-grid,
  .related-grid {
    grid-template-columns: 1fr;
  }
}
</style>
