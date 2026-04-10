<template>
  <div v-if="loading" class="page-shell detail-grid">
    <section class="detail-main">
      <BaseSkeleton height="14rem" />
      <BaseSkeleton height="12rem" />
      <BaseSkeleton height="10rem" />
    </section>
    <aside class="detail-side">
      <BaseSkeleton height="10rem" />
    </aside>
  </div>

  <div class="page-shell detail-grid" v-else-if="note">
    <section class="detail-main">
      <HeroBanner kicker="笔记详情" :title="note.title" :description="noteSummary">
        <template #actions>
          <BaseButton disabled>收藏笔记待接接口</BaseButton>
          <BaseButton variant="secondary" disabled>举报待接接口</BaseButton>
        </template>
      </HeroBanner>

      <div class="glass-panel interaction-pending" data-testid="note-detail-interaction-pending">
        <p class="interaction-pending__title">互动能力待接后端</p>
        <p>当前页面已切到真实笔记详情读取，点赞、收藏、举报接口尚未提供，已移除本地模拟成功反馈。</p>
      </div>

      <article class="glass-panel markdown-panel" v-html="renderedHtml"></article>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>相关推荐</h2>
          <p>当前接口尚未返回相关推荐，后续可继续补齐关联笔记能力。</p>
        </div>
        <div v-if="relatedNotes.length" class="related-grid">
          <NoteCard v-for="item in relatedNotes" :key="item.id" :item="item" />
        </div>
        <BaseEmpty
          v-else
          title="暂无相关推荐"
          description="当前仅收口真实详情读取；关联笔记待后端提供独立查询接口后继续补齐。"
        />
      </div>
    </section>

    <aside class="detail-side">
      <NoteOutline :items="noteOutline" />
      <BaseEmpty title="笔记状态" :description="noteStatusDescription" />
    </aside>
  </div>

  <div v-else-if="notFound" class="page-shell">
    <BaseErrorState title="笔记不存在" description="可能已删除、当前账号不可见，或你访问了他人的笔记。">
      <BaseButton @click="router.push('/community')">返回社区</BaseButton>
    </BaseErrorState>
  </div>

  <div v-else class="page-shell">
    <BaseErrorState title="笔记加载失败" description="服务暂时不可用，请稍后刷新重试。" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { fetchNoteDetail, type NoteDTO } from '@/api/notes'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import NoteOutline from '@/components/business/NoteOutline.vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const notFound = ref(false)
const note = ref<NoteDTO | null>(null)

const noteSummary = computed(() => extractSummary(note.value?.contentMd || ''))
const noteOutline = computed(() => extractOutline(note.value?.contentMd || ''))
const noteStatusDescription = computed(() => {
  if (!note.value) return '笔记状态读取中'
  const status = note.value.status?.trim() || '未标记'
  const visibility = note.value.visibility?.trim() || '仅自己可见'
  return `当前状态 ${status}，可见性 ${visibility}`
})
const relatedNotes = computed<
  Array<{ id: number, title: string, summary: string, updatedAt: string, status: string, tags: string[] }>
>(() => [])
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

  try {
    note.value = await fetchNoteDetail(Number(route.params.id))
  } catch (error) {
    const status = axios.isAxiosError(error) ? (error.response?.status ?? 0) : 0
    if (status === 404) {
      notFound.value = true
    }
  } finally {
    loading.value = false
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

.interaction-pending {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-4);
}

.interaction-pending__title {
  margin: 0;
  font-weight: 600;
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
