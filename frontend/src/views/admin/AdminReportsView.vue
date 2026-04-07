<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>举报处理</h1>
      <p>查看举报类型、关联内容和处理进度。</p>
    </div>
    <section class="glass-panel panel">
      <BaseTable>
        <thead>
          <tr>
            <th>举报原因</th>
            <th>关联内容</th>
            <th>举报人</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in reportItems" :key="item.id">
            <td>{{ item.reason }}</td>
            <td>{{ item.target }}</td>
            <td>{{ item.reporter }}</td>
            <td>{{ item.status }}</td>
          </tr>
        </tbody>
      </BaseTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import BaseTable from '@/components/base/BaseTable.vue'
import { reportItems } from '@/mock/admin'
</script>

<style scoped lang="scss">
.admin-view {
  display: grid;
  gap: var(--space-6);
}

.panel {
  padding: var(--space-4);
}
</style>
