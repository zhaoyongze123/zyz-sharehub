<template>
  <div class="page-shell detail-grid" v-if="roadmap">
    <section class="detail-main">
      <HeroBanner kicker="路线详情" :title="roadmap.title" :description="roadmap.summary">
        <template #actions>
          <BaseButton @click="markComplete">标记本阶段完成</BaseButton>
          <BaseButton variant="secondary" @click="favoriteRoadmap">收藏路线</BaseButton>
        </template>
      </HeroBanner>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>节点进度结构</h2>
          <p>采用时间线结构表达阶段与任务，贴近实施文档里的路线详情要求。</p>
        </div>
        <RoadmapTimeline :items="roadmapTimeline" />
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>关联资料</h2>
          <p>路线中的关键节点可以直接跳到相关资料或笔记。</p>
        </div>
        <div class="related-grid">
          <ResourceCard v-for="item in resources.slice(0, 2)" :key="item.id" :item="item" />
        </div>
      </div>

      <div class="glass-panel panel">
        <div class="section-heading">
          <h2>评论互动</h2>
          <p>路线讨论和问题记录统一沉淀。</p>
        </div>
        <CommentList :items="resourceComments" />
      </div>
    </section>

    <aside class="detail-side">
      <BaseEmpty title="阶段进度" :description="`当前模拟进度 ${progress}%`" />
      <BaseEmpty title="联动" description="后续可接已完成节点、路线收藏和评论接口。" />
    </aside>
  </div>

  <div v-else class="page-shell">
    <BaseErrorState title="路线不存在" description="请返回路线广场重新选择。" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import CommentList from '@/components/business/CommentList.vue'
import HeroBanner from '@/components/business/HeroBanner.vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import RoadmapTimeline from '@/components/business/RoadmapTimeline.vue'
import { resourceComments, resources } from '@/mock/resources'
import { roadmapTimeline, roadmaps } from '@/mock/roadmaps'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()
const roadmap = computed(() => roadmaps.find((item) => String(item.id) === String(route.params.id)))
const progress = ref(46)

function markComplete() {
  progress.value = Math.min(100, progress.value + 18)
  appStore.showToast('进度已更新', `当前路线完成度 ${progress.value}%`)
}

function favoriteRoadmap() {
  appStore.showToast('已收藏路线', '后续可在个人中心继续学习')
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

.panel {
  padding: var(--space-6);
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
