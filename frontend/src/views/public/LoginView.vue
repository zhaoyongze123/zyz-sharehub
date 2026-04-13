<template>
  <main class="login-shell">
    <!-- 登录面版 -->
    <div class="glass-card login-container animate-fade-in-up">
      <h1 class="login-title">用内容构建你的学习资产</h1>
      <p class="login-subtitle">登录后可发布资料、组织路线、记录笔记并导出简历</p>

      <div class="login-actions">
        <BaseButton
          v-if="isLocalDev"
          class="btn-fixed-height"
          @click="loginForDev('user')"
        >
          本地开发登录
        </BaseButton>
        <BaseButton
          v-if="isLocalDev"
          variant="secondary"
          class="btn-fixed-height"
          @click="loginForDev('admin')"
        >
          本地管理员登录
        </BaseButton>
        <button type="button" class="btn btn-github btn-fixed-height" @click="loginWithGithub">
          <svg class="github-icon" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path>
          </svg>
          使用 GitHub 继续
        </button>
        <RouterLink to="/" class="btn btn-secondary btn-fixed-height">先逛逛公开内容</RouterLink>
      </div>

      <div class="login-footer">
        继续即表示你同意 <a href="#">《用户协议》</a> 与 <a href="#">《隐私政策》</a>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import { useAuthStore } from '@/stores/auth'
import '@/assets/landing.css'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const isLocalDev = computed(() => ['localhost', '127.0.0.1'].includes(window.location.hostname))
const OAUTH_REDIRECT_KEY = 'ShareHub.oauthRedirect'

function loginForDev(role: 'user' | 'admin') {
  authStore.loginAs(role)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/resume'
  void router.replace(redirect)
}

function loginWithGithub() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/me'
  window.sessionStorage.setItem(OAUTH_REDIRECT_KEY, redirect)
  const query = new URLSearchParams({ redirect })
  window.location.assign(`/oauth2/authorization/github?${query.toString()}`)
}

onMounted(() => {
  document.body.classList.add('page-login')
})

onBeforeUnmount(() => {
  document.body.classList.remove('page-login')
})
</script>
