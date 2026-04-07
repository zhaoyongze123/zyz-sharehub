<template>
  <div class="page-shell view-grid">
    <section class="section-heading">
      <h1>笔记广场</h1>
      <p>用卡片预览最新复盘、面试总结和工程记录。</p>
    </section>

    <div class="toolbar glass-panel">
      <BaseInput v-model="keyword" label="搜索笔记" placeholder="例如 面试 / 复盘 / RAG" />
      <BaseTabs v-model="tab" :items="tabItems" />
    </div>

    <div class="tag-row">
      <BaseTag v-for="tag in noteTags" :key="tag" :tone="selectedTag === tag ? 'primary' : 'default'" @click="selectedTag = tag">{{ tag }}</BaseTag>
    </div>

    <BaseEmpty v-if="!filteredNotes.length" title="暂时没有匹配笔记" description="试试切换标签，或者回到个人中心创建草稿。" />
    <div v-else class="list-grid">
      <NoteCard v-for="item in filteredNotes" :key="item.id" :item="item" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseTabs from '@/components/base/BaseTabs.vue'
import BaseTag from '@/components/base/BaseTag.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import { noteTags, notes } from '@/mock/notes'

const keyword = ref('')
const tab = ref('latest')
const selectedTag = ref('全部')

const tabItems = [
  { label: '最新', value: 'latest' },
  { label: '热门', value: 'hot' }
]

const filteredNotes = computed(() =>
  notes
    .filter((item) => {
      const matchKeyword = !keyword.value || `${item.title}${item.summary}`.toLowerCase().includes(keyword.value.toLowerCase())
      const matchTag = selectedTag.value === '全部' || item.tags.includes(selectedTag.value)
      return matchKeyword && matchTag
    })
    .sort((a, b) => (tab.value === 'latest' ? b.updatedAt.localeCompare(a.updatedAt) : b.tags.length - a.tags.length))
)
</script>

<style scoped lang="scss">
.view-grid {
  display: grid;
  gap: var(--space-5);
}

.toolbar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--space-4);
  padding: var(--space-5);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.tag-row :deep(.base-tag) {
  cursor: pointer;
}

.list-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-5);
}

@media (max-width: 64rem) {
  .toolbar,
  .list-grid {
    grid-template-columns: 1fr;
  }
}
</style>
