<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>举报处理</h1>
      <p>查看举报类型、关联内容和处理进度。</p>
    </div>
    <section v-if="loading" class="glass-panel panel panel-state">
      <BaseSkeleton height="12rem" />
    </section>

    <BaseErrorState
      v-else-if="error"
      title="举报列表加载失败"
      description="暂时无法读取真实举报队列，请稍后重试。"
    />

    <section v-else-if="reportItems.length" class="glass-panel panel">
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

    <BaseEmpty
      v-else
      title="暂无举报数据"
      description="当前真实举报队列为空。"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAdminReports, type AdminReportItem } from '@/api/admin'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTable from '@/components/base/BaseTable.vue'

const loading = ref(true)
const error = ref(false)
const reportItems = ref<AdminReportItem[]>([])

async function loadReports() {
  loading.value = true
  error.value = false

  try {
    const response = await fetchAdminReports(1, 20)
    reportItems.value = response.items
  } catch (loadError) {
    error.value = true
    reportItems.value = []
    console.error(loadError)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadReports()
})
</script>

<style scoped lang="scss">
.admin-view {
  display: grid;
  gap: var(--space-6);
}

.panel {
  padding: var(--space-4);
}

.panel-state {
  min-height: 14rem;
}
</style>
