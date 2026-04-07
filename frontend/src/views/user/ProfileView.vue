<template>
  <div class="page-shell profile-grid">
    <ProfileHeader role="个人中心" :title="profileTitle" description="统一查看我的发布、收藏、点赞、草稿箱和安全设置。" :stats="profileStats" />

    <section class="glass-panel panel">
      <div class="section-heading">
        <h2>我的内容</h2>
        <p>使用 Tab 区分发布、收藏、点赞、历史和草稿。</p>
      </div>
      <BaseTabs v-model="tab" :items="tabItems" />
      <ul class="content-list">
        <li v-for="item in currentItems" :key="item" class="glass-panel content-item">{{ item }}</li>
      </ul>
    </section>

    <aside class="side-stack">
      <BaseEmpty title="安全设置" description="后续可接密码、绑定账号和登录设备管理。" />
      <BaseButton variant="secondary" @click="logout">退出登录</BaseButton>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseTabs from '@/components/base/BaseTabs.vue'
import ProfileHeader from '@/components/business/ProfileHeader.vue'
import { profileStats } from '@/mock/user'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const tab = ref('published')

const tabItems = [
  { label: '我的发布', value: 'published' },
  { label: '我的收藏', value: 'favorites' },
  { label: '我的点赞', value: 'likes' },
  { label: '浏览历史', value: 'history' },
  { label: '草稿箱', value: 'drafts' }
]

const contentMap: Record<string, string[]> = {
  published: ['MCP Server 鉴权模板仓库', 'RAG 评测体系复盘'],
  favorites: ['多 Agent 调度提示词库', 'RAG 系统设计路线图'],
  likes: ['Agentic Coding 面试高频题集'],
  history: ['Spring Boot + GitHub OAuth 接入清单'],
  drafts: ['Agent 系统设计答题框架']
}

const currentItems = computed(() => contentMap[tab.value] ?? [])
const profileTitle = computed(() => `欢迎回来，${authStore.profile?.nickname ?? '访客'}`)

function logout() {
  authStore.logout()
  appStore.showToast('已退出登录', '已返回公开访问态')
  router.push('/')
}
</script>

<style scoped lang="scss">
.profile-grid {
  display: grid;
  gap: var(--space-6);
}

.panel {
  padding: var(--space-6);
}

.content-list {
  list-style: none;
  margin: var(--space-5) 0 0;
  padding: 0;
  display: grid;
  gap: var(--space-3);
}

.content-item {
  padding: var(--space-4);
}

.side-stack {
  display: grid;
  gap: var(--space-4);
}
</style>
