<template>
  <div class="page-shell" v-if="isLoading">
    <BaseEmpty title="加载中" description="正在拉取笔记详情..." />
  </div>
  <div class="page-shell detail-grid" v-else-if="note">
    <section class="detail-main">
      <HeroBanner kicker="笔记详情" :title="note.title" :description="note.summary || ''">
        <template #actions>
          <BaseButton @click="favoriteNote">收藏笔记</BaseButton>
          <BaseButton variant="secondary" @click="reportVisible = true">举报</BaseButton>
        </template>
      </HeroBanner>

      <InteractionBar :likes="likes" :favorites="favorites" @like="likeNote" @favorite="favoriteNote" @report="reportVisible = true" />

      <article class="glass-panel markdown-panel" v-html="renderedHtml"></article>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>相关推荐</h2>
          <p>继续阅读相关的工程笔记。</p>
        </div>
        <div class="related-grid">
          <NoteCard v-for="item in relatedNotes" :key="item.id" :item="item" />
        </div>
      </div>
    </section>

    <aside class="detail-side">
      <NoteOutline :items="note?.outline || []" />
      <BaseEmpty title="关联资料" description="联调后可展示引用资料与路线的精确关系。" />
    </aside>

    <ReportDialog v-model:reason="reportReason" :visible="reportVisible" @close="reportVisible = false" @submit="submitReport" />
  </div>

  <div v-else-if="loadError" class="page-shell">
    <BaseErrorState title="笔记加载失败" :description="loadError" />
  </div>

  <div v-else class="page-shell">
    <BaseErrorState title="笔记不存在" description="可能已删除或当前账号不可见。" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import InteractionBar from '@/components/business/InteractionBar.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import NoteOutline from '@/components/business/NoteOutline.vue'
import ReportDialog from '@/components/business/ReportDialog.vue'
import { fetchNote, fetchNotes, type NoteItemDto } from '@/api/notes'
import { useAppStore } from '@/stores/app'

type NoteCardItem = {
  id: number
  title: string
  summary: string
  updatedAt: string
  status: string
  tags: string[]
}

const route = useRoute()
const appStore = useAppStore()
const reportVisible = ref(false)
const reportReason = ref('')
const likes = ref(0)
const favorites = ref(0)
const detail = ref<NoteItemDto | null>(null)
const relatedNotes = ref<NoteCardItem[]>([])
const isLoading = ref(false)
const loadError = ref('')

const note = computed(() => {
  if (!detail.value) return null
  return {
    ...detail.value,
    id: Number(detail.value.id),
    title: detail.value.title ?? '',
    summary: detail.value.summary ?? '',
    content: detail.value.content ?? '',
    status: detail.value.status ?? '',
    tags: detail.value.tags ?? [],
    outline: (detail.value as any).outline || [],
    updatedAt: (detail.value as any).updatedAt || (detail.value as any).updated_at || ''
  }
})

const renderedHtml = computed(() => {
  if (!note.value?.content) return ''
  const html = marked(note.value.content || note.value.summary || '')
  return DOMPurify.sanitize(html as string)
})

async function loadDetail() {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await fetchNote(route.params.id as string)
    detail.value = res
    likes.value = (res as any).likes ?? 0
    favorites.value = (res as any).favorites ?? 0
  } catch (err: any) {
    loadError.value = err?.response?.data?.msg || err?.message || '加载失败'
  } finally {
    isLoading.value = false
  }
}

async function loadRelated() {
  try {
    const { list } = await fetchNotes({ page: 1, pageSize: 3 })
    relatedNotes.value = (list || [])
      .filter((item) => String(item.id) !== String(route.params.id))
      .slice(0, 2)
      .map(mapToCardItem)
  } catch (err) {
    console.warn('load related failed', err)
  }
}

function likeNote() {
  likes.value += 1
  appStore.showToast('已点赞', '这篇笔记已加入你的点赞记录')
}

function favoriteNote() {
  favorites.value += 1
  appStore.showToast('已收藏', '可在个人中心继续回看')
}

function submitReport() {
  reportVisible.value = false
  appStore.showToast('举报已提交', reportReason.value || '已进入处理队列')
  reportReason.value = ''
}

onMounted(() => {
  loadDetail()
  loadRelated()
})

function mapToCardItem(item: NoteItemDto): NoteCardItem {
  return {
    id: Number(item.id),
    title: item.title ?? '未命名笔记',
    summary: item.summary ?? '',
    updatedAt: (item as any).updatedAt || (item as any).updated_at || '',
    status: item.status ?? 'DRAFT',
    tags: item.tags ?? []
  }
}
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

.markdown-panel,
.panel {
  padding: var(--space-6);
}

.markdown-panel :deep(h1),
.markdown-panel :deep(h2),
.markdown-panel :deep(p) {
  margin-top: 0;
}

.markdown-panel :deep(p) {
  color: var(--color-text-soft);
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
