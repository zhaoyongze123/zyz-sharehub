<template>
  <div class="layout">
    <header class="site-header">
      <div class="site-header__inner">
        <RouterLink class="brand" to="/">ShareBase</RouterLink>
        <nav class="main-nav">
          <RouterLink to="/resources">资料</RouterLink>
          <RouterLink to="/roadmaps">路线</RouterLink>
          <RouterLink to="/notes">笔记</RouterLink>
        </nav>
        <div class="header-actions">
          <RouterLink class="ghost-link" to="/login">登录</RouterLink>
          <RouterLink class="primary-link" to="/publish/resource">开始发布</RouterLink>
        </div>
      </div>
    </header>
    <main>
      <RouterView />
    </main>
    <footer class="site-footer">
      <div class="site-footer__inner">
        <span>© 2026 ShareBase. 让资料、路线、笔记和简历在同一套系统里流动。</span>
        <div class="site-footer__links">
          <a href="#">隐私政策</a>
          <a href="#">使用条款</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped lang="scss">
.site-header {
  position: sticky;
  top: 0;
  z-index: var(--z-nav);
  border-bottom: 1px solid var(--color-border);
  background: rgba(245, 247, 251, 0.76);
  backdrop-filter: blur(var(--blur-md));
}

.site-header__inner,
.site-footer__inner {
  width: var(--container-width);
  margin: 0 auto;
  min-height: 4rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.brand {
  font-size: var(--font-size-xl);
  font-weight: 800;
  letter-spacing: -0.03em;
}

.main-nav,
.header-actions,
.site-footer__links {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.main-nav a,
.ghost-link,
.site-footer__links a {
  color: var(--color-text-soft);
}

.main-nav a.router-link-active {
  color: var(--color-text-main);
}

.primary-link {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: white;
  box-shadow: var(--shadow-primary);
}

.site-footer {
  padding: var(--space-8) 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

@media (max-width: 48rem) {
  .site-header__inner,
  .site-footer__inner {
    flex-wrap: wrap;
    padding: var(--space-3) 0;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
