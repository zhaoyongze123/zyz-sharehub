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
          <BaseButton variant="secondary" @click="handleFavoriteResource">收藏</BaseButton>
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

      <InteractionBar :likes="likes" :favorites="favorites" @like="handleLikeResource" @favorite="handleFavoriteResource" @report="reportVisible = true" />

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
          <p>当前先保留页面侧静态评论占位，后续再接评论真接口。</p>
        </div>
        <CommentList :items="resourceComments" />
      </div>
    </section>

    <aside class="detail-side">
      <section class="glass-panel panel preview-panel">
        <div class="section-heading">
          <h2>附件预览</h2>
          <p>已接后端资料附件，支持 PDF 页面内预览与文档下载。</p>
        </div>

        <div v-if="previewLoading" class="preview-loading">
          <BaseSkeleton height="24rem" />
        </div>

        <div v-else-if="previewMode === 'pdf' && pdfPreviewUrl" class="preview-frame-wrap">
          <iframe :src="pdfPreviewUrl" title="PDF 资料预览" class="preview-frame" />
        </div>

        <div v-else-if="previewMode === 'office' && officePreviewUrl" class="preview-frame-wrap">
          <iframe :src="officePreviewUrl" title="Office 文档预览" class="preview-frame" />
        </div>

        <BaseEmpty
          v-else-if="previewMode === 'empty'"
          title="暂无附件"
          description="当前资料还没有上传可预览的附件。"
        />

        <BaseEmpty
          v-else-if="previewMode === 'unsupported'"
          title="暂不支持在线预览"
          :description="`当前文件类型为 ${resource.fileType}，请直接下载查看。`"
        />

        <BaseErrorState
          v-else
          title="附件预览失败"
          :description="previewErrorMessage || '当前预览链路加载失败，请直接下载文件查看。'"
        />

        <BaseButton :disabled="!resource.previewUrl" @click="downloadResource">下载资源</BaseButton>
      </section>
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
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { createReport } from '@/api/reports'
import {
  favoriteResource,
  fetchRelatedResources,
  fetchResourceDetail,
  likeResource,
  type ResourceItem
} from '@/api/resources'
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
const previewLoading = ref(false)
const previewMode = ref<'pdf' | 'office' | 'unsupported' | 'empty' | 'error'>('empty')
const pdfPreviewUrl = ref('')
const officePreviewUrl = ref('')
const previewErrorMessage = ref('')

const resourceComments = [
  { author: 'Mina', createdAt: '2 小时前', content: '资源详情已切到真实接口后，这里继续补评论真数据会更顺。' },
  { author: 'Alex', createdAt: '昨天', content: '相关推荐已经来自后端，评论区下一轮可以继续收口。' }
]

async function loadResource() {
  loading.value = true
  notFound.value = false
  resource.value = null
  relatedResources.value = []
  resetPreviewState()

  try {
    const detail = await fetchResourceDetail(String(route.params.id))
    resource.value = detail
    likes.value = detail.likes
    favorites.value = detail.favorites
    relatedResources.value = await fetchRelatedResources(detail.id)
    await loadPreview(detail)
  } catch (error) {
    const status = axios.isAxiosError(error) ? (error.response?.status ?? 0) : 0
    if ([404, 410].includes(status)) {
      notFound.value = true
    }
  } finally {
    loading.value = false
  }
}

async function handleLikeResource() {
  if (!resource.value) {
    return
  }
  const result = await likeResource(resource.value.id)
  likes.value = result.likes ?? likes.value
  if (resource.value) {
    resource.value.likes = likes.value
  }
  appStore.showToast('已点赞', '这个资料已加入你的正反馈记录')
}

async function handleFavoriteResource() {
  if (!resource.value) {
    return
  }
  const result = await favoriteResource(resource.value.id)
  favorites.value = result.favorites ?? favorites.value
  if (resource.value) {
    resource.value.favorites = favorites.value
  }
  appStore.showToast('已收藏', '稍后可在个人中心里查看')
}

function downloadResource() {
  if (!resource.value?.previewUrl) {
    appStore.showToast('暂无可下载附件', '当前资料还没有上传附件', 'error')
    return
  }
  window.open(resource.value.previewUrl, '_blank', 'noopener')
}

function getFileExtension(fileType: string) {
  return fileType.trim().toLowerCase().replace(/^\./, '')
}

function resetPreviewState() {
  previewLoading.value = false
  previewMode.value = 'empty'
  previewErrorMessage.value = ''
  officePreviewUrl.value = ''
  if (pdfPreviewUrl.value) {
    URL.revokeObjectURL(pdfPreviewUrl.value)
    pdfPreviewUrl.value = ''
  }
}

async function loadPreview(detail: ResourceItem) {
  if (!detail.previewUrl) {
    previewMode.value = 'empty'
    return
  }

  const extension = getFileExtension(detail.fileType)

  if (extension === 'pdf') {
    previewLoading.value = true
    try {
      const response = await axios.get(detail.previewUrl, {
        responseType: 'blob'
      })
      pdfPreviewUrl.value = URL.createObjectURL(response.data as Blob)
      previewMode.value = 'pdf'
    } catch (error) {
      previewMode.value = 'error'
      previewErrorMessage.value = axios.isAxiosError(error)
        ? `PDF 预览加载失败（HTTP ${error.response?.status ?? 'UNKNOWN'}）`
        : 'PDF 预览加载失败'
    } finally {
      previewLoading.value = false
    }
    return
  }

  if (extension === 'doc' || extension === 'docx') {
    officePreviewUrl.value = `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(new URL(detail.previewUrl, window.location.origin).toString())}`
    previewMode.value = 'office'
    return
  }

  previewMode.value = 'unsupported'
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

onBeforeUnmount(() => {
  resetPreviewState()
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

.preview-panel {
  align-content: start;
}

.panel {
  padding: var(--space-6);
}

.preview-loading,
.preview-frame-wrap {
  min-height: 24rem;
}

.preview-frame {
  width: 100%;
  min-height: 24rem;
  border: none;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.9);
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

@media (max-width: 64rem) {
  .detail-grid,
  .related-grid {
    grid-template-columns: 1fr;
  }
}
</style>
