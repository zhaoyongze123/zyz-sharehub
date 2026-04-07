<template>
  <div class="page-shell detail-grid" v-if="note">
    <section class="detail-main">
      <HeroBanner kicker="笔记详情" :title="note.title" :description="note.summary">
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
      <NoteOutline :items="note.outline" />
      <BaseEmpty title="关联资料" description="联调后可展示引用资料与路线的精确关系。" />
    </aside>

    <ReportDialog v-model:reason="reportReason" :visible="reportVisible" @close="reportVisible = false" @submit="submitReport" />
  </div>

  <div v-else class="page-shell">
    <BaseErrorState title="笔记不存在" description="可能已删除或当前账号不可见。" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import InteractionBar from '@/components/business/InteractionBar.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import NoteOutline from '@/components/business/NoteOutline.vue'
import ReportDialog from '@/components/business/ReportDialog.vue'
import { notes } from '@/mock/notes'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()
const reportVisible = ref(false)
const reportReason = ref('')
const likes = ref(86)
const favorites = ref(112)

const note = computed(() => notes.find((item) => String(item.id) === String(route.params.id)))
const relatedNotes = computed(() => notes.filter((item) => item.id !== note.value?.id).slice(0, 2))
const renderedHtml = computed(() => {
  if (!note.value) return ''
  return note.value.content
    .split('\n')
    .map((line) => {
      if (line.startsWith('# ')) return `<h1>${line.slice(2)}</h1>`
      if (line.startsWith('## ')) return `<h2>${line.slice(3)}</h2>`
      if (!line.trim()) return '<br />'
      return `<p>${line}</p>`
    })
    .join('')
})

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
