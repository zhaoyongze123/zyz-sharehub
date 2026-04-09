<template>
  <div class="admin-dashboard-shell">
    <div class="hero">
      <div>
        <h1 class="hero__title">管理中心仪表盘</h1>
        <p class="hero__subtitle">复用真实举报、审计日志和用户列表，汇总当前治理面板的待办与风险。</p>
      </div>
      <div class="hero__meta" data-testid="admin-dashboard-meta">
        <span>{{ dashboardMeta }}</span>
      </div>
    </div>

    <section v-if="loading" class="glass-panel panel panel-state">
      <BaseSkeleton height="18rem" />
    </section>

    <BaseErrorState
      v-else-if="error"
      title="仪表盘加载失败"
      description="暂时无法读取真实治理数据，请稍后重试。"
    />

    <template v-else>
      <div class="stats-grid">
        <article
          v-for="stat in coreStats"
          :key="stat.title"
          class="glass-panel stat-card"
          :data-testid="stat.testId"
        >
          <div class="stat-card__label">{{ stat.title }}</div>
          <div class="stat-card__value">{{ stat.value }}</div>
          <div class="stat-card__detail">{{ stat.detail }}</div>
        </article>
      </div>

      <div class="content-grid">
        <section class="glass-panel panel todo-panel">
          <div class="panel__header">
            <div>
              <h2>关键待办</h2>
              <p>优先展示真实未处理举报，其次回落到最新治理动作。</p>
            </div>
            <RouterLink class="panel__link" to="/admin/reports">查看队列</RouterLink>
          </div>

          <BaseEmpty
            v-if="!pendingTasks.length"
            title="暂无待办"
            description="当前真实举报和治理动作都已处理完。"
          />

          <div v-else class="todo-list">
            <article
              v-for="task in pendingTasks"
              :key="task.id"
              class="todo-item"
              :data-testid="`admin-dashboard-task-${task.id}`"
            >
              <div class="todo-item__type">{{ task.typeLabel }}</div>
              <div class="todo-item__body">
                <div class="todo-item__title">{{ task.title }}</div>
                <p class="todo-item__description">{{ task.description }}</p>
              </div>
              <RouterLink class="todo-item__link" :to="task.link">{{ task.linkLabel }}</RouterLink>
            </article>
          </div>
        </section>

        <section class="glass-panel panel activity-panel">
          <div class="panel__header">
            <div>
              <h2>最新治理动作</h2>
              <p>展示最近写入的真实审计日志。</p>
            </div>
          </div>

          <BaseEmpty
            v-if="!recentActivities.length"
            title="暂无治理动作"
            description="当前审计日志为空。"
          />

          <div v-else class="activity-list">
            <article
              v-for="activity in recentActivities"
              :key="activity.id"
              class="activity-item"
              :data-testid="`admin-dashboard-activity-${activity.id}`"
            >
              <div class="activity-item__title">{{ activity.action }}</div>
              <div class="activity-item__target">{{ activity.target }}</div>
              <div class="activity-item__meta">{{ activity.operatorKey }}</div>
            </article>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  fetchAdminAuditLogs,
  fetchAdminReports,
  fetchAdminUsers,
  type AdminAuditLogItem,
  type AdminReportItem,
  type AdminUserItem
} from '@/api/admin'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'

interface DashboardTask {
  id: string
  typeLabel: string
  title: string
  description: string
  link: string
  linkLabel: string
}

const loading = ref(true)
const error = ref(false)
const reportItems = ref<AdminReportItem[]>([])
const auditItems = ref<AdminAuditLogItem[]>([])
const userItems = ref<AdminUserItem[]>([])

const openReports = computed(() => reportItems.value.filter((item) => item.status === '待处理'))
const bannedUsers = computed(() => userItems.value.filter((item) => item.status === '已封禁'))

const coreStats = computed(() => [
  {
    title: '待处理举报',
    value: String(openReports.value.length),
    detail: `举报总量 ${reportItems.value.length}`,
    testId: 'admin-dashboard-stat-open-reports'
  },
  {
    title: '最近治理动作',
    value: String(auditItems.value.length),
    detail: auditItems.value[0]?.action || '暂无治理动作',
    testId: 'admin-dashboard-stat-audit-actions'
  },
  {
    title: '后台可管用户',
    value: String(userItems.value.length),
    detail: `已封禁 ${bannedUsers.value.length}`,
    testId: 'admin-dashboard-stat-users'
  },
  {
    title: '最新风险目标',
    value: openReports.value[0]?.target || '暂无',
    detail: openReports.value[0]?.reason || '当前没有待处理举报',
    testId: 'admin-dashboard-stat-top-target'
  }
])

const pendingTasks = computed<DashboardTask[]>(() => {
  const reportTasks = openReports.value.slice(0, 3).map((item) => ({
    id: `report-${item.id}`,
    typeLabel: '举报待处理',
    title: item.reason,
    description: `${item.target} · 举报人 ${item.reporter}`,
    link: '/admin/reports',
    linkLabel: '前往处理'
  }))

  if (reportTasks.length > 0) {
    return reportTasks
  }

  return auditItems.value.slice(0, 3).map((item) => ({
    id: `audit-${item.id}`,
    typeLabel: '治理记录',
    title: item.action,
    description: `${item.target} · 操作人 ${item.operatorKey}`,
    link: '/admin/users',
    linkLabel: '查看明细'
  }))
})

const recentActivities = computed(() => auditItems.value.slice(0, 5))

const dashboardMeta = computed(() => {
  if (!auditItems.value.length) {
    return `已加载 ${reportItems.value.length} 条举报 / ${userItems.value.length} 个用户`
  }

  return `最近动作 ${auditItems.value[0].action} · 已加载 ${reportItems.value.length} 条举报`
})

async function loadDashboard() {
  loading.value = true
  error.value = false

  try {
    const [reportsResponse, auditResponse, usersResponse] = await Promise.all([
      fetchAdminReports(1, 20),
      fetchAdminAuditLogs(1, 20),
      fetchAdminUsers(1, 20)
    ])
    reportItems.value = reportsResponse.items
    auditItems.value = auditResponse.items
    userItems.value = usersResponse.items
  } catch (loadError) {
    error.value = true
    reportItems.value = []
    auditItems.value = []
    userItems.value = []
    console.error(loadError)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<style scoped lang="scss">
.admin-dashboard-shell {
  display: grid;
  gap: var(--space-6);
}

.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
}

.hero__title {
  margin: 0;
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-text);
}

.hero__subtitle,
.hero__meta {
  color: var(--color-text-soft);
}

.hero__meta {
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
  font-size: 0.875rem;
}

.stats-grid,
.content-grid {
  display: grid;
  gap: var(--space-4);
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.content-grid {
  grid-template-columns: minmax(0, 1.7fr) minmax(18rem, 1fr);
}

.panel,
.stat-card {
  padding: var(--space-5);
}

.panel-state {
  min-height: 18rem;
}

.stat-card {
  display: grid;
  gap: var(--space-2);
}

.stat-card__label {
  color: var(--color-text-soft);
  font-size: 0.75rem;
}

.stat-card__value {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-text);
  word-break: break-word;
}

.stat-card__detail {
  color: var(--color-text-soft);
  font-size: 0.875rem;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.panel__header h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 1.125rem;
  font-weight: 700;
}

.panel__header p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-soft);
  font-size: 0.875rem;
}

.panel__link,
.todo-item__link {
  color: var(--color-primary);
  font-weight: 600;
}

.todo-list,
.activity-list {
  display: grid;
  gap: var(--space-3);
}

.todo-item,
.activity-item {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
}

.todo-item {
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
}

.todo-item__type {
  padding: var(--space-2) var(--space-3);
  border-radius: 999px;
  background: rgba(29, 111, 220, 0.12);
  color: var(--color-primary);
  font-size: 0.75rem;
  font-weight: 700;
}

.todo-item__title,
.activity-item__title {
  color: var(--color-text);
  font-weight: 700;
}

.todo-item__description,
.activity-item__target,
.activity-item__meta {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.875rem;
}

@media (max-width: 80rem) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 48rem) {
  .hero,
  .todo-item {
    grid-template-columns: 1fr;
    display: grid;
    align-items: start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
