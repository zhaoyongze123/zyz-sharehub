<template>
  <div class="app-shell">
    <header class="header">
      <h1>ShareHub</h1>
      <nav>
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/resources">资料</RouterLink>
        <RouterLink to="/publish">发布</RouterLink>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border-default);
}

nav {
  display: flex;
  gap: var(--space-4);
}

nav a {
  color: var(--color-text-secondary);
  text-decoration: none;
}

nav a.router-link-active {
  color: var(--color-brand-600);
  font-weight: 600;
}

.main {
  max-width: var(--breakpoint-xl);
  margin: 0 auto;
  padding: var(--space-6);
}
</style>
