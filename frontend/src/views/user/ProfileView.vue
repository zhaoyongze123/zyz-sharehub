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
          <RouterLink
            v-if="authStore.isAdmin"
            class="admin-entry"
            to="/admin"
            data-testid="profile-admin-entry"
          >
            进入管理后台
          </RouterLink>
          <div class="hero-upload-card">
            <p class="upload-title">头像上传</p>
            <BaseUploader
              ref="avatarUploaderRef"
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
          <button
            class="btn-outline"
            type="button"
            :disabled="profileSaving"
            data-testid="profile-edit-toggle"
            @click="editing = !editing"
          >
            {{ editing ? '收起资料编辑' : '编辑个人资料' }}
          </button>
        </div>
      </header>

      <section v-if="editing" class="profile-editor panel" data-testid="profile-editor">
        <div class="panel-heading">
          <h2>编辑个人资料</h2>
          <p>支持保存昵称和个人简介。</p>
        </div>
        <div class="editor-grid">
          <BaseInput
            v-model="profileForm.displayName"
            label="昵称"
            maxlength="32"
            placeholder="输入你的显示昵称"
          />
          <BaseTextarea
            v-model="profileForm.bio"
            label="个人简介"
            maxlength="200"
            placeholder="输入一句简短的个人介绍"
          />
        </div>
        <div class="editor-actions">
          <button class="btn-outline" type="button" :disabled="profileSaving" @click="resetProfileForm">
            重置
          </button>
          <button
            class="btn-primary"
            type="button"
            :disabled="profileSaving || !canSaveProfile"
            data-testid="profile-save"
            @click="saveProfile"
          >
            {{ profileSaving ? '保存中...' : '保存个人资料' }}
          </button>
        </div>
      </section>

      <section class="stat-grid" data-testid="profile-stat-grid">
        <article
          v-for="item in statCards"
          :key="item.label"
          class="stat-card"
          :data-testid="`profile-stat-${item.key}`"
        >
          <p class="stat-label">{{ item.label }}</p>
          <p class="stat-value" :data-testid="`profile-stat-value-${item.key}`">{{ item.value }}</p>
          <p class="stat-desc" :data-testid="`profile-stat-desc-${item.key}`">{{ item.description }}</p>
        </article>
      </section>

      <section class="content-grid">
        <article class="panel">
          <div class="panel-heading">
            <h2>我的资料</h2>
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
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import BaseUploader from '@/components/base/BaseUploader.vue'
import { fetchMeDashboard, type MeDashboardData, updateMyProfile, uploadMyAvatar } from '@/api/me'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const appStore = useAppStore()
const authStore = useAuthStore()
const dashboard = ref<MeDashboardData | null>(null)
const loading = ref(true)
const loadError = ref(false)
const avatarUploading = ref(false)
const profileSaving = ref(false)
const editing = ref(false)
const selectedAvatarFile = ref<File | null>(null)
const avatarUploaderRef = ref<InstanceType<typeof BaseUploader> | null>(null)
const profileForm = ref({
  displayName: '',
  bio: ''
})

const status = computed(() => {
  if (loading.value) return 'loading'
  if (loadError.value || !dashboard.value) return 'error'
  return 'ready'
})

const canSaveProfile = computed(() => {
  if (!dashboard.value) {
    return false
  }
  const normalizedName = profileForm.value.displayName.trim()
  const normalizedBio = profileForm.value.bio.trim()
  return normalizedName.length > 0 && (
    normalizedName !== dashboard.value.profile.displayName ||
    normalizedBio !== dashboard.value.profile.bio
  )
})

const statCards = computed(() => {
  if (!dashboard.value) {
    return []
  }

  return [
    {
      key: 'resources',
      label: '资料工作台',
      value: dashboard.value.stats.resources,
      description: `近 7 天新增 ${dashboard.value.stats.recentResources} 条，已发布 ${dashboard.value.stats.publishedResources} 条`
    },
    {
      key: 'notes',
      label: '笔记广场笔记',
      value: dashboard.value.stats.notes,
      description: `当前草稿 ${dashboard.value.stats.draftNotes} 条`
    },
    {
      key: 'resumes',
      label: '简历记录',
      value: dashboard.value.stats.resumes,
      description: `已生成 ${dashboard.value.stats.generatedResumes} 份`
    },
    {
      key: 'favorites-roadmaps',
      label: '收藏与路线',
      value: `${dashboard.value.stats.favorites} / ${dashboard.value.stats.roadmaps}`,
      description: '左侧为已发布笔记被收藏数，右侧为路线数'
    }
  ]
})

function syncProfileForm() {
  if (!dashboard.value) {
    return
  }
  profileForm.value = {
    displayName: dashboard.value.profile.displayName,
    bio: dashboard.value.profile.bio
  }
}

async function loadDashboard() {
  loading.value = true
  loadError.value = false

  try {
    dashboard.value = await fetchMeDashboard()
    authStore.updateProfile({
      nickname: dashboard.value.profile.displayName,
      headline: dashboard.value.profile.bio || 'ShareHub 用户',
      avatarUrl: dashboard.value.profile.avatarUrl
    })
    syncProfileForm()
  } catch {
    dashboard.value = null
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function resetProfileForm() {
  syncProfileForm()
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

async function saveProfile() {
  const displayName = profileForm.value.displayName.trim()
  const bio = profileForm.value.bio.trim()

  if (!displayName) {
    appStore.showToast('保存失败', '昵称不能为空', 'error')
    return
  }

  profileSaving.value = true

  try {
    await updateMyProfile({ displayName, bio })
    await loadDashboard()
    authStore.updateProfile({
      nickname: displayName,
      headline: bio || 'ShareHub 用户'
    })
    editing.value = false
    appStore.showToast('资料已保存', '个人资料已更新', 'success')
  } finally {
    profileSaving.value = false
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

.admin-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 12px 16px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.24);
  color: white;
  text-decoration: none;
  font-weight: 600;
}

.admin-entry:hover {
  background: rgba(15, 23, 42, 0.28);
}

.profile-editor {
  padding: 24px;
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

.btn-primary {
  border: none;
  background: #2563eb;
  color: white;
  padding: 12px 16px;
  border-radius: 10px;
}

.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.72;
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
  .item-time {
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

.editor-grid {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
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

.item-list {
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
