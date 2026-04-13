<template>
  <div>
    <header class="workspace-header">
      <div class="workspace-header__inner">
        <div>
          <RouterLink class="brand" to="/">ShareHub</RouterLink>
          <p>创作工作台</p>
        </div>
        <nav class="workspace-nav">
          <RouterLink to="/publish/resource">发布资料</RouterLink>
          <RouterLink to="/publish/roadmap">创建路线</RouterLink>
          <RouterLink to="/editor/note">写笔记</RouterLink>
          <RouterLink to="/resume">简历工作台</RouterLink>
          <RouterLink to="/me">个人中心</RouterLink>
        </nav>
      </div>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped lang="scss">
.workspace-header {
  border-bottom: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(var(--blur-md));
}

.workspace-header__inner {
  width: var(--container-width);
  margin: 0 auto;
  padding: var(--space-5) 0;
  display: grid;
  gap: var(--space-4);
}

.workspace-header p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-soft);
}

.workspace-nav {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.workspace-nav a {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-pill);
  color: var(--color-text-soft);
}

.workspace-nav a.router-link-active {
  background: rgba(29, 111, 220, 0.08);
  color: var(--color-primary);
}
</style>
