<template>
  <div class="settings-modal-backdrop">
    <div v-if="showToast" class="local-toast">{{ toastMessage }}</div>
    <div class="settings-modal">
      <div class="settings-sidebar">
        <button class="icon-btn close-btn" @click="router.back()">
          <div class="i-carbon-close"></div>
        </button>

        <div class="user-profile-header">
          <div class="user-avatar-wrapper">
            <div class="i-carbon-logo-github user-avatar-icon"></div>
          </div>
          <div class="user-info">
            <div class="user-name">{{ authStore.profile?.nickname }}</div>
            <div class="user-type">{{ authStore.profile?.role === 'admin' ? '管理员' : '个人帐户' }}</div>
          </div>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section-title">个性化</div>
        <nav class="settings-nav">
          <button v-for="tab in tabs" :key="tab.id"
                  class="nav-tab"
                  :class="{ active: currentTab === tab.id }"
                  @click="currentTab = tab.id">
            <div :class="[tab.icon, 'tab-icon']"></div>
            <span>{{ tab.label }}</span>
          </button>
        </nav>
      </div>

      <div class="settings-content">
        <div class="content-scroll">
          <header class="content-header">
            <h3>{{ currentTabLabel }}</h3>
          </header>

          <!-- 个人资料 -->
          <template v-if="currentTab === 'profile'">
            <div class="setting-list">
              <div class="setting-item avatar-item">
                <div class="item-info">头像</div>
                <div class="item-control row-control">
                  <img src="https://avatars.githubusercontent.com/u/9919?s=64&v=4" class="avatar-preview" />
                  <button class="btn-outline" @click="handleAvatarChange">更换头像</button>
                </div>
              </div>
              <div class="setting-item">
                <div class="item-info">
                  用户名
                  <p class="item-desc">用于社区显示和个人主页URL（sharehub.com/@{{ profileForm.username }}）</p>
                </div>
                <div class="item-control">
                  <input type="text" class="control-input" v-model="profileForm.username" />
                </div>
              </div>
              <div class="setting-item">
                <div class="item-info">
                  个人简介
                  <p class="item-desc">展示在你的帖子和个人主页的信息</p>
                </div>
                <div class="item-control" style="flex: 1; margin-left: 40px; justify-content: flex-end;">
                  <textarea class="control-textarea" placeholder="分享你的技术栈与目标..." v-model="profileForm.bio"></textarea>
                </div>
              </div>
              <div class="setting-item border-none">
                <div class="item-info">个人网站</div>
                <div class="item-control">
                  <input type="url" class="control-input" placeholder="https://" v-model="profileForm.website" />
                </div>
              </div>
            </div>
            <div class="save-actions">
              <button class="btn-primary" @click="handleSaveProfile">保存修改</button>
            </div>
          </template>

          <!-- 偏好设置 -->
          <template v-else-if="currentTab === 'preferences'">
            <div class="setting-list">
              <div class="setting-item">
                <div class="item-info">界面主题</div>
                <div class="item-control">
                  <select class="control-select" v-model="prefForm.theme" @change="handleSavePreferences">
                    <option>跟随系统</option>
                    <option>浅色模式</option>
                    <option>深色模式</option>
                  </select>
                </div>
              </div>
              <div class="setting-item">
                <div class="item-info">
                  社区动态提醒
                  <p class="item-desc">当有人回复、点赞你的帖子或路线时，是否发送站内通知</p>
                </div>
                <div class="item-control">
                  <input type="checkbox" class="toggle-switch" v-model="prefForm.notifyCommunity" @change="handleSavePreferences" />
                </div>
              </div>
              <div class="setting-item border-none">
                <div class="item-info">
                  精选技术邮件
                  <p class="item-desc">每周接收来自 ShareHub 资料广场的精选架构/开源内容推送</p>
                </div>
                <div class="item-control">
                  <input type="checkbox" class="toggle-switch" v-model="prefForm.notifyEmail" @change="handleSavePreferences" />
                </div>
              </div>
            </div>
          </template>

          <!-- 账号绑定 -->
          <template v-else-if="currentTab === 'integrations'">
            <div class="setting-list">
              <div class="setting-item auth-provider border-none">
                <div class="provider-info row-control">
                  <div class="i-carbon-logo-github" style="font-size: 24px;"></div>
                  <div class="item-info">
                    GitHub
                    <p class="item-desc">已连接至 {{ authStore.profile?.nickname }}</p>
                  </div>
                </div>
                <div class="item-control"><button class="btn-outline text-danger" @click="handleUnbind">解除绑定</button></div>
              </div>
            </div>
          </template>

          <!-- 安全保护 -->
          <template v-else-if="currentTab === 'security'">
            <div class="setting-block card-block">
              <div class="setting-header block-padding">
                <div class="i-carbon-security setting-block-icon"></div>
                <div class="setting-meta">
                  <h4>双因素认证 (2FA)</h4>
                  <p>开启 Authenticator 或短信验证码，防止由于密码泄露导致的账户安全风险。</p>
                </div>
              </div>
              <div class="setting-action block-padding">
                <button class="btn-outline" @click="handleEnable2FA">立即开启 2FA</button>
              </div>
            </div>

            <div class="setting-list">
              <div class="setting-item">
                <div class="item-info">登录密码</div>
                <div class="item-control">
                  <button class="btn-outline" @click="handleUpdatePassword">更新密码</button>
                </div>
              </div>
              <div class="setting-item">
                <div class="item-info">
                  活跃会话
                  <p class="item-desc">当前设备 (Mac OS, Safari 浏览器)</p>
                </div>
                <div class="item-control">
                  <button class="btn-danger" @click="handleLogoutAllDevices">登出所有设备</button>
                </div>
              </div>
              <div class="setting-item border-none">
                <div class="item-info">帐号注销</div>
                <div class="item-control">
                  <button class="btn-text text-danger" @click="handleDeleteAccount">删除帐户数据</button>
                </div>
              </div>
            </div>
          </template>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const currentTab = ref('profile')

const tabs = [
  { id: 'profile', label: '个人资料', icon: 'i-carbon-logo-github' },
  { id: 'preferences', label: '偏好设置', icon: 'i-carbon-settings-adjust' },
  { id: 'integrations', label: '账号绑定', icon: 'i-carbon-link' },
  { id: 'security', label: '安全与隐私', icon: 'i-carbon-locked' },
]

const currentTabLabel = computed(() => {
  return tabs.find(t => t.id === currentTab.value)?.label || '设置'
})

// 个人资料相关
const profileForm = reactive({
  username: authStore.profile?.nickname || '',
  bio: authStore.profile?.headline || '',
  website: ''
})

const showToast = ref(false)
const toastMessage = ref('')

const triggerToast = (msg: string) => {
  toastMessage.value = msg
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const handleAvatarChange = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = () => triggerToast('头像已更新 (模拟)')
  input.click()
}

const handleSaveProfile = () => {
  authStore.updateProfile({
    nickname: profileForm.username,
    headline: profileForm.bio
  })
  triggerToast('个人资料保存成功！')
}

// 偏好设置相关
const prefForm = reactive({
  theme: '跟随系统',
  notifyCommunity: true,
  notifyEmail: true
})

const handleSavePreferences = () => {
  triggerToast('偏好设置已自动保存')
}

// 账号绑定相关
const handleUnbind = () => {
  if (confirm('确定要解除 GitHub 账号绑定吗？')) {
    triggerToast('已成功解除绑定')
  }
}

// 安全与隐私相关
const handleEnable2FA = () => {
  triggerToast('向导：请下载 Authenticator 扫描二维码')
}

const handleUpdatePassword = () => {
  const newPwd = prompt('请输入新密码：')
  if (newPwd) triggerToast('密码更新成功，请牢记。')
}

const handleLogoutAllDevices = () => {
  if (confirm('这将登出你所有设备上的 ShareHub 帐号，需要重新登录。确定要继续吗？')) {
    authStore.logout()
    router.push('/login')
  }
}

function handleDeleteAccount() {
  if (confirm('警告：此操作不可逆！删除后将清除所有数据。确定要删除帐户吗？')) {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.settings-modal-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  padding: 32px;
  background-color: rgba(240, 240, 240, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.local-toast {
  position: absolute;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: #10b981;
  color: white;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
  z-index: 100;
  animation: fadeInDown 0.3s ease-out;
}

@keyframes fadeInDown {
  from { opacity: 0; transform: translate(-50%, -10px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}

.settings-modal {
  width: 100%;
  max-width: 900px;
  height: 80vh;
  min-height: 500px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(0, 0, 0, 0.05);
  display: flex;
  overflow: hidden;
}

.settings-sidebar {
  width: 250px;
  background: #f9fafb;
  border-right: 1px solid #e5e7eb;
  padding: 20px 12px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.close-btn {
  position: absolute;
  top: 20px;
  left: 12px;
  background: transparent;
  border: none;
  font-size: 20px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}

.close-btn:hover {
  background: #e5e7eb;
  color: #111827;
}

.settings-title {
  margin: 40px 0 16px 12px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}
.user-profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  margin-top: 24px;
}

.user-avatar-wrapper {
  width: 48px;
  height: 48px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.user-avatar-icon {
  font-size: 48px;
  color: #111827;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  line-height: 1.2;
}

.user-type {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.nav-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 16px 12px;
}

.nav-section-title {
  font-size: 16px;
  color: #374151;
  padding: 0 12px;
  margin-bottom: 8px;
  margin-top: 8px;
}
.settings-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  border: none;
  background: transparent;
  text-align: left;
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-tab:hover {
  background: #f3f4f6;
  color: #111827;
}

.nav-tab.active {
  background: #e5e7eb;
  color: #111827;
}

.tab-icon {
  font-size: 16px;
}

.settings-content {
  flex: 1;
  background: white;
  position: relative;
}

.content-scroll {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  padding: 32px 48px;
}

.content-header {
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 16px;
  margin-bottom: 24px;
}

.content-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.setting-block {
  background: #f9fafb;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  margin-bottom: 32px;
}

.block-padding {
  padding: 20px;
}

.setting-header {
  display: flex;
  gap: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.setting-block-icon {
  font-size: 24px;
  color: #111827;
}

.setting-meta h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 600;
}

.setting-meta p {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.setting-action {
  background: white;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}

.btn-primary {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  background: #111827;
  font-size: 14px;
  font-weight: 500;
  color: white;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.85;
}

.btn-outline {
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  background: white;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.setting-list {
  display: flex;
  flex-direction: column;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 0;
  border-bottom: 1px solid #e5e7eb;
}

.avatar-item {
  align-items: center;
}

.avatar-preview {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
  object-fit: cover;
}

.setting-item.border-none {
  border-bottom: none;
}

.item-info {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.item-desc {
  margin: 6px 0 0 0;
  font-size: 13px;
  font-weight: 400;
  color: #6b7280;
  max-width: 360px;
  line-height: 1.5;
}

.item-control {
  display: flex;
  align-items: center;
  gap: 16px;
}

.row-control {
  align-items: center;
  gap: 16px;
}

.control-input {
  width: 280px;
  height: 38px;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.control-input:focus, .control-textarea:focus {
  border-color: #2563eb;
}

.control-textarea {
  width: 100%;
  max-width: 400px;
  min-height: 80px;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  outline: none;
}

.control-select {
  padding: 8px 32px 8px 12px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  background-color: white;
  font-size: 14px;
  color: #111827;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%236B7280'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 16px;
}

.save-actions {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

.btn-text {
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  font-size: 14px;
  color: #111827;
  font-weight: 500;
  cursor: pointer;
}

.text-danger {
  color: #dc2626 !important;
}

.btn-danger {
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid #fee2e2;
  background: #fef2f2;
  color: #dc2626;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-danger:hover {
  background: #fee2e2;
}

.toggle-switch {
  appearance: none;
  width: 44px;
  height: 24px;
  background: #e5e7eb;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  outline: none;
  transition: background 0.3s;
}

.toggle-switch:checked {
  background: #10b981;
}

.toggle-switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: transform 0.3s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.toggle-switch:checked::after {
  transform: translateX(20px);
}

/* Provider Integrations specific styles */
.auth-provider {
  align-items: center;
}

.provider-info {
  display: flex;
}

@media (max-width: 768px) {
  .settings-modal {
    flex-direction: column;
    height: 100%;
    border-radius: 0;
  }
  .settings-sidebar {
    width: 100%;
    height: auto;
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
    padding: 16px;
  }
  .settings-title {
    display: none;
  }
  .settings-nav {
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 8px;
  }
  .nav-tab {
    white-space: nowrap;
  }
  .content-scroll {
    padding: 24px;
  }
  .close-btn {
    display: none;
  }
  .setting-item {
    flex-direction: column;
    gap: 12px;
  }
  .item-control, .control-input {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>
