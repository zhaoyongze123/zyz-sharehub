<template>
  <div class="page-shell auth-page">
    <section class="auth-card glass-panel">
      <BaseTag tone="primary">GitHub OAuth</BaseTag>
      <h1>用内容构建你的学习资产</h1>
      <p>登录后可发布资料、组织路线、记录笔记并导出简历。当前先用前端模拟登录态，后续再切真实后端 OAuth。</p>

      <div class="auth-actions">
        <BaseButton size="lg" @click="login('user')">以普通用户进入</BaseButton>
        <BaseButton size="lg" variant="secondary" @click="login('admin')">以管理员进入</BaseButton>
      </div>

      <BaseEmpty title="回跳提示" :description="redirectTarget" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseTag from '@/components/base/BaseTag.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const redirectTarget = computed(() => `登录成功后将跳转到：${String(route.query.redirect || '/')}`)

function login(role: 'user' | 'admin') {
  authStore.loginAs(role)
  appStore.showToast('登录成功', role === 'admin' ? '已进入管理员模拟态' : '已进入普通用户模拟态')
  router.push(String(route.query.redirect || '/'))
}
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

.auth-actions {
  display: grid;
  gap: var(--space-3);
}
</style>
