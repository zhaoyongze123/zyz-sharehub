<template>
  <div class="resource-marketplace">
    <header class="marketplace-header">
      <div class="header-text">
        <h1 class="page-title">资料广场</h1>
        <p class="page-desc">从架构设计到核心源码，发现并复用高质量的技术资产。</p>
      </div>
      <div class="search-wrap">
        <div class="i-carbon-search search-icon"></div>
        <input type="text" v-model="keyword" placeholder="搜索资源（例如：RAG, Spring Boot）..." class="search-input" />
      </div>
    </header>

    <div class="marketplace-body">
      <!-- Left Filter Sidebar -->
      <aside class="filter-sidebar">
        <div class="filter-group">
          <h3 class="filter-title">类别</h3>
          <ul class="filter-list">
            <li v-for="cat in categoryOptions" :key="cat.value" 
                class="filter-item"
                :class="{ active: category === cat.value }"
                @click="category = cat.value">
              {{ cat.label }}
            </li>
          </ul>
        </div>
        
        <div class="filter-group">
          <h3 class="filter-title">热门标签</h3>
          <div class="tag-cloud">
            <span v-for="tag in resourceTags" :key="tag" 
                  class="tag-pill" 
                  :class="{ active: selectedTag === tag }"
                  @click="selectedTag = tag">
              {{ tag }}
            </span>
          </div>
        </div>
      </aside>

      <!-- Right Content Area -->
      <main class="results-area">
        <div class="results-header">
          <span class="results-count">找到 {{ filteredResources.length }} 个资源</span>
          <div class="sort-control">
            <span class="sort-label">排序：</span>
            <select v-model="sortBy" class="sort-select">
              <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
        </div>

        <BaseErrorState v-if="status === 'error'" />
        <BaseEmpty v-else-if="status === 'empty'" title="没有匹配的资料" description="试试缩短关键词，或者切换分类与标签。" />
        <div v-else-if="status === 'loading'" class="resource-grid">
          <BaseSkeleton v-for="item in 6" :key="item" height="16rem" />
        </div>
        <div v-else class="resource-grid">
          <ResourceCard v-for="item in pagedResources" :key="item.id" :item="item" />
        </div>

        <div class="pagination-wrap" v-if="filteredResources.length > pageSize">
          <BasePagination :page="pageNum" :page-size="pageSize" :total="filteredResources.length" @update:page="pageNum = $event" />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BasePagination from '@/components/base/BasePagination.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import { resourceCategories, resources, resourceTags } from '@/mock/resources'

const route = useRoute()
const keyword = ref(String(route.query.keyword || ''))
const category = ref('全部')
const sortBy = ref('latest')
const selectedTag = ref('全部')
const pageNum = ref(1)
const pageSize = 9

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
.resource-marketplace {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.marketplace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #111827 0%, #374151 100%);
  border-radius: 16px;
  padding: 40px;
  color: white;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

.header-text {
  max-width: 50%;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px 0;
  letter-spacing: -0.02em;
}

.page-desc {
  font-size: 15px;
  color: #d1d5db;
  margin: 0;
  line-height: 1.5;
}

.search-wrap {
  position: relative;
  width: 360px;
}

.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  font-size: 18px;
}

.search-input {
  width: 100%;
  height: 52px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  padding: 0 20px 0 44px;
  color: white;
  font-size: 15px;
  outline: none;
  transition: all 0.2s;
}

.search-input::placeholder {
  color: #9ca3af;
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.05);
}

.marketplace-body {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.filter-sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-title {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0;
}

.filter-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.filter-item:hover {
  background: #f3f4f6;
  color: #111827;
}

.filter-item.active {
  background: #111827;
  color: white;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-pill {
  padding: 6px 12px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.tag-pill:hover {
  background: #e5e7eb;
}

.tag-pill.active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}

.results-area {
  flex: 1;
  min-width: 0;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.results-count {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 13px;
  color: #9ca3af;
}

.sort-select {
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  cursor: pointer;
  outline: none;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.pagination-wrap {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .marketplace-header {
    flex-direction: column;
    align-items: stretch;
    padding: 24px;
    gap: 20px;
  }
  .header-text {
    max-width: 100%;
  }
  .search-wrap {
    width: 100%;
  }
  .marketplace-body {
    flex-direction: column;
  }
  .filter-sidebar {
    width: 100%;
    gap: 20px;
  }
  .filter-list {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
