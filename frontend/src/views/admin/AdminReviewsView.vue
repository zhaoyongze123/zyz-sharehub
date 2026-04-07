<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>内容审核</h1>
      <p>支持待审列表、详情预览、通过/驳回与原因填写。</p>
    </div>
    <div class="admin-grid">
      <section class="glass-panel panel">
        <BaseTable>
          <thead>
            <tr>
              <th>标题</th>
              <th>类型</th>
              <th>作者</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in reviewItems" :key="item.id" @click="selectedTitle = item.title">
              <td>{{ item.title }}</td>
              <td>{{ item.type }}</td>
              <td>{{ item.author }}</td>
              <td>{{ item.status }}</td>
            </tr>
          </tbody>
        </BaseTable>
      </section>
      <ReviewActionPanel v-model:reason="reason" @approve="approve" @reject="reject" />
    </div>
    <BaseEmpty title="当前预览" :description="selectedTitle || '选择左侧内容查看处理对象'" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import ReviewActionPanel from '@/components/business/ReviewActionPanel.vue'
import { reviewItems } from '@/mock/admin'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const selectedTitle = ref('')
const reason = ref('')

function approve() {
  appStore.showToast('审核已通过', selectedTitle.value || '已处理当前内容')
}

function reject() {
  appStore.showToast('审核已驳回', reason.value || '已记录驳回原因', 'error')
}
</script>

<style scoped lang="scss">
.admin-view {
  display: grid;
  gap: var(--space-6);
}

.admin-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-4);
}

.panel {
  padding: var(--space-4);
}

tbody tr {
  cursor: pointer;
}

@media (max-width: 64rem) {
  .admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
