<template>
  <div v-if="loading" class="detail-shell detail-grid" data-testid="note-detail-loading">
    <section class="detail-main">
      <BaseSkeleton height="18rem" />
      <BaseSkeleton height="16rem" />
    </section>
    <aside class="detail-side">
      <BaseSkeleton height="14rem" />
      <BaseSkeleton height="10rem" />
    </aside>
  </div>

  <div v-else-if="note" class="detail-shell detail-grid" data-testid="note-detail-page">
    <section class="detail-main">
      <header class="detail-hero">
        <div class="hero-kicker-row">
          <BaseTag v-if="note.isOfficial" tone="primary">平台公告</BaseTag>
          <BaseTag v-if="note.isOfficial" tone="accent">官方</BaseTag>
          <BaseTag v-if="note.isPinned" tone="danger">置顶</BaseTag>
          <span class="hero-kicker">{{ noteCategoryLabel }}</span>
        </div>
        <h1 class="hero-title">{{ note.title }}</h1>
        <p class="hero-summary">{{ noteSummary }}</p>

        <div class="hero-meta-row">
          <div class="author-chip">
            <img v-if="note.ownerAvatarUrl" :src="note.ownerAvatarUrl" class="author-avatar" />
            <div v-else class="author-avatar author-avatar--fallback">{{ ownerInitial }}</div>
            <div>
              <div class="author-name-row">
                <strong>{{ note.ownerName || note.ownerKey || '未知作者' }}</strong>
                <span v-if="note.isOfficial" class="author-role">管理员发布</span>
              </div>
              <p>发布时间 {{ noteTimeLabel }}</p>
            </div>
          </div>

          <div class="hero-actions">
            <BaseButton size="sm" @click="handleFavoriteNote">收藏 {{ favorites }}</BaseButton>
            <BaseButton size="sm" variant="secondary" @click="handleLikeNote">点赞 {{ likes }}</BaseButton>
            <BaseButton size="sm" variant="secondary" @click="reportVisible = true">举报</BaseButton>
            <BaseButton v-if="canDelete" size="sm" variant="danger" @click="handleDeleteNote">删除</BaseButton>
          </div>
        </div>
      </header>

      <article class="detail-article markdown-body" data-testid="note-detail-content" v-html="renderedHtml"></article>

      <section class="detail-footer-bar">
        <div class="footer-stat"><span>状态</span><strong>{{ note.status || '未标记' }}</strong></div>
        <div class="footer-stat"><span>可见性</span><strong>{{ note.visibility || '仅作者可见' }}</strong></div>
        <div class="footer-stat"><span>领域</span><strong>{{ noteCategoryLabel }}</strong></div>
      </section>

      <section class="related-section">
        <div class="section-heading">
          <h2>相关推荐</h2>
          <p>基于当前主题和正文语义做相近笔记推荐。</p>
        </div>
        <div v-if="relatedNotes.length" class="related-grid">
          <NoteCard v-for="item in relatedNotes" :key="item.id" :item="item" />
        </div>
        <BaseEmpty v-else title="暂无相关推荐" description="当前只保留真实可访问数据，不做伪造填充。" />
      </section>
    </section>

    <aside class="detail-side">
      <section class="side-card">
        <p class="side-label">阅读导航</p>
        <h3>正文目录</h3>
        <ol class="outline-list">
          <li v-for="(item, index) in noteOutline" :key="`${item}-${index}`">{{ item }}</li>
        </ol>
      </section>

      <section class="side-card side-card--muted">
        <p class="side-label">帖子状态</p>
        <ul class="state-list">
          <li><span>作者</span><strong>{{ note.ownerName || note.ownerKey || '未知作者' }}</strong></li>
          <li><span>领域</span><strong>{{ noteCategoryLabel }}</strong></li>
          <li><span>公告</span><strong>{{ note.isOfficial ? '是' : '否' }}</strong></li>
          <li><span>置顶</span><strong>{{ note.isPinned ? '是' : '否' }}</strong></li>
        </ul>
      </section>
    </aside>

    <ReportDialog
      v-model:reason="reportReason"
      :visible="reportVisible"
      :submitting="reporting"
      :error-message="reportErrorMessage"
      @close="closeReportDialog"
      @submit="submitReport" />
  </div>

  <div v-else-if="notFound" class="detail-shell" data-testid="note-detail-not-found">
    <BaseErrorState title="笔记不存在" description="可能已删除、当前账号不可见，或你访问了他人的笔记。">
      <BaseButton @click="router.push('/community')">返回社区</BaseButton>
    </BaseErrorState>
  </div>

  <div v-else class="detail-shell" data-testid="note-detail-error">
    <BaseErrorState title="笔记加载失败" description="服务暂时不可用，请稍后刷新重试。" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import {
  deleteNote,
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
import BaseTag from '@/components/base/BaseTag.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import ReportDialog from '@/components/business/ReportDialog.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
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
const currentUserKey = computed(() => window.localStorage.getItem('ShareHub.userKey') || '')

const noteSummary = computed(() => extractSummary(note.value?.contentMd || ''))
const noteOutline = computed(() => extractOutline(note.value?.contentMd || ''))
const noteCategoryLabel = computed(() => note.value?.category?.trim() || '所有领域')
const noteTimeLabel = computed(() => {
  const raw = note.value?.createdAt || note.value?.updatedAt
  if (!raw) return '未知时间'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return '未知时间'
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`
})
const ownerInitial = computed(() => (note.value?.ownerName || note.value?.ownerKey || 'U').charAt(0).toUpperCase())
const canDelete = computed(() => {
  if (!note.value) return false
  return authStore.isAdmin || note.value.ownerKey === currentUserKey.value
})
const renderedHtml = computed(() => {
  if (!note.value) return ''
  return DOMPurify.sanitize(marked(note.value.contentMd || '') as string)
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
  if (!note.value || interactionSubmitting.value) return
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
  if (!note.value || interactionSubmitting.value) return
  interactionSubmitting.value = true
  try {
    const result = await favoriteNote(note.value.id)
    favorites.value = result.favorites ?? favorites.value
    appStore.showToast('已收藏', '稍后可在个人中心里查看这篇笔记')
  } finally {
    interactionSubmitting.value = false
  }
}

async function handleDeleteNote() {
  if (!note.value) return
  const confirmed = window.confirm(`确认删除《${note.value.title}》？删除后不可恢复。`)
  if (!confirmed) return
  try {
    await deleteNote(note.value.id)
    appStore.showToast('删除成功', '帖子已从社区移除')
    router.push('/community')
  } catch (error: any) {
    appStore.showToast('删除失败', error?.response?.data?.msg ?? '请稍后再试', 'error')
  }
}

function closeReportDialog() {
  if (reporting.value) return
  reportVisible.value = false
  reportReason.value = ''
  reportErrorMessage.value = ''
}

async function submitReport() {
  if (!note.value || reporting.value) return
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
      reportErrorMessage.value = error instanceof Error && error.message.trim() ? error.message : '举报提交失败，请稍后重试'
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

watch(() => route.params.id, () => { void loadNote() })
onMounted(() => { void loadNote() })
</script>

<style scoped lang="scss">
.detail-shell {
  --paper: #fffdf8;
  --ink: #162033;
  --soft: #667085;
  --line: #e9dfd0;
  --accent: #b45309;
  --accent-soft: #fef3c7;
  display: grid;
  gap: 24px;
}
.detail-grid {
  grid-template-columns: minmax(0, 1.9fr) 320px;
  align-items: start;
}
.detail-main,
.detail-side {
  display: grid;
  gap: 24px;
}
.detail-hero,
.detail-article,
.detail-footer-bar,
.related-section,
.side-card {
  background: var(--paper);
  border: 1px solid var(--line);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
}
.detail-hero {
  padding: 36px;
  background: linear-gradient(135deg, #fff7ed 0%, #fffdf8 55%, #ffffff 100%);
}
.hero-kicker-row,
.hero-meta-row,
.hero-actions,
.author-chip,
.author-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.hero-kicker {
  color: var(--accent);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.hero-title {
  margin: 18px 0 16px;
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.02;
  letter-spacing: -0.04em;
  color: var(--ink);
}
.hero-summary {
  margin: 0;
  max-width: 760px;
  font-size: 17px;
  line-height: 1.8;
  color: var(--soft);
}
.hero-meta-row {
  justify-content: space-between;
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid rgba(180, 83, 9, 0.12);
}
.author-avatar {
  width: 52px;
  height: 52px;
  border-radius: 999px;
  object-fit: cover;
  background: #e2e8f0;
}
.author-avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: linear-gradient(135deg, #f59e0b, #b45309);
  font-weight: 800;
}
.author-chip p {
  margin: 4px 0 0;
  color: var(--soft);
  font-size: 13px;
}
.author-role {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}
.detail-article {
  padding: 40px 36px;
  color: var(--ink);
  line-height: 1.9;
}
.detail-article :deep(h1),
.detail-article :deep(h2),
.detail-article :deep(h3) {
  color: #0f172a;
  line-height: 1.2;
  margin: 1.8em 0 0.7em;
}
.detail-article :deep(h1) { font-size: 2.2rem; }
.detail-article :deep(h2) { font-size: 1.7rem; }
.detail-article :deep(h3) { font-size: 1.3rem; }
.detail-article :deep(p),
.detail-article :deep(li) {
  color: #334155;
  font-size: 1rem;
}
.detail-article :deep(blockquote) {
  margin: 1.5rem 0;
  padding: 1rem 1.2rem;
  border-left: 4px solid #f59e0b;
  background: #fff7ed;
  color: #7c2d12;
}
.detail-article :deep(pre) {
  overflow: auto;
  padding: 1rem;
  border-radius: 16px;
  background: #172033;
  color: #f8fafc;
}
.detail-footer-bar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 24px 28px;
}
.footer-stat span,
.side-label,
.section-heading p {
  color: var(--soft);
}
.footer-stat strong {
  display: block;
  margin-top: 6px;
  color: var(--ink);
  font-size: 18px;
}
.related-section {
  padding: 28px;
}
.section-heading {
  margin-bottom: 18px;
}
.section-heading h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: var(--ink);
}
.related-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.side-card {
  padding: 24px;
}
.side-card--muted {
  background: #fff;
}
.side-card h3 {
  margin: 6px 0 16px;
  font-size: 22px;
  color: var(--ink);
}
.outline-list,
.state-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 12px;
  color: #334155;
}
.state-list {
  padding-left: 0;
  list-style: none;
}
.state-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.state-list span {
  color: var(--soft);
}
@media (max-width: 1100px) {
  .detail-grid,
  .related-grid,
  .detail-footer-bar {
    grid-template-columns: 1fr;
  }
}
</style>
