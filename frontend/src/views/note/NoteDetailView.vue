<template>
  <div v-if="loading" class="page-shell detail-grid" data-testid="note-detail-loading">
    <section class="detail-main">
      <BaseSkeleton height="14rem" />
      <BaseSkeleton height="12rem" />
      <BaseSkeleton height="10rem" />
    </section>
    <aside class="detail-side">
      <BaseSkeleton height="10rem" />
    </aside>
  </div>

  <div class="page-shell detail-grid" v-else-if="note" data-testid="note-detail-page">
    <section class="detail-main" data-testid="note-detail-main">
      <HeroBanner kicker="笔记详情" :title="note.title" :description="noteSummary">
        <template #actions>
          <BaseButton @click="handleFavoriteNote">收藏</BaseButton>
          <BaseButton variant="secondary" @click="reportVisible = true">举报</BaseButton>
        </template>
      </HeroBanner>

      <InteractionBar
        :likes="likes"
        :favorites="favorites"
        :disable-like="interactionSubmitting"
        :disable-favorite="interactionSubmitting"
        @report="reportVisible = true"
        @like="handleLikeNote"
        @favorite="handleFavoriteNote"
      />

      <article class="glass-panel markdown-panel" data-testid="note-detail-content" v-html="renderedHtml"></article>

      <div class="glass-panel panel" data-testid="note-detail-related">
        <div class="section-heading">
          <h2>相关推荐</h2>
        </div>
        <div v-if="relatedNotes.length" class="related-grid">
          <NoteCard v-for="item in relatedNotes" :key="item.id" :item="item" />
        </div>
        <BaseEmpty v-else title="暂无相关推荐" description="" />
      </div>
    </section>

    <aside class="detail-side" data-testid="note-detail-side">
      <NoteOutline :items="noteOutline" />
      <BaseEmpty title="笔记状态" :description="noteStatusDescription" />
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

  <div v-else-if="notFound" class="page-shell" data-testid="note-detail-not-found">
    <BaseErrorState title="笔记不存在" description="可能已删除、当前账号不可见，或你访问了他人的笔记。">
      <BaseButton @click="router.push('/community')">返回社区</BaseButton>
    </BaseErrorState>
  </div>

  <div v-else class="page-shell" data-testid="note-detail-error">
    <BaseErrorState title="笔记加载失败" description="服务暂时不可用，请稍后刷新重试。" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import {
  favoriteNote,
  fetchNoteDetail,
  fetchNoteInteractions,
  fetchRelatedNotes,
  likeNote,
  type NoteDTO,
  type RelatedNoteItem
} from '@/api/notes'
import { createReport } from '@/api/reports'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import InteractionBar from '@/components/business/InteractionBar.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import NoteOutline from '@/components/business/NoteOutline.vue'
import ReportDialog from '@/components/business/ReportDialog.vue'
import { useAppStore } from '@/stores/app'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const reportVisible = ref(false)
const reportReason = ref('')
const reporting = ref(false)
const interactionSubmitting = ref(false)
const reportErrorMessage = ref('')
const loading = ref(true)
const notFound = ref(false)
const note = ref<NoteDTO | null>(null)
const likes = ref(0)
const favorites = ref(0)
const relatedNotes = ref<RelatedNoteItem[]>([])

const noteSummary = computed(() => extractSummary(note.value?.contentMd || ''))
const noteOutline = computed(() => extractOutline(note.value?.contentMd || ''))
const noteStatusDescription = computed(() => {
  if (!note.value) return '笔记状态读取中'
  const status = note.value.status?.trim() || '未标记'
  const visibility = note.value.visibility?.trim() || '仅自己可见'
  return `当前状态 ${status}，可见性 ${visibility}`
})
const renderedHtml = computed(() => {
  if (!note.value) return ''
  return note.value.contentMd
    .split('\n')
    .map((line) => {
      const safeLine = escapeHtml(line)
      if (line.startsWith('# ')) return `<h1>${escapeHtml(line.slice(2))}</h1>`
      if (line.startsWith('## ')) return `<h2>${escapeHtml(line.slice(3))}</h2>`
      if (!line.trim()) return '<br />'
      return `<p>${safeLine}</p>`
    })
    .join('')
})

async function loadNote() {
  loading.value = true
  notFound.value = false
  note.value = null
  relatedNotes.value = []
  likes.value = 0
  favorites.value = 0

  try {
    const noteId = Number(route.params.id)
    const [detail, interactions] = await Promise.all([
      fetchNoteDetail(noteId),
      fetchNoteInteractions(noteId)
    ])
    note.value = detail
    likes.value = interactions.likes
    favorites.value = interactions.favorites
    try {
      relatedNotes.value = await fetchRelatedNotes(noteId)
    } catch (error) {
      relatedNotes.value = []
      if (!axios.isAxiosError(error) || error.response?.status !== 404) {
        appStore.showToast('相关推荐加载失败', '已降级为空列表，不影响笔记详情查看', 'error')
      }
    }
  } catch (error) {
    const status = axios.isAxiosError(error) ? (error.response?.status ?? 0) : 0
    if (status === 404) {
      notFound.value = true
    }
  } finally {
    loading.value = false
  }
}

async function handleLikeNote() {
  if (!note.value || interactionSubmitting.value) {
    return
  }
  interactionSubmitting.value = true
  try {
    const result = await likeNote(note.value.id)
    likes.value = result.likes ?? likes.value
    appStore.showToast('已点赞', '这篇笔记已加入你的正反馈记录')
  } finally {
    interactionSubmitting.value = false
  }
}

async function handleFavoriteNote() {
  if (!note.value || interactionSubmitting.value) {
    return
  }
  interactionSubmitting.value = true
  try {
    const result = await favoriteNote(note.value.id)
    favorites.value = result.favorites ?? favorites.value
    appStore.showToast('已收藏', '稍后可在个人中心里查看这篇笔记')
  } finally {
    interactionSubmitting.value = false
  }
}

function closeReportDialog() {
  if (reporting.value) {
    return
  }
  reportVisible.value = false
  reportReason.value = ''
  reportErrorMessage.value = ''
}

async function submitReport() {
  if (!note.value || reporting.value) {
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
      targetType: 'NOTE',
      noteId: note.value.id,
      reason: reportReason.value.trim()
    })
    appStore.showToast('举报已提交', `举报 #${report.id} 已进入后台处理队列`)
    closeReportDialog()
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

function extractSummary(content: string) {
  const firstParagraph = content
    .split('\n')
    .map((line) => line.trim())
    .find((line) => line && !line.startsWith('#'))
  return firstParagraph || '当前笔记暂无摘要，已直接展示真实正文。'
}

function extractOutline(content: string) {
  const headings = content
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.startsWith('## '))
    .map((line) => line.slice(3))
  return headings.length ? headings : ['正文']
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

watch(() => route.params.id, () => {
  void loadNote()
})

onMounted(() => {
  void loadNote()
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
