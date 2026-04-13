<template>
  <div class="page-shell auth-page">
    <section class="auth-card glass-panel">
      <BaseTag tone="accent">处理中</BaseTag>
      <h1>正在处理登录结果</h1>
      <p>{{ message }}</p>
      <BaseSkeleton height="1rem" />
      <BaseButton variant="secondary" @click="retry">重新处理</BaseButton>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTag from '@/components/base/BaseTag.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const message = ref('正在校验授权状态并恢复回跳目标页。')
const OAUTH_REDIRECT_KEY = 'ShareHub.oauthRedirect'

function retry() {
  message.value = '正在重新校验授权状态并恢复回跳目标页。'
  void restoreSession()
}

async function restoreSession() {
  try {
    authStore.initialized = false
    const profile = await authStore.syncProfileFromServer()
    authStore.initialized = true

    if (!profile) {
      message.value = '未从 `/api/auth/me` 恢复到真实身份，请重新发起登录。'
      return
    }

    message.value = '真实登录状态已恢复，正在跳转。'
    const sessionRedirect = window.sessionStorage.getItem(OAUTH_REDIRECT_KEY)
    const redirect = typeof route.query.redirect === 'string'
      ? route.query.redirect
      : (sessionRedirect || (profile.role === 'admin' ? '/admin' : '/me'))
    window.sessionStorage.removeItem(OAUTH_REDIRECT_KEY)
    await router.replace(String(redirect))
  } catch {
    authStore.profile = null
    authStore.initialized = true
    window.sessionStorage.removeItem(OAUTH_REDIRECT_KEY)
    message.value = '登录回调校验失败，请确认后端会话有效后重试。'
  }
}

onMounted(() => {
  window.setTimeout(() => {
    void restoreSession()
  }, 300)
})
</script>

<style scoped lang="scss">
.auth-page {
  display: grid;
  place-items: center;
  min-height: calc(100vh - 8rem);
}

.auth-card {
  width: min(32rem, 100%);
  display: grid;
  gap: var(--space-5);
  padding: var(--space-10);
}

.auth-card h1,
.auth-card p {
  margin: 0;
}

.auth-card p {
  color: var(--color-text-soft);
}
</style>
