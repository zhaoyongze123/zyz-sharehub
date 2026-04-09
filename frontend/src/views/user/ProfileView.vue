<template>
  <div class="profile-page">
    <div v-if="status === 'loading'" class="profile-shell">
      <BaseSkeleton height="12rem" />
      <div class="stat-grid">
        <BaseSkeleton v-for="item in 4" :key="item" height="8rem" />
      </div>
      <div class="content-grid">
        <BaseSkeleton height="18rem" />
        <BaseSkeleton height="18rem" />
      </div>
    </div>

    <div v-else-if="status === 'error'" class="profile-shell">
      <BaseErrorState title="个人中心加载失败" description="请确认登录态有效，或稍后刷新重试。" />
    </div>

    <div v-else-if="dashboard" class="profile-shell">
      <header class="hero-card">
        <div class="hero-main">
          <p class="hero-kicker">个人帐户</p>
          <h1>个人资料</h1>
          <p class="hero-desc">当前页面已切到真实 `/api/me` 聚合数据，展示账号身份与工作台统计。</p>
          <div class="identity-row">
            <div class="avatar" data-testid="profile-avatar">
              <img v-if="dashboard.profile.avatarUrl" :src="dashboard.profile.avatarUrl" alt="avatar" />
              <span v-else>{{ dashboard.profile.displayName.slice(0, 1).toUpperCase() }}</span>
            </div>
            <div>
              <div class="identity-name">{{ dashboard.profile.displayName }}</div>
              <div class="identity-meta">
                <span>@{{ dashboard.profile.login }}</span>
                <span>状态 {{ dashboard.profile.status }}</span>
                <span>{{ authStore.isAdmin ? '管理员' : '个人帐户' }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="hero-actions">
          <div class="hero-upload-card">
            <p class="upload-title">头像上传</p>
            <BaseUploader
              ref="avatarUploaderRef"
              hint="选择图片后立即走 `/api/auth/avatar` 真上传，并刷新个人中心头像。"
              :file="selectedAvatarFile"
              accept="image/*"
              :disabled="avatarUploading"
              input-testid="profile-avatar-input"
              @update:file="selectedAvatarFile = $event"
              @select="handleAvatarSelected"
            />
          </div>
          <button
            class="btn-outline"
            type="button"
            :disabled="avatarUploading"
            data-testid="profile-avatar-upload"
            @click="triggerAvatarUpload"
          >
            {{ avatarUploading ? '头像上传中...' : '通过真实接口上传头像' }}
          </button>
          <button class="btn-outline" type="button" disabled>资料编辑待接写接口</button>
        </div>
      </header>

      <section class="stat-grid">
        <article v-for="item in statCards" :key="item.label" class="stat-card">
          <p class="stat-label">{{ item.label }}</p>
          <p class="stat-value">{{ item.value }}</p>
          <p class="stat-desc">{{ item.description }}</p>
        </article>
      </section>

      <section class="content-grid">
        <article class="panel">
          <div class="panel-heading">
            <h2>我的资料</h2>
            <p>最近 5 条资料工作台记录，来自 `/api/me/resources`。</p>
          </div>
          <ul v-if="dashboard.recentResources.length" class="item-list">
            <li v-for="item in dashboard.recentResources" :key="item.id" class="item-row">
              <div>
                <RouterLink :to="`/resources/${item.id}`" class="item-link">{{ item.title }}</RouterLink>
                <p class="item-meta">状态 {{ item.status }} · 可见性 {{ item.visibility }}</p>
              </div>
              <span class="item-time">{{ item.updatedAt }}</span>
            </li>
          </ul>
          <BaseEmpty v-else title="暂无资料" description="当前账号还没有资料工作台记录。" />
        </article>

        <article class="panel">
          <div class="panel-heading">
            <h2>我的笔记</h2>
            <p>最近 5 条笔记工作台记录，来自 `/api/me/notes`。</p>
          </div>
          <ul v-if="dashboard.recentNotes.length" class="item-list">
            <li v-for="item in dashboard.recentNotes" :key="item.id" class="item-row">
              <div>
                <RouterLink :to="`/notes/${item.id}`" class="item-link">{{ item.title }}</RouterLink>
                <p class="item-meta">状态 {{ item.status }}</p>
              </div>
            </li>
          </ul>
          <BaseEmpty v-else title="暂无笔记" description="当前账号还没有笔记记录。" />
        </article>

        <article class="panel panel-wide">
          <div class="panel-heading">
            <h2>我的简历</h2>
            <p>最近 5 条简历记录，来自 `/api/me/resumes`。</p>
          </div>
          <ul v-if="dashboard.recentResumes.length" class="item-list">
            <li v-for="item in dashboard.recentResumes" :key="item.id" class="item-row">
              <div>
                <span class="item-link">{{ item.fileName }}</span>
                <p class="item-meta">模板 {{ item.templateKey }} · 状态 {{ item.status }}</p>
              </div>
              <span class="item-time">{{ item.updatedAt }}</span>
            </li>
          </ul>
          <BaseEmpty v-else title="暂无简历" description="当前账号还没有已生成简历。" />
        </article>

        <article class="panel">
          <div class="panel-heading">
            <h2>当前边界</h2>
            <p>本轮只收口读路径，避免继续保留“模拟保存成功”。</p>
          </div>
          <ul class="status-list">
            <li>已接真实接口：`/api/me`、`/api/me/resources`、`/api/me/notes`、`/api/me/resumes`</li>
            <li>已移除页面内个人资料模拟保存动作，改为只读展示</li>
            <li>头像上传、昵称简介写回仍待对应后端写接口，本页保持禁用提示</li>
          </ul>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseUploader from '@/components/base/BaseUploader.vue'
import { fetchMeDashboard, type MeDashboardData, uploadMyAvatar } from '@/api/me'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const appStore = useAppStore()
const authStore = useAuthStore()
const dashboard = ref<MeDashboardData | null>(null)
const loading = ref(true)
const loadError = ref(false)
const avatarUploading = ref(false)
const selectedAvatarFile = ref<File | null>(null)
const avatarUploaderRef = ref<InstanceType<typeof BaseUploader> | null>(null)

const status = computed(() => {
  if (loading.value) return 'loading'
  if (loadError.value || !dashboard.value) return 'error'
  return 'ready'
})

const statCards = computed(() => {
  if (!dashboard.value) {
    return []
  }

  return [
    {
      label: '资料工作台',
      value: dashboard.value.stats.resources,
      description: `近 7 天新增 ${dashboard.value.stats.recentResources} 条，已发布 ${dashboard.value.stats.publishedResources} 条`
    },
    {
      label: '社区笔记',
      value: dashboard.value.stats.notes,
      description: `当前草稿 ${dashboard.value.stats.draftNotes} 条`
    },
    {
      label: '简历记录',
      value: dashboard.value.stats.resumes,
      description: `已生成 ${dashboard.value.stats.generatedResumes} 份`
    },
    {
      label: '收藏与路线',
      value: `${dashboard.value.stats.favorites} / ${dashboard.value.stats.roadmaps}`,
      description: '左侧为收藏数，右侧为路线数'
    }
  ]
})

async function loadDashboard() {
  loading.value = true
  loadError.value = false

  try {
    dashboard.value = await fetchMeDashboard()
  } catch {
    dashboard.value = null
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function handleAvatarSelected(file: File | null) {
  if (!file) {
    return
  }

  avatarUploading.value = true

  try {
    await uploadMyAvatar(file)
    selectedAvatarFile.value = null
    await loadDashboard()
    if (dashboard.value?.profile.avatarUrl) {
      authStore.updateProfile({
        avatarUrl: dashboard.value.profile.avatarUrl
      })
    }
    appStore.showToast('头像已更新', '个人中心已刷新为最新头像', 'success')
  } finally {
    avatarUploading.value = false
    selectedAvatarFile.value = null
  }
}

function triggerAvatarUpload() {
  avatarUploaderRef.value?.openFileDialog()
}

onMounted(() => {
  void loadDashboard()
})
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
}

.profile-shell {
  display: grid;
  gap: 24px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 32px;
  border-radius: 20px;
  background: linear-gradient(135deg, #0f172a, #1d4ed8);
  color: white;
}

.hero-upload-card {
  display: grid;
  gap: 8px;
}

.upload-title {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-kicker {
  margin: 0 0 8px;
  color: rgba(255, 255, 255, 0.72);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
}

.hero-card h1 {
  margin: 0;
  font-size: 36px;
}

.hero-desc {
  margin: 12px 0 0;
  max-width: 42rem;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.6;
}

.identity-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.16);
  font-size: 28px;
  font-weight: 700;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.identity-name {
  font-size: 24px;
  font-weight: 700;
}

.identity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-actions {
  width: min(22rem, 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 12px;
}

.btn-outline {
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.08);
  color: white;
  padding: 12px 16px;
  border-radius: 10px;
}

.btn-outline:disabled {
  cursor: not-allowed;
  opacity: 0.78;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card,
.panel {
  border-radius: 16px;
  background: white;
  border: 1px solid #e5e7eb;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.stat-card {
  padding: 20px;
}

.stat-label,
.item-meta,
.panel-heading p,
.stat-desc,
.item-time,
.status-list {
  color: #6b7280;
}

.stat-label {
  margin: 0;
  font-size: 14px;
}

.stat-value {
  margin: 10px 0 6px;
  font-size: 32px;
  font-weight: 800;
  color: #111827;
}

.stat-desc {
  margin: 0;
  line-height: 1.5;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel {
  padding: 24px;
}

.panel-wide {
  grid-column: 1 / -1;
}

.panel-heading h2 {
  margin: 0;
  font-size: 22px;
}

.panel-heading p {
  margin: 8px 0 0;
}

.item-list,
.status-list {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.item-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f3f4f6;
}

.item-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-link {
  color: #111827;
  font-weight: 600;
  text-decoration: none;
}

.item-link:hover {
  color: #2563eb;
}

.item-meta {
  margin: 6px 0 0;
}

.item-time {
  flex-shrink: 0;
}

.status-list {
  display: grid;
  gap: 10px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .hero-card,
  .content-grid,
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
  }

  .item-row {
    flex-direction: column;
  }
}
</style>
