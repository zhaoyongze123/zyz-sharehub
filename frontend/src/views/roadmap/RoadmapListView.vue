<template>
  <div class="page-shell view-grid">
    <section class="section-heading">
      <h1>路线广场</h1>
      <p>把学习拆成阶段节点，再把资源和笔记连接进来。</p>
    </section>

    <div class="toolbar glass-panel">
      <BaseInput v-model="keyword" label="搜索路线" placeholder="例如 Agent / RAG / Prompt" />
      <BaseSelect v-model="selectedTag" label="标签" :options="tagOptions" />
    </div>

    <BaseEmpty v-if="!filteredRoadmaps.length" title="暂无匹配路线" description="可以切换关键词，或先创建你的第一条路线。" />
    <div v-else class="list-grid">
      <RoadmapCard v-for="item in filteredRoadmaps" :key="item.id" :item="item" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import RoadmapCard from '@/components/business/RoadmapCard.vue'
import { roadmapTags, roadmaps } from '@/mock/roadmaps'

const keyword = ref('')
const selectedTag = ref('全部')
const tagOptions = roadmapTags.map((item) => ({ label: item, value: item }))

const filteredRoadmaps = computed(() =>
  roadmaps.filter((item) => {
    const matchKeyword = !keyword.value || `${item.title}${item.summary}`.toLowerCase().includes(keyword.value.toLowerCase())
    const matchTag = selectedTag.value === '全部' || item.tags.includes(selectedTag.value)
    return matchKeyword && matchTag
  })
)
</script>

<style scoped lang="scss">
.view-grid {
  display: grid;
  gap: var(--space-5);
}

.toolbar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  padding: var(--space-5);
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
