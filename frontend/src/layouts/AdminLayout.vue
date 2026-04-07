<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink class="brand" to="/admin">ShareBase Admin</RouterLink>
      <nav class="admin-nav">
        <RouterLink to="/admin">概览</RouterLink>
        <RouterLink to="/admin/reviews">内容审核</RouterLink>
        <RouterLink to="/admin/reports">举报处理</RouterLink>
        <RouterLink to="/admin/users">用户管理</RouterLink>
        <RouterLink to="/admin/taxonomy">标签分类</RouterLink>
      </nav>
    </aside>
    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped lang="scss">
.admin-shell {
  display: grid;
  grid-template-columns: 17rem 1fr;
  min-height: 100vh;
}

.admin-sidebar {
  padding: var(--space-6);
  border-right: 1px solid var(--color-border);
  background: rgba(20, 32, 51, 0.94);
}

.brand {
  display: inline-block;
  margin-bottom: var(--space-8);
  color: white;
  font-weight: 800;
}

.admin-nav {
  display: grid;
  gap: var(--space-2);
}

.admin-nav a {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  color: rgba(255, 255, 255, 0.72);
}

.admin-nav a.router-link-active {
  background: rgba(255, 255, 255, 0.12);
  color: white;
}

.admin-main {
  padding: var(--space-8);
}

@media (max-width: 64rem) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    border-right: 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }
}
</style>
