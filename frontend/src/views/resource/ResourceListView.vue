<template>
  <div class="page-shell view-grid">
    <section class="section-heading">
      <h1>资料广场</h1>
      <p>分类、标签、搜索、排序和分页都先在前端假数据上跑通，后续再切接口。</p>
    </section>

    <div class="toolbar glass-panel">
      <BaseInput v-model="keyword" label="搜索关键词" placeholder="例如 RAG / OAuth / 面试" />
      <BaseSelect v-model="category" label="分类" :options="categoryOptions" />
      <BaseSelect v-model="sortBy" label="排序" :options="sortOptions" />
    </div>

    <div class="tag-row">
      <BaseTag v-for="tag in resourceTags" :key="tag" :tone="selectedTag === tag ? 'primary' : 'default'" @click="selectedTag = tag">{{ tag }}</BaseTag>
    </div>

    <BaseErrorState v-if="status === 'error'" />
    <BaseEmpty v-else-if="status === 'empty'" title="没有匹配的资料" description="试试缩短关键词，或者切换分类与标签。" />
    <div v-else-if="status === 'loading'" class="skeleton-grid">
      <BaseSkeleton v-for="item in 6" :key="item" height="16rem" />
    </div>
    <div v-else class="list-grid">
      <ResourceCard v-for="item in pagedResources" :key="item.id" :item="item" />
    </div>

    <BasePagination :page="pageNum" :page-size="pageSize" :total="filteredResources.length" @update:page="pageNum = $event" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BasePagination from '@/components/base/BasePagination.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTag from '@/components/base/BaseTag.vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import { resourceCategories, resources, resourceTags } from '@/mock/resources'

const route = useRoute()
const keyword = ref(String(route.query.keyword || ''))
const category = ref('全部')
const sortBy = ref('latest')
const selectedTag = ref('全部')
const pageNum = ref(1)
const pageSize = 6

const categoryOptions = resourceCategories.map((item) => ({ label: item, value: item }))
const sortOptions = [
  { label: '最新优先', value: 'latest' },
  { label: '最热优先', value: 'hot' }
]

const filteredResources = computed(() => {
  const list = resources.filter((item) => {
    const matchKeyword = !keyword.value || `${item.title}${item.summary}${item.tags.join(' ')}`.toLowerCase().includes(keyword.value.toLowerCase())
    const matchCategory = category.value === '全部' || item.category === category.value
    const matchTag = selectedTag.value === '全部' || item.tags.includes(selectedTag.value)
    return matchKeyword && matchCategory && matchTag
  })

  return [...list].sort((a, b) => {
    if (sortBy.value === 'hot') {
      return b.likes - a.likes
    }
    return b.updatedAt.localeCompare(a.updatedAt)
  })
})

const pagedResources = computed(() => {
  const start = (pageNum.value - 1) * pageSize
  return filteredResources.value.slice(start, start + pageSize)
})

const status = computed(() => {
  if (keyword.value === 'error') return 'error'
  if (keyword.value === 'loading') return 'loading'
  if (!filteredResources.value.length) return 'empty'
  return 'ready'
})

watchEffect(() => {
  if (pageNum.value > Math.ceil(filteredResources.value.length / pageSize) && filteredResources.value.length) {
    pageNum.value = 1
  }
})
</script>

<style scoped lang="scss">
.view-grid {
  display: grid;
  gap: var(--space-5);
}

.toolbar {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

.list-grid,
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

@media (max-width: 64rem) {
  .toolbar,
  .list-grid,
  .skeleton-grid {
    grid-template-columns: 1fr;
  }
}
</style>
