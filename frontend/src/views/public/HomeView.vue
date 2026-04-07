<template>
  <div class="page-shell home-view">
    <HeroBanner kicker="ShareBase" :title="typedTitle" description="把资料、路线、笔记和简历放进同一套沉淀系统，先完成高保真交互，再逐步联通真实业务接口。">
      <template #actions>
        <BaseButton size="lg" @click="router.push('/login')">用 GitHub 登录</BaseButton>
        <BaseButton size="lg" variant="secondary" @click="router.push('/resources')">浏览资料广场</BaseButton>
      </template>
    </HeroBanner>

    <GlobalSearchBar v-model="keyword" placeholder="搜索资料、路线或笔记，例如 RAG / MCP / 面试" @submit="goSearch" />

    <section class="home-grid">
      <div class="section-heading">
        <h2>推荐资料</h2>
        <p>挑出最容易立刻复用的模板、清单和题集。</p>
      </div>
      <div class="card-grid">
        <ResourceCard v-for="item in resources.slice(0, 3)" :key="item.id" :item="item" />
      </div>
    </section>

    <section class="home-grid">
      <div class="section-heading">
        <h2>推荐学习路线</h2>
        <p>把零散知识拆成真正可执行的阶段任务。</p>
      </div>
      <div class="card-grid card-grid--roadmap">
        <RoadmapCard v-for="item in roadmaps" :key="item.id" :item="item" />
      </div>
    </section>

    <section class="home-grid">
      <div class="section-heading">
        <h2>最新笔记</h2>
        <p>从复盘、实战记录到面试答题框架，沉淀过程也沉淀能力。</p>
      </div>
      <div class="card-grid">
        <NoteCard v-for="item in notes.slice(0, 3)" :key="item.id" :item="item" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import GlobalSearchBar from '@/components/business/GlobalSearchBar.vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import RoadmapCard from '@/components/business/RoadmapCard.vue'
import NoteCard from '@/components/business/NoteCard.vue'
import { resources } from '@/mock/resources'
import { roadmaps } from '@/mock/roadmaps'
import { notes } from '@/mock/notes'

const router = useRouter()
const keyword = ref('')
const fullTitle = '沉淀下一代技术资产'
const index = ref(0)
const typedTitle = computed(() => fullTitle.slice(0, index.value))
let timer = 0

function goSearch() {
  router.push({
    path: '/resources',
    query: keyword.value ? { keyword: keyword.value } : undefined
  })
}

onMounted(() => {
  timer = window.setInterval(() => {
    if (index.value >= fullTitle.length) {
      window.clearInterval(timer)
      return
    }
    index.value += 1
  }, 80)
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
})
</script>

<style scoped lang="scss">
.home-view {
  display: grid;
  gap: var(--space-10);
  padding-top: var(--space-8);
}

.home-grid {
  display: grid;
  gap: var(--space-5);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-5);
}

.card-grid--roadmap {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 64rem) {
  .card-grid,
  .card-grid--roadmap {
    grid-template-columns: 1fr;
  }
}
</style>
