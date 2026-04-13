<template>
  <div class="console-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-brand" @click="router.push('/')">
        <img class="brand-logo" :src="brandLogoUrl" alt="ShareHub logo" />
        <span class="brand-name">ShareHub</span>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">
          <div class="nav-title">探索发现</div>
          <RouterLink to="/resources" class="nav-item">
            <div class="i-carbon-compass nav-icon"></div>
            <span>资料广场</span>
          </RouterLink>
          <RouterLink to="/roadmaps" class="nav-item">
            <div class="i-carbon-map nav-icon"></div>
            <span>路线图</span>
          </RouterLink>
          <RouterLink to="/community" class="nav-item">
            <div class="i-carbon-forum nav-icon"></div>
            <span>社区</span>
          </RouterLink>
        </div>

        <div class="nav-section">
          <div class="nav-title">工作台</div>
          <RouterLink to="/publish/resource" class="nav-item">
            <div class="i-carbon-cloud-upload nav-icon"></div>
            <span>发布资料</span>
          </RouterLink>
          <RouterLink to="/publish/roadmap" class="nav-item">
            <div class="i-carbon-flow-stream nav-icon"></div>
            <span>编排路线</span>
          </RouterLink>
          <RouterLink to="/editor/note" class="nav-item">
            <div class="i-carbon-pen nav-icon"></div>
            <span>记录笔记</span>
          </RouterLink>
          <RouterLink to="/resume" class="nav-item">
            <div class="i-carbon-identification nav-icon"></div>
            <span>我的履历</span>
          </RouterLink>
        </div>
      </nav>

      <div class="sidebar-footer">
        <!-- Account Dropdown Menu (ChatGPT style) -->
        <div class="account-menu-container" v-if="menuOpen">
          <div class="account-menu-overlay" @click="menuOpen = false"></div>
          <div class="account-menu">
            <div class="account-menu-item top-profile" style="align-items: flex-start;">
              <div class="avatar-circle account-avatar account-avatar-lg">
                <img
                  v-if="authStore.profile?.avatarUrl"
                  :src="authStore.profile.avatarUrl"
                  alt="avatar"
                  class="account-avatar-image"
                  data-testid="console-avatar-image"
                />
                <span v-else class="account-avatar-fallback" data-testid="console-avatar-fallback">{{ avatarFallback }}</span>
              </div>
              <div class="profile-text" style="display: flex; flex-direction: column; margin-left: 12px; gap: 4px;">
                <span class="profile-name" style="font-size: 16px; font-weight: 600; color: var(--app-text-main); line-height: 1;" data-testid="console-menu-name">{{ authStore.profile?.nickname || '未登录' }}</span>
                <span class="profile-type" style="font-size: 14px; color: var(--app-text-muted); line-height: 1;">{{ authStore.profile?.role === 'admin' ? '管理员' : '个人帐户' }}</span>
              </div>
            </div>
            
            <div class="menu-divider"></div>
            
            <div class="menu-section-title" style="padding: 8px 16px 4px; font-size: 16px; color: var(--app-text-sub);">个性化</div>
            
            <RouterLink to="/me" class="account-menu-item active-item" @click="menuOpen = false" style="background-color: var(--app-bg-hover); border-radius: 8px; margin: 4px 8px; width: auto; padding: 8px;">
              <div class="avatar-circle account-avatar account-avatar-sm">
                <img
                  v-if="authStore.profile?.avatarUrl"
                  :src="authStore.profile.avatarUrl"
                  alt="avatar"
                  class="account-avatar-image"
                />
                <span v-else class="account-avatar-fallback">{{ avatarFallback }}</span>
              </div>
              <span style="font-weight: 500; color: var(--app-text-main); font-size: 15px;">个人资料</span>
            </RouterLink>
            <RouterLink to="/me" class="account-menu-item" @click="menuOpen = false">
              <div class="i-carbon-settings menu-icon"></div>
              <span>设置</span>
            </RouterLink>
            
            <div class="menu-divider"></div>
            
            <div class="account-menu-item" @click="handleHelp">
              <div class="i-carbon-help menu-icon"></div>
              <span>帮助</span>
              <div class="i-carbon-chevron-right arrow-icon" style="margin-left: auto;"></div>
            </div>
            <div class="account-menu-item" @click="handleLogout">
              <div class="i-carbon-logout menu-icon"></div>
              <span>退出登录</span>
            </div>          </div>
        </div>

        <div class="user-profile-mini" @click="menuOpen = !menuOpen">
          <img
            v-if="authStore.profile?.avatarUrl"
            :src="authStore.profile.avatarUrl"
            alt="avatar"
            class="avatar"
            data-testid="console-sidebar-avatar"
          />
          <div v-else class="avatar" data-testid="console-sidebar-avatar-fallback">{{ avatarFallback }}</div>
          <div class="user-info">
            <span class="user-name" data-testid="console-sidebar-name">{{ authStore.profile?.nickname || '未登录' }}</span>
            <span class="user-role">{{ authStore.profile?.role === 'admin' ? '管理员' : '个人帐户' }}</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="main-container">
      <header class="top-header">
        <div class="header-left">
          <h1 class="page-title">{{ route.meta.title || 'ShareHub' }}</h1>
        </div>
        <div class="header-right">
          <div class="search-trigger" @click="handleSearch">
            <div class="i-carbon-search icon"></div>
            <span class="text">全局检索...</span>
            <kbd>⌘K</kbd>
          </div>
        </div>
      </header>

      <main class="content-area" :class="{ 'no-padding': route.meta.fullHeight }">
        <RouterView v-slot="{ Component }">
          <Transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
      
      <!-- 搜索遮罩层 -->
      <div v-if="searchModalOpen" class="search-modal-backdrop" @click="searchModalOpen = false">
        <div class="search-modal-content" @click.stop>
          <div class="search-input-wrapper">
            <div class="i-carbon-search modal-search-icon"></div>
            <input type="text" class="search-input" placeholder="输入以搜索资源、路书或文章..." @keyup.enter="processSearch" autofocus />
            <kbd class="esc-kbd" @click="searchModalOpen = false">ESC</kbd>
          </div>
          <div class="search-results">
            <div class="search-tip">开始输入，即刻寻找...</div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import brandLogoUrl from '@/assets/logo/sharehub-favicon.png'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuOpen = ref(false)
const searchModalOpen = ref(false)
const avatarFallback = computed(() => authStore.profile?.nickname?.trim().slice(0, 1).toUpperCase() || '未')

const handleSearch = () => {
  searchModalOpen.value = true
}

const processSearch = () => {
  searchModalOpen.value = false
  // 实际检索逻辑会被路由或全局搜索组件处理
}

const handleHelp = () => {
  menuOpen.value = false
  window.open('https://github.com/sharehub-docs', '_blank')
}

const handleLogout = () => {
  menuOpen.value = false
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.console-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: var(--app-bg-main);
  color: var(--app-text-main);
}

.sidebar {
  width: 280px;
  background-color: var(--app-bg-nav);
  border-right: 1px solid var(--app-border);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  z-index: 10;
  position: relative;
}

.sidebar-brand {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--app-bg-hover);
  user-select: none;
}

.brand-logo {
  width: 28px;
  height: 28px;
  object-fit: contain;
  display: block;
}

.brand-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--app-text-main);
  letter-spacing: -0.01em;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.nav-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
  padding-left: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  color: var(--app-text-sub);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.15s ease-in-out;
}

.nav-item:hover {
  background-color: var(--app-bg-hover);
  color: var(--app-text-main);
}

.nav-item.router-link-active {
  background-color: var(--app-bg-hover);
  color: var(--app-accent);
}

.nav-item.router-link-active .nav-icon {
  opacity: 1;
}

.nav-icon {
  width: 18px;
  height: 18px;
  opacity: 0.7;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--app-border);
  position: relative;
}

.account-menu-container {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 10px;
  width: 260px;
  z-index: 50;
}

.account-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: -1;
}

.account-menu {
  background: var(--app-bg-modal);
  border-radius: 16px;
  box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.05), inset 0 0 0 1px rgba(0, 0, 0, 0.05);
  padding: 8px;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: bottom center;
}

@keyframes slideUp {
  0% { transform: translateY(8px) scale(0.98); opacity: 0; }
  100% { transform: translateY(0) scale(1); opacity: 1; }
}

.account-menu-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--app-text-main);
  font-size: 14px;
  text-decoration: none;
  gap: 12px;
  transition: background-color 0.15s;
}

.account-menu-item:hover {
  background-color: var(--app-bg-hover);
}

.top-profile {
  padding: 10px 12px;
  margin-bottom: 4px;
}

.menu-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #8c5a45;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: 14px;
}

.menu-icon-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
}

.profile-text {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.profile-name {
  font-weight: 500;
  font-size: 14px;
}

.profile-type {
  font-size: 12px;
  color: var(--app-text-muted);
}

.account-avatar {
  overflow: hidden;
  border-radius: 50%;
  border: 1.5px solid var(--app-text-main);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg-modal);
  color: var(--app-text-main);
  flex-shrink: 0;
}

.account-avatar-lg {
  width: 40px;
  height: 40px;
  border-width: 2px;
}

.account-avatar-sm {
  width: 24px;
  height: 24px;
  margin-right: 8px;
}

.account-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.account-avatar-fallback {
  font-size: 12px;
  font-weight: 700;
}

.menu-icon {
  width: 18px;
  height: 18px;
  color: var(--app-text-sub);
}

.arrow-icon {
  font-size: 16px;
  color: var(--app-text-light);
  margin-left: auto;
}

.menu-divider {
  height: 1px;
  background-color: var(--app-border);
  margin: 6px 0;
}

.user-profile-mini {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  text-decoration: none;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-profile-mini:hover {
  background-color: var(--app-bg-hover);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #8c5a45;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 500;
  object-fit: cover;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-main);
}

.user-role {
  font-size: 12px;
  color: var(--app-text-muted);
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: var(--app-bg-main);
}

.top-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background-color: rgba(250, 250, 250, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--app-border);
  position: sticky;
  top: 0;
  z-index: 5;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--app-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background-color: var(--app-bg-nav);
  border: 1px solid var(--app-border);
  border-radius: 6px;
  color: var(--app-text-light);
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  transition: all 0.2s;
}

.search-trigger:hover {
  border-color: var(--app-border-focus);
  box-shadow: 0 2px 4px rgba(0,0,0,0.04);
  color: var(--app-text-muted);
}

.search-trigger .icon {
  width: 14px;
  height: 14px;
}

.search-trigger kbd {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  background: var(--app-bg-hover);
  padding: 2px 4px;
  border-radius: 4px;
  color: var(--app-text-muted);
  border: 1px solid var(--app-border);
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

.content-area.no-padding {
  padding: 0;
  overflow-y: hidden;
  display: flex;
  flex-direction: column;
}

/* 全局搜索遮罩样式 */
.search-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 15vh;
  z-index: 100;
  animation: fadeIn 0.15s ease-out;
}

.search-modal-content {
  width: 100%;
  max-width: 600px;
  background: var(--app-bg-modal);
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--app-border);
}

.modal-search-icon {
  font-size: 24px;
  color: var(--app-text-light);
  margin-right: 12px;
}

.search-input {
  flex: 1;
  border: none;
  font-size: 18px;
  outline: none;
  color: var(--app-text-main);
}

.search-input::placeholder {
  color: var(--app-text-light);
}

.esc-kbd {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, monospace;
  font-size: 12px;
  background: var(--app-bg-hover);
  padding: 4px 8px;
  border-radius: 6px;
  color: var(--app-text-muted);
  border: 1px solid var(--app-border);
  cursor: pointer;
}

.search-results {
  min-height: 200px;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg-main);
}

.search-tip {
  color: var(--app-text-light);
  font-size: 15px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
