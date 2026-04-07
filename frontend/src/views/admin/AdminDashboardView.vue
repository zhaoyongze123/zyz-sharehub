<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>后台首页</h1>
      <p>查看审核、举报和用户增长概览。</p>
    </div>
    <div class="stat-grid">
      <AdminStatCard v-for="item in adminStats" :key="item.label" :label="item.label" :value="item.value" :description="item.description" />
    </div>
  </div>
</template>

<script setup lang="ts">
import AdminStatCard from '@/components/business/AdminStatCard.vue'
import { adminStats } from '@/mock/admin'
</script>

<style scoped lang="scss">
.admin-view {
  display: grid;
  gap: var(--space-6);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
}

@media (max-width: 64rem) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
