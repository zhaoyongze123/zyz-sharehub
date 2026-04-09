<template>
  <div class="roadmap-paths">
    <header class="paths-hero">
      <div class="hero-content">
        <h1 class="hero-title">学习路线图</h1>
        <p class="hero-desc">精心设计的系统化成长路径，从零到一掌握核心技术栈。</p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">{{ roadmaps.length }}</span>
            <span class="stat-label">开源路线</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">体系化</span>
            <span class="stat-label">知识结构</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">实战派</span>
            <span class="stat-label">工业级项目</span>
          </div>
        </div>
      </div>
    </header>

    <div class="paths-filter">
      <div class="filter-tabs">
        <button v-for="cat in categoryOptions" :key="cat.value"
                class="tab-btn"
                :class="{ active: category === cat.value }"
                @click="category = cat.value">
          {{ cat.label }}
        </button>
      </div>
      <div class="search-box">
        <div class="i-carbon-search search-icon"></div>
        <input type="text" v-model="keyword" placeholder="搜索阶段或技术..." class="search-input" />
      </div>
    </div>

    <main class="paths-content">
      <BaseErrorState v-if="status === 'error'" />
      <BaseEmpty v-else-if="status === 'empty'" title="没有匹配的路线图" description="尝试调整搜索关键词或分类。" />
      <div v-else-if="status === 'loading'" class="paths-grid">
        <BaseSkeleton v-for="item in 4" :key="item" height="24rem" />
      </div>
      <div v-else class="paths-grid">
        <RoadmapCard v-for="item in filteredRoadmaps" :key="item.id" :item="item" class="roadmap-feature-card" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchRoadmaps, type RoadmapListItem } from '@/api/roadmaps'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import RoadmapCard from '@/components/business/RoadmapCard.vue'
import { useRoadmapStore } from '@/stores/roadmap'

const route = useRoute()
const roadmapStore = useRoadmapStore()

const keyword = ref(String(route.query.keyword ?? roadmapStore.keyword ?? ''))
const category = ref(roadmapStore.selectedTag || '全部')
const roadmaps = ref<RoadmapListItem[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref<string | null>(null)

const categoryOptions = [
  { label: '全部路线', value: '全部' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '私密', value: 'PRIVATE' }
]

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())

const filteredRoadmaps = computed(() => {
  return roadmaps.value.filter((item) => {
    const text = `${item.title ?? ''}${item.summary ?? ''}${item.description ?? ''}`.toLowerCase()
    const matchKeyword = !normalizedKeyword.value || text.includes(normalizedKeyword.value)
    const categoryValue = category.value.toUpperCase()
    const statusMatches = [item.status, item.visibility]
      .filter(Boolean)
      .some((val) => String(val).toUpperCase() === categoryValue)
    const tagMatches = item.tags?.some((tag) => String(tag).toUpperCase() === categoryValue)
    const matchCategory = category.value === '全部' || statusMatches || tagMatches
    return matchKeyword && matchCategory
  })
})

const status = computed(() => {
  if (loading.value) return 'loading'
  if (loadError.value) return 'error'
  if (!filteredRoadmaps.value.length) return 'empty'
  return 'ready'
})

async function loadRoadmaps() {
  loading.value = true
  loadError.value = null
  try {
    const response = await fetchRoadmaps({
      page: roadmapStore.pageNum,
      pageSize: roadmapStore.pageSize,
      status: category.value === '全部' ? undefined : category.value
    })
    roadmaps.value = response.list
    total.value = response.total ?? response.list.length
  } catch (error: any) {
    loadError.value = error?.message ?? '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadRoadmaps)

watch(keyword, (val) => {
  roadmapStore.keyword = val
})

watch(category, () => {
  roadmapStore.selectedTag = category.value
  loadRoadmaps()
})
</script>

<style scoped lang="scss">
.roadmap-paths {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.paths-hero {
  background: white;
  border-radius: 20px;
  padding: 60px 48px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.025);
  border: 1px solid #f3f4f6;
  position: relative;
  overflow: hidden;
}

.paths-hero::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 50%;
  height: 100%;
  background: radial-gradient(circle at top right, rgba(37, 99, 235, 0.05), transparent 70%);
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 16px 0;
  letter-spacing: -0.02em;
}

.hero-desc {
  font-size: 18px;
  color: #4b5563;
  margin: 0 0 40px 0;
  max-width: 600px;
  line-height: 1.6;
}

.hero-stats {
  display: flex;
  gap: 48px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #2563eb;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.paths-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  padding: 12px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  position: sticky;
  top: 80px;
  z-index: 10;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #111827;
  background: #f9fafb;
}

.tab-btn.active {
  background: #111827;
  color: white;
}

.search-box {
  position: relative;
  width: 300px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
}

.search-input {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 0 16px 0 40px;
  font-size: 14px;
  color: #111827;
  outline: none;
  transition: all 0.2s;
  background: #f9fafb;
}

.search-input:focus {
  background: white;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.paths-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 32px;
}

/* Make roadmap cards look more like feature cards in this view */
:deep(.roadmap-feature-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s;
}

:deep(.roadmap-feature-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

@media (max-width: 768px) {
  .paths-hero {
    padding: 40px 24px;
  }
  .paths-filter {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  .filter-tabs {
    flex-wrap: wrap;
  }
  .search-box {
    width: 100%;
  }
}
</style>
