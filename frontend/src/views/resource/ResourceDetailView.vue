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
          <p>当前先保留页面侧静态评论占位，后续再接评论真接口。</p>
        </div>
        <CommentList :items="resourceComments" />
      </div>
    </section>

    <aside class="detail-side">
      <BaseEmpty title="预览与下载" description="当前为静态预览占位，联调后可接后端预览与文件下载流。" />
      <BaseEmpty title="状态覆盖" description="若访问不存在的资料，请直接改 URL 为不存在 id，会显示 404 态。" />
    </aside>

    <ReportDialog v-model:reason="reportReason" :visible="reportVisible" @close="reportVisible = false" @submit="submitReport" />
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
const loading = ref(true)
const notFound = ref(false)
const resource = ref<ResourceItem | null>(null)
const relatedResources = ref<ResourceItem[]>([])
const likes = ref(0)
const favorites = ref(0)

const resourceComments = [
  { author: 'Mina', createdAt: '2 小时前', content: '资源详情已切到真实接口后，这里继续补评论真数据会更顺。' },
  { author: 'Alex', createdAt: '昨天', content: '相关推荐已经来自后端，评论区下一轮可以继续收口。' }
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
  appStore.showToast('下载任务已启动', '联调后这里会接真实下载流')
}

function submitReport() {
  reportVisible.value = false
  appStore.showToast('举报已提交', reportReason.value || '已进入后台处理队列')
  reportReason.value = ''
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

@media (max-width: 64rem) {
  .detail-grid,
  .related-grid {
    grid-template-columns: 1fr;
  }
}
</style>
