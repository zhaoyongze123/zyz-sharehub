<template>
  <div class="profile-page">
    <div v-if="status === 'loading'" class="profile-shell">
      <BaseSkeleton height="10rem" />
      <div class="workbench-grid">
        <BaseSkeleton height="24rem" />
        <div class="right-col">
          <BaseSkeleton height="9rem" />
          <BaseSkeleton height="9rem" />
          <BaseSkeleton height="9rem" />
        </div>
      </div>
    </div>

    <div v-else-if="status === 'error'" class="profile-shell">
      <BaseErrorState title="个人中心加载失败" description="请确认登录态有效，或稍后刷新重试。" />
    </div>

    <div v-else class="profile-shell">
      <header class="settings-header">
        <div>
          <p class="kicker">Account Settings</p>
          <h1 class="page-title">个人设置</h1>
        </div>
        <div class="header-chip">
          <span>{{ authStore.isAdmin ? '管理员账户' : '个人账户' }}</span>
          <span class="chip-dot" />
          <span>{{ viewProfile.status }}</span>
        </div>
      </header>

      <section class="settings-layout">
        <aside class="settings-nav">
          <p class="nav-title">设置分组</p>
          <ul>
            <li v-for="group in settingGroups" :key="group.id">
              <button
                type="button"
                class="nav-link"
                :class="{ active: activeGroup === group.id }"
                @click="scrollToGroup(group.id)"
              >
                {{ group.label }}
              </button>
            </li>
          </ul>
        </aside>

        <div class="settings-main">
          <article class="panel">
            <div :id="groupIds.account" class="group-anchor" />
            <div class="section-head">
              <h2>账户信息</h2>
            </div>
            <div class="identity-row">
              <div class="avatar" data-testid="profile-avatar">
                <img v-if="viewProfile.avatarUrl" :src="viewProfile.avatarUrl" alt="avatar" />
                <span v-else>{{ viewProfile.displayName.slice(0, 1).toUpperCase() }}</span>
              </div>
              <div class="identity-text">
                <strong>{{ viewProfile.displayName }}</strong>
                <span>@{{ viewProfile.login }}</span>
                <span>{{ authStore.isAdmin ? '管理员' : '个人帐户' }} · 状态 {{ viewProfile.status }}</span>
              </div>
            </div>
          </article>

          <article class="panel">
            <div :id="groupIds.profile" class="group-anchor" />
            <div class="section-head">
              <h2>基础资料</h2>
            </div>
            <div v-if="feedback.profile.level !== 'idle'" class="feedback-bar" :class="feedbackClass('profile')">{{ feedbackText('profile') }}</div>
            <label class="field">
              <span class="field-label">显示昵称</span>
              <input
                v-model="profileName"
                data-testid="profile-name-input"
                maxlength="64"
                placeholder="输入展示昵称"
                type="text"
              />
            </label>
            <div class="action-row">
              <button
                class="btn btn-primary"
                data-testid="profile-save"
                type="button"
                :disabled="profileSaving"
                @click="saveProfile"
              >
                {{ profileSaving ? '保存中...' : '保存资料' }}
              </button>
              <span class="helper-text">留空时系统会自动回退为登录名。</span>
            </div>
          </article>

          <article class="panel">
            <div :id="groupIds.theme" class="group-anchor" />
            <div class="section-head">
              <h2>主题设置</h2>
              <p>当前主题：{{ currentThemeLabel }}</p>
            </div>
            <div class="theme-option-row">
              <button
                v-for="option in themeOptions"
                :key="option.value"
                class="theme-pill"
                :class="{ active: appStore.theme === option.value }"
                type="button"
                @click="changeTheme(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <p class="helper-text theme-tip">深色模式采用自然深灰配色，阅读更柔和。</p>
          </article>

          <article class="panel">
            <div :id="groupIds.avatar" class="group-anchor" />
            <div class="section-head">
              <h2>头像管理</h2>
            </div>
            <div v-if="feedback.avatar.level !== 'idle'" class="feedback-bar" :class="feedbackClass('avatar')">{{ feedbackText('avatar') }}</div>
            <BaseUploader
              ref="avatarUploaderRef"
              class="avatar-uploader"
              hint="选择头像后点击上传"
              :file="selectedAvatarFile"
              accept="image/*"
              :disabled="avatarUploading"
              input-testid="profile-avatar-input"
              @update:file="selectedAvatarFile = $event"
              @select="handleAvatarSelected"
            />
            <div class="action-row action-row-stack">
              <button
                class="btn"
                type="button"
                :disabled="avatarUploading"
                data-testid="profile-avatar-upload"
                @click="triggerAvatarUpload"
              >
                {{ avatarUploading ? '头像上传中...' : '上传头像' }}
              </button>
              <button
                class="btn btn-danger"
                type="button"
                :disabled="avatarUploading || !viewProfile.avatarUrl"
                data-testid="profile-avatar-delete"
                @click="clearAvatar"
              >
                {{ avatarUploading ? '处理中...' : '删除头像' }}
              </button>
            </div>
          </article>

          <article class="panel">
            <div :id="groupIds.stats" class="group-anchor" />
            <div class="section-head">
              <h2>数据概览</h2>
            </div>
            <section class="stat-grid" data-testid="profile-stat-grid">
            <article
              v-for="item in statCards"
              :key="item.label"
              class="stat-card"
              :data-testid="`profile-stat-${item.key}`"
            >
              <div class="stat-main-row">
                <p class="stat-label">{{ item.label }}</p>
                <p class="stat-value" :data-testid="`profile-stat-value-${item.key}`">{{ item.value }}</p>
              </div>
              <p class="stat-desc" :data-testid="`profile-stat-desc-${item.key}`">{{ item.description }}</p>
            </article>
            </section>
          </article>

          <article class="panel">
            <div :id="groupIds.activity" class="group-anchor" />
            <div class="section-head">
              <h2>我的资料</h2>
              <p>最近 5 条资料活动</p>
            </div>
            <ul v-if="collectionsLoading" class="item-list">
              <li class="item-row"><BaseSkeleton height="2.5rem" /></li>
              <li class="item-row"><BaseSkeleton height="2.5rem" /></li>
            </ul>
            <ul v-else-if="recentResources.length" class="item-list">
              <li v-for="item in recentResources" :key="item.id" class="item-row">
                <div>
                  <RouterLink :to="`/resources/${item.id}`" class="item-link">{{ item.title }}</RouterLink>
                  <p class="item-meta">{{ item.status }} · {{ item.visibility }}</p>
                </div>
                <span class="item-time">{{ item.updatedAt }}</span>
              </li>
            </ul>
            <BaseEmpty v-else title="暂无资料" description="当前账号还没有资料记录。" />
            <hr class="section-divider" />

            <div class="section-head section-sub">
              <h3>我的笔记</h3>
              <p>最近 5 条笔记活动</p>
            </div>
            <ul v-if="collectionsLoading" class="item-list compact">
              <li class="item-row"><BaseSkeleton height="2.5rem" /></li>
              <li class="item-row"><BaseSkeleton height="2.5rem" /></li>
            </ul>
            <ul v-else-if="recentNotes.length" class="item-list compact">
              <li v-for="item in recentNotes" :key="item.id" class="item-row">
                <div>
                  <RouterLink :to="`/notes/${item.id}`" class="item-link">{{ item.title }}</RouterLink>
                  <p class="item-meta">{{ item.status }}</p>
                </div>
              </li>
            </ul>
            <BaseEmpty v-else title="暂无笔记" description="当前账号还没有笔记记录。" />

            <hr class="section-divider" />

            <div class="section-head section-sub">
              <h3>我的简历</h3>
              <p>最近 5 条简历活动</p>
            </div>
            <ul v-if="collectionsLoading" class="item-list compact">
              <li class="item-row"><BaseSkeleton height="2.5rem" /></li>
              <li class="item-row"><BaseSkeleton height="2.5rem" /></li>
            </ul>
            <ul v-else-if="recentResumes.length" class="item-list compact">
              <li v-for="item in recentResumes" :key="item.id" class="item-row">
                <div>
                  <span class="item-link">{{ item.fileName }}</span>
                  <p class="item-meta">{{ item.templateKey }} · {{ item.status }}</p>
                </div>
                <span class="item-time">{{ item.updatedAt }}</span>
              </li>
            </ul>
            <BaseEmpty v-else title="暂无简历" description="当前账号还没有简历记录。" />
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseUploader from '@/components/base/BaseUploader.vue'
import {
  deleteMyAvatar,
  fetchMeSummary,
  fetchMyRecentNotes,
  fetchMyRecentResources,
  fetchMyRecentResumes,
  type MeDashboardData,
  type MeSummaryData,
  updateMyProfile,
  uploadMyAvatar
} from '@/api/me'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const appStore = useAppStore()
const authStore = useAuthStore()
const meSummary = ref<MeSummaryData | null>(null)
const recentResources = ref<MeDashboardData['recentResources']>([])
const recentNotes = ref<MeDashboardData['recentNotes']>([])
const recentResumes = ref<MeDashboardData['recentResumes']>([])
const loading = ref(true)
const collectionsLoading = ref(true)
const loadError = ref(false)
const avatarUploading = ref(false)
const profileSaving = ref(false)
const selectedAvatarFile = ref<File | null>(null)
const avatarUploaderRef = ref<InstanceType<typeof BaseUploader> | null>(null)
const profileName = ref('')
const activeGroup = ref('account')

const groupIds = {
  account: 'settings-group-account',
  profile: 'settings-group-profile',
  theme: 'settings-group-theme',
  avatar: 'settings-group-avatar',
  stats: 'settings-group-stats',
  activity: 'settings-group-activity'
} as const

const settingGroups = [
  { id: 'account', label: '账户信息' },
  { id: 'profile', label: '基础资料' },
  { id: 'theme', label: '主题设置' },
  { id: 'avatar', label: '头像管理' },
  { id: 'stats', label: '数据概览' },
  { id: 'activity', label: '最近活动' }
] as const

const themeOptions = [
  { label: '跟随系统', value: '跟随系统' },
  { label: '浅色模式', value: '浅色模式' },
  { label: '深色模式', value: '深色模式' }
] as const

const currentThemeLabel = computed(() => {
  const matched = themeOptions.find((item) => item.value === appStore.theme)
  return matched?.label ?? appStore.theme
})

type FeedbackScope = 'profile' | 'avatar'
type FeedbackLevel = 'saved' | 'failed' | 'idle'
const feedback = ref<Record<FeedbackScope, { level: FeedbackLevel, message: string }>>({
  profile: { level: 'idle', message: '等待保存操作' },
  avatar: { level: 'idle', message: '等待头像操作' }
})
let groupObserver: IntersectionObserver | null = null

const viewProfile = computed(() => {
  if (meSummary.value?.profile) {
    return meSummary.value.profile
  }

  if (authStore.profile) {
    return {
      id: authStore.profile.id,
      login: window.localStorage.getItem('sharehub.userKey') || authStore.profile.nickname,
      displayName: authStore.profile.nickname,
      avatarUrl: authStore.profile.avatarUrl,
      status: 'ACTIVE'
    }
  }

  return {
    id: 0,
    login: '',
    displayName: '未登录用户',
    avatarUrl: undefined,
    status: 'ACTIVE'
  }
})

const status = computed(() => {
  if (loading.value && !authStore.profile && !meSummary.value) return 'loading'
  if (loadError.value && !authStore.profile && !meSummary.value) return 'error'
  return 'ready'
})

const statCards = computed(() => {
  const stats = meSummary.value?.stats ?? {
    resources: 0,
    favorites: 0,
    roadmaps: 0,
    notes: 0,
    resumes: 0,
    recentResources: 0,
    publishedResources: 0,
    draftNotes: 0,
    generatedResumes: 0
  }

  return [
    {
      key: 'resources',
      label: '资料工作台',
      value: stats.resources,
      description: `近 7 天新增 ${stats.recentResources} 条，已发布 ${stats.publishedResources} 条`
    },
    {
      key: 'notes',
      label: '社区笔记',
      value: stats.notes,
      description: `当前草稿 ${stats.draftNotes} 条`
    },
    {
      key: 'resumes',
      label: '简历记录',
      value: stats.resumes,
      description: `已生成 ${stats.generatedResumes} 份`
    },
    {
      key: 'favorites-roadmaps',
      label: '收藏与路线',
      value: `${stats.favorites} / ${stats.roadmaps}`,
      description: '左侧为收藏数，右侧为路线数'
    }
  ]
})

async function loadSummary() {
  loading.value = true
  loadError.value = false

  try {
    meSummary.value = await fetchMeSummary()
    profileName.value = meSummary.value.profile.displayName
    authStore.updateProfile({
      nickname: meSummary.value.profile.displayName,
      avatarUrl: meSummary.value.profile.avatarUrl
    })
  } catch {
    meSummary.value = null
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function loadCollections() {
  collectionsLoading.value = true
  const [resourcesResult, notesResult, resumesResult] = await Promise.allSettled([
    fetchMyRecentResources(5),
    fetchMyRecentNotes(5),
    fetchMyRecentResumes(5)
  ])

  recentResources.value = resourcesResult.status === 'fulfilled' ? resourcesResult.value : []
  recentNotes.value = notesResult.status === 'fulfilled' ? notesResult.value : []
  recentResumes.value = resumesResult.status === 'fulfilled' ? resumesResult.value : []
  collectionsLoading.value = false
}

function setFeedback(scope: FeedbackScope, level: FeedbackLevel, message: string) {
  feedback.value[scope] = { level, message }
}

function feedbackClass(scope: FeedbackScope) {
  return {
    'is-idle': feedback.value[scope].level === 'idle',
    'is-saved': feedback.value[scope].level === 'saved',
    'is-failed': feedback.value[scope].level === 'failed'
  }
}

function feedbackText(scope: FeedbackScope) {
  const state = feedback.value[scope]
  if (state.level === 'saved') {
    return `Saved: ${state.message}`
  }
  if (state.level === 'failed') {
    return `Failed: ${state.message}`
  }
  return `Ready: ${state.message}`
}

function changeTheme(nextTheme: '跟随系统' | '浅色模式' | '深色模式') {
  if (appStore.theme === nextTheme) {
    return
  }
  appStore.setTheme(nextTheme)
  appStore.showToast('主题已更新', `${nextTheme} 已生效`, 'success')
}

function scrollToGroup(groupId: string) {
  const target = document.getElementById(groupIds[groupId as keyof typeof groupIds])
  if (!target) {
    return
  }
  activeGroup.value = groupId
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function setupGroupObserver() {
  groupObserver?.disconnect()
  groupObserver = new IntersectionObserver(
    (entries) => {
      const visibleEntries = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)

      if (!visibleEntries.length) {
        return
      }

      const matched = settingGroups.find((group) => groupIds[group.id] === visibleEntries[0].target.id)
      if (matched) {
        activeGroup.value = matched.id
      }
    },
    {
      rootMargin: '-24% 0px -62% 0px',
      threshold: [0.1, 0.25, 0.5]
    }
  )

  settingGroups.forEach((group) => {
    const element = document.getElementById(groupIds[group.id])
    if (element) {
      groupObserver?.observe(element)
    }
  })
}

async function handleAvatarSelected(file: File | null) {
  if (!file) {
    return
  }

  avatarUploading.value = true

  try {
    await uploadMyAvatar(file)
    selectedAvatarFile.value = null
    await loadSummary()
    if (meSummary.value?.profile.avatarUrl) {
      authStore.updateProfile({
        avatarUrl: meSummary.value.profile.avatarUrl
      })
    }
    appStore.showToast('头像已更新', '个人中心已刷新为最新头像', 'success')
    setFeedback('avatar', 'saved', '头像已上传并完成刷新。')
  } catch {
    setFeedback('avatar', 'failed', '头像上传失败，请重试。')
  } finally {
    avatarUploading.value = false
    selectedAvatarFile.value = null
  }
}

async function saveProfile() {
  if (!viewProfile.value.login) {
    return
  }

  profileSaving.value = true
  try {
    await updateMyProfile({
      name: profileName.value.trim()
    })
    await loadSummary()
    appStore.showToast('个人资料已保存', '刷新后将显示最新资料', 'success')
    setFeedback('profile', 'saved', '资料已保存并同步。')
  } catch {
    appStore.showToast('资料保存失败', '请检查网络或服务端状态后重试', 'error')
    setFeedback('profile', 'failed', '资料保存失败，请稍后重试。')
  } finally {
    profileSaving.value = false
  }
}

async function clearAvatar() {
  if (!viewProfile.value.avatarUrl || avatarUploading.value) {
    return
  }

  avatarUploading.value = true
  try {
    await deleteMyAvatar()
    await loadSummary()
    authStore.updateProfile({
      avatarUrl: undefined
    })
    appStore.showToast('头像已移除', '已回退为默认头像展示', 'success')
    setFeedback('avatar', 'saved', '头像已删除。')
  } catch {
    appStore.showToast('移除头像失败', '请稍后重试', 'error')
    setFeedback('avatar', 'failed', '头像删除失败，请稍后重试。')
  } finally {
    avatarUploading.value = false
    selectedAvatarFile.value = null
  }
}

function triggerAvatarUpload() {
  avatarUploaderRef.value?.openFileDialog()
}

onMounted(() => {
  if (authStore.profile?.nickname) {
    profileName.value = authStore.profile.nickname
  }

  void Promise.allSettled([loadSummary(), loadCollections()]).finally(() => {
    void nextTick(() => {
      setupGroupObserver()
    })
  })
})

onBeforeUnmount(() => {
  groupObserver?.disconnect()
})
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 8px;
}

.profile-shell {
  display: grid;
  gap: 16px;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid #d9d9df;
  background: linear-gradient(180deg, #fafafa 0%, #f5f5f6 100%);
}

.kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6b7280;
}

.page-title {
  margin: 6px 0 2px;
  font-size: 30px;
  line-height: 1.1;
  color: #171717;
}

.subtitle {
  margin: 0;
  color: #52525b;
  font-size: 14px;
}

.header-chip {
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid #d4d4d8;
  background: #ffffff;
  color: #52525b;
  font-size: 13px;
}

.chip-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: #a1a1aa;
}

.avatar {
  width: 54px;
  height: 54px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #111827;
  color: #ffffff;
  font-size: 22px;
  font-weight: 600;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.identity-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #52525b;
  font-size: 13px;
}

.identity-text strong {
  color: #18181b;
  font-size: 15px;
}

.settings-layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 16px;
}

.settings-nav {
  border-radius: 14px;
  border: 1px solid #e4e4e7;
  background: #fcfcfd;
  padding: 14px;
  align-self: start;
  position: sticky;
  top: 20px;
}

.settings-nav ul {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.settings-nav li {
  margin: 0;
}

.nav-link {
  width: 100%;
  text-align: left;
  font-size: 13px;
  color: #52525b;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
}

.nav-link:hover {
  border-color: #d4d4d8;
  background: #ffffff;
}

.nav-link.active {
  border-color: #c7e9dd;
  background: #effaf6;
  color: #087357;
}

.nav-title {
  margin: 0;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #71717a;
}

.settings-main {
  display: grid;
  gap: 16px;
}

.panel {
  border-radius: 14px;
  background: white;
  border: 1px solid #e4e4e7;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.03);
  padding: 18px;
}

.group-anchor {
  position: relative;
  top: -10px;
  height: 1px;
}

.panel-dark {
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
}

.panel h2 {
  margin: 0;
  font-size: 18px;
  color: #18181b;
}

.section-head p {
  margin: 4px 0 0;
}

.section-sub h3 {
  margin: 0;
  font-size: 16px;
  color: #27272a;
}

.section-divider {
  border: 0;
  border-top: 1px solid #ececf1;
  margin: 16px 0;
}

.feedback-bar {
  margin: 10px 0 12px;
  border-radius: 8px;
  border: 1px solid #e4e4e7;
  background: #fafafa;
  color: #52525b;
  font-size: 12px;
  padding: 8px 10px;
}

.feedback-bar.is-saved {
  border-color: #b7e4d5;
  background: #edf9f4;
  color: #0e7a5c;
}

.feedback-bar.is-failed {
  border-color: #f3d1d1;
  background: #fff5f5;
  color: #b42318;
}

.panel-note,
.item-meta,
.item-time,
.helper-text,
.field-label {
  color: #71717a;
  font-size: 13px;
}

.panel-note {
  margin: 6px 0 0;
}

.field {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.field-label {
  font-weight: 600;
}

.field input {
  width: 100%;
  height: 42px;
  border: 1px solid #d4d4d8;
  border-radius: 10px;
  background: #ffffff;
  color: #18181b;
  padding: 0 12px;
  outline: none;
}

.field input:focus {
  border-color: #10a37f;
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.14);
}

.uploader-shell {
  margin-top: 14px;
  display: grid;
  gap: 8px;
}

.action-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-row-stack {
  margin-top: 8px;
}

.theme-option-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.theme-pill {
  border: 1px solid #d4d4d8;
  border-radius: 999px;
  background: #ffffff;
  color: #3f3f46;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
}

.theme-pill.active {
  border-color: #10a37f;
  background: #ecfdf5;
  color: #047857;
}

.theme-tip {
  margin-top: 10px;
}

:deep(.avatar-uploader) {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  gap: 6px;
}

:deep(.avatar-uploader .hint) {
  font-size: 12px;
}

.btn {
  border: 1px solid #d4d4d8;
  background: #ffffff;
  color: #18181b;
  padding: 10px 14px;
  border-radius: 10px;
  font-weight: 500;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  border-color: #10a37f;
  background: #10a37f;
  color: #ffffff;
}

.btn-danger {
  border-color: #f1d0d0;
  color: #b42318;
  background: #fff7f7;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  border: 1px solid #e4e4e7;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px 12px;
}

.stat-main-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.stat-label {
  margin: 0;
  font-size: 11px;
  color: #71717a;
}

.stat-value {
  margin: 0;
  font-size: 18px;
  line-height: 1;
  font-weight: 650;
  color: #18181b;
}

.stat-desc {
  margin: 6px 0 0;
  color: #71717a;
  font-size: 12px;
  line-height: 1.35;
}

.panel-heading p {
  margin: 4px 0 0;
}

.item-list {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.item-list.compact {
  margin-top: 10px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f4f4f5;
}

.item-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-link {
  color: #18181b;
  font-weight: 600;
  text-decoration: none;
}

.item-link:hover {
  color: #10a37f;
}

.item-meta {
  margin: 4px 0 0;
}

.item-time {
  flex-shrink: 0;
}

@media (max-width: 960px) {
  .settings-header {
    flex-direction: column;
  }

  .settings-layout,
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .settings-nav {
    position: static;
  }

  .item-row {
    flex-direction: column;
  }

  .action-row {
    flex-direction: column;
    align-items: stretch;
  }
}

:global([data-theme='dark']) .profile-page .settings-header {
  background: linear-gradient(180deg, #2a3038 0%, #252b33 100%);
  border-color: #3a424d;
}

:global([data-theme='dark']) .profile-page .page-title,
:global([data-theme='dark']) .profile-page .panel h2,
:global([data-theme='dark']) .profile-page .identity-text strong,
:global([data-theme='dark']) .profile-page .item-link,
:global([data-theme='dark']) .profile-page .stat-value {
  color: #eef1f5;
}

:global([data-theme='dark']) .profile-page .kicker,
:global([data-theme='dark']) .profile-page .nav-title,
:global([data-theme='dark']) .profile-page .header-chip,
:global([data-theme='dark']) .profile-page .identity-text,
:global([data-theme='dark']) .profile-page .item-meta,
:global([data-theme='dark']) .profile-page .item-time,
:global([data-theme='dark']) .profile-page .helper-text,
:global([data-theme='dark']) .profile-page .stat-label,
:global([data-theme='dark']) .profile-page .stat-desc {
  color: #a8b2bf;
}

:global([data-theme='dark']) .profile-page .settings-nav,
:global([data-theme='dark']) .profile-page .panel,
:global([data-theme='dark']) .profile-page .stat-card,
:global([data-theme='dark']) .profile-page .feedback-bar,
:global([data-theme='dark']) .profile-page .field input,
:global([data-theme='dark']) .profile-page .btn,
:global([data-theme='dark']) .profile-page .theme-pill {
  background: #2a3038;
  border-color: #3f4754;
}

:global([data-theme='dark']) .profile-page .feedback-bar,
:global([data-theme='dark']) .profile-page .field input,
:global([data-theme='dark']) .profile-page .btn,
:global([data-theme='dark']) .profile-page .theme-pill,
:global([data-theme='dark']) .profile-page .nav-link {
  color: #d5dbe4;
}

:global([data-theme='dark']) .profile-page .settings-nav {
  background: #262c34;
}

:global([data-theme='dark']) .profile-page .nav-link:hover {
  border-color: #566071;
  background: #303741;
}

:global([data-theme='dark']) .profile-page .nav-link.active,
:global([data-theme='dark']) .profile-page .theme-pill.active {
  border-color: #2f8f78;
  background: #153a33;
  color: #7be3cb;
}

:global([data-theme='dark']) .profile-page .item-row {
  border-bottom-color: #353d48;
}

:global([data-theme='dark']) .profile-page .section-divider {
  border-top-color: #3a424d;
}
</style>
