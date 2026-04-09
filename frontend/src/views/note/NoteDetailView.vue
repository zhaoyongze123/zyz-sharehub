<template>
  <div class="page-shell" v-if="loading">
    <BaseEmpty title="笔记加载中" description="正在从云端获取内容，请稍候" />
  </div>

  <div class="page-shell detail-grid" v-else-if="note">
    <section class="detail-main">
      <HeroBanner kicker="笔记详情" :title="noteTitle" :description="noteSummary">
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
        <div v-if="relatedNoteCards.length" class="related-grid">
          <NoteCard v-for="item in relatedNoteCards" :key="item.id" :item="item" />
        </div>
        <BaseEmpty v-else title="暂无更多笔记" description="发布更多内容后会在此展示推荐" />
      </div>
    </section>

    <aside class="detail-side">
      <NoteOutline :items="outlineItems" />
      <BaseEmpty title="关联资料" description="联调后可展示引用资料与路线的精确关系。" />
    </aside>

    <ReportDialog v-model:reason="reportReason" :visible="reportVisible" @close="reportVisible = false" @submit="submitReport" />
  </div>

  <div v-else class="page-shell">
    <BaseErrorState title="笔记不存在" :description="errorMessage || '可能已删除或当前账号不可见。'" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import InteractionBar from '@/components/business/InteractionBar.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import NoteOutline from '@/components/business/NoteOutline.vue'
import ReportDialog from '@/components/business/ReportDialog.vue'
import { useAppStore } from '@/stores/app'
import { fetchNoteDetail, fetchNotes, type NoteDTO } from '@/api/notes'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const route = useRoute()
const appStore = useAppStore()
const reportVisible = ref(false)
const reportReason = ref('')
const likes = ref(86)
const favorites = ref(112)

const note = ref<NoteDTO | null>(null)
const relatedNotes = ref<NoteDTO[]>([])
const loading = ref(false)
const errorMessage = ref('')

const noteTitle = computed(() => note.value?.title || '未命名笔记')
const noteSummary = computed(() => {
  if (!note.value?.contentMd) return ''
  return note.value.contentMd.replace(/\s+/g, ' ').slice(0, 120)
})

const renderedHtml = computed(() => {
  if (!note.value?.contentMd) return ''
  return DOMPurify.sanitize(marked.parse(note.value.contentMd) as string)
})

const outlineItems = computed(() => {
  if (!note.value?.contentMd) return []
  return note.value.contentMd
    .split('\n')
    .filter((line) => /^#{1,6}\s+/.test(line))
    .map((line) => line.replace(/^#{1,6}\s+/, '').trim())
})

const relatedNoteCards = computed(() =>
  relatedNotes.value.map((item) => ({
    id: item.id,
    title: item.title || '未命名笔记',
    summary: (item.contentMd || '').replace(/\s+/g, ' ').slice(0, 80) || '暂无摘要',
    updatedAt: '',
    status: item.status || 'DRAFT',
    tags: [] as string[]
  }))
)

async function loadDetail(id: number) {
  if (!id) return
  loading.value = true
  errorMessage.value = ''
  try {
    note.value = await fetchNoteDetail(id)
    await loadRelated(id)
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.msg || '笔记加载失败'
    note.value = null
  } finally {
    loading.value = false
  }
}

async function loadRelated(currentId: number) {
  try {
    const data = await fetchNotes({ page: 1, pageSize: 4, status: 'PUBLISHED' })
    relatedNotes.value = (data.items || data.list || []).filter((item) => item.id !== currentId).slice(0, 2)
  } catch (e) {
    relatedNotes.value = []
  }
}

onMounted(() => {
  const id = Number(route.params.id)
  void loadDetail(id)
})

watch(
  () => route.params.id,
  (newId) => {
    const idNum = Number(newId)
    void loadDetail(idNum)
  }
)

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
