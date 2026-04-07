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

function retry() {
  window.location.reload()
}

onMounted(() => {
  const role = route.query.role === 'admin' ? 'admin' : 'user'
  window.setTimeout(() => {
    authStore.loginAs(role)
    message.value = '登录状态已恢复，正在跳转。'
    router.replace(String(route.query.redirect || '/'))
  }, 900)
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
