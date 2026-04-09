<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>内容审核</h1>
      <p>当前复用真实举报队列完成治理闭环；仅开放已接通的“完成处理”动作。</p>
    </div>

    <section v-if="loading" class="glass-panel panel panel-state">
      <BaseSkeleton height="12rem" />
    </section>

    <BaseErrorState
      v-else-if="error"
      title="审核队列加载失败"
      description="暂时无法读取真实举报队列，请稍后重试。"
    />

    <div v-else-if="reviewItems.length" class="admin-grid">
      <section class="glass-panel panel">
        <BaseTable>
          <thead>
            <tr>
              <th>关联内容</th>
              <th>举报原因</th>
              <th>举报人</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in reviewItems"
              :key="item.id"
              :class="{ 'is-active': item.id === selectedItem?.id }"
              :data-testid="`admin-reviews-row-${item.id}`"
              @click="selectedItemId = item.id"
            >
              <td>{{ item.target }}</td>
              <td>{{ item.reason }}</td>
              <td>{{ item.reporter }}</td>
              <td>{{ item.status }}</td>
            </tr>
          </tbody>
        </BaseTable>
      </section>
      <ReviewActionPanel
        v-model:reason="reason"
        approve-label="完成处理"
        reject-label="驳回未开放"
        :approve-disabled="!selectedItem || resolving || selectedItem.rawStatus !== 'OPEN'"
        :reject-disabled="true"
        @approve="approve"
      />
    </div>

    <BaseEmpty
      v-else
      title="暂无审核数据"
      description="当前真实举报队列为空。"
    />

    <BaseEmpty
      title="当前预览"
      :description="previewDescription"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchAdminReports, resolveAdminReport, type AdminReportItem } from '@/api/admin'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import ReviewActionPanel from '@/components/business/ReviewActionPanel.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(true)
const error = ref(false)
const resolving = ref(false)
const reason = ref('')
const reviewItems = ref<AdminReportItem[]>([])
const selectedItemId = ref<number | null>(null)

const selectedItem = computed(() => reviewItems.value.find((item) => item.id === selectedItemId.value) ?? null)
const previewDescription = computed(() => {
  if (!selectedItem.value) {
    return '选择左侧真实举报记录查看处理对象'
  }

  return `${selectedItem.value.target} · ${selectedItem.value.reason} · ${selectedItem.value.reporter}`
})

async function loadReviews() {
  loading.value = true
  error.value = false

  try {
    const response = await fetchAdminReports(1, 20)
    reviewItems.value = response.items
    selectedItemId.value = response.items[0]?.id ?? null
  } catch (loadError) {
    error.value = true
    reviewItems.value = []
    selectedItemId.value = null
    console.error(loadError)
  } finally {
    loading.value = false
  }
}

async function approve() {
  if (!selectedItem.value || selectedItem.value.rawStatus !== 'OPEN') {
    return
  }

  resolving.value = true

  try {
    const resolvedItem = await resolveAdminReport(selectedItem.value.id)
    reviewItems.value = reviewItems.value.map((item) => (item.id === resolvedItem.id ? resolvedItem : item))
    reason.value = ''
    appStore.showToast('举报已处理', `${resolvedItem.target} 已同步更新为已处理`)
  } catch (resolveError) {
    console.error(resolveError)
  } finally {
    resolving.value = false
  }
}

onMounted(() => {
  void loadReviews()
})
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

.panel-state {
  min-height: 14rem;
}

tbody tr {
  cursor: pointer;
}

tbody tr.is-active {
  background: rgba(11, 120, 163, 0.08);
}

@media (max-width: 64rem) {
  .admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
