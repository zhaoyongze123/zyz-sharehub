<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>审计日志</h1>
      <p>查看后台真实治理动作、操作人和目标对象。</p>
    </div>

    <section v-if="loading" class="glass-panel panel panel-state">
      <BaseSkeleton height="12rem" />
    </section>

    <BaseErrorState
      v-else-if="error"
      title="审计日志加载失败"
      description="暂时无法读取真实审计日志，请稍后重试。"
    />

    <section v-else-if="auditItems.length" class="glass-panel panel">
      <BaseTable>
        <thead>
          <tr>
            <th>治理动作</th>
            <th>关联目标</th>
            <th>操作人</th>
            <th>记录时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in auditItems"
            :key="item.id"
            :data-testid="`admin-audit-row-${item.id}`"
          >
            <td>{{ item.action }}</td>
            <td>{{ item.target }}</td>
            <td>{{ item.operatorKey }}</td>
            <td>{{ item.createdAt }}</td>
          </tr>
        </tbody>
      </BaseTable>
    </section>

    <BaseEmpty
      v-else
      title="暂无审计日志"
      description="当前真实审计日志为空。"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAdminAuditLogs, type AdminAuditLogItem } from '@/api/admin'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTable from '@/components/base/BaseTable.vue'

const loading = ref(true)
const error = ref(false)
const auditItems = ref<AdminAuditLogItem[]>([])

async function loadAuditLogs() {
  loading.value = true
  error.value = false

  try {
    const response = await fetchAdminAuditLogs(1, 20)
    auditItems.value = response.items
  } catch (loadError) {
    error.value = true
    auditItems.value = []
    console.error(loadError)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadAuditLogs()
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
