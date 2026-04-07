import { defineStore } from 'pinia'

export interface UserProfile {
  id: number
  nickname: string
  role: 'guest' | 'user' | 'admin'
  headline: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    profile: null as UserProfile | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.profile),
    isAdmin: (state) => state.profile?.role === 'admin'
  },
  actions: {
    async bootstrap() {
      if (this.initialized) {
        return
      }

      const savedRole = window.localStorage.getItem('sharebase.role')
      if (savedRole === 'user' || savedRole === 'admin') {
        this.profile = {
          id: 1,
          nickname: savedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen',
          role: savedRole,
          headline: savedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者'
        }
      }
      this.initialized = true
    },
    loginAs(role: 'user' | 'admin') {
      window.localStorage.setItem('sharebase.role', role)
      this.profile = {
        id: 1,
        nickname: role === 'admin' ? 'Admin Zoe' : 'Alex Chen',
        role,
        headline: role === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者'
      }
    },
    logout() {
      window.localStorage.removeItem('sharebase.role')
      this.profile = null
    }
  }
})
