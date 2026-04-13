import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()

    if (!authStore.initialized) {
      await authStore.bootstrap()
    }

    if (typeof to.meta.title === 'string') {
      document.title = `${to.meta.title} - ShareHub`
    }

    if (to.meta.auth && !authStore.isLoggedIn) {
      return {
        name: 'login',
        query: { redirect: to.fullPath }
      }
    }

    if (to.meta.admin && !authStore.isAdmin) {
      return { name: 'forbidden' }
    }

    return true
  })

  router.afterEach(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}
