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
        const savedNickname = window.localStorage.getItem('sharebase.nickname')
        const savedHeadline = window.localStorage.getItem('sharebase.headline')
        this.profile = {
          id: 1,
          nickname: savedNickname || (savedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen'),
          role: savedRole,
          headline: savedHeadline || (savedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者')
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
    updateProfile(data: Partial<UserProfile>) {
      if (this.profile) {
        Object.assign(this.profile, data)
        if (data.nickname) {
          window.localStorage.setItem('sharebase.nickname', data.nickname)
        }
        if (data.headline) {
          window.localStorage.setItem('sharebase.headline', data.headline)
        }
      }
    },
    logout() {
      window.localStorage.removeItem('sharebase.role')
      window.localStorage.removeItem('sharebase.nickname')
      window.localStorage.removeItem('sharebase.headline')
      this.profile = null
    }
  }
})
