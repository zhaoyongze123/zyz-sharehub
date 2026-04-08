import { defineStore } from 'pinia'
import { fetchCurrentUser } from '@/api/auth'

export interface UserProfile {
  id: number
  login?: string
  nickname: string
  role: 'guest' | 'user' | 'admin'
  headline?: string
  avatarUrl?: string | null
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

      try {
        const user = await fetchCurrentUser()
        this.profile = {
          id: user.id,
          login: user.login,
          nickname: user.name || user.login,
          role: 'user',
          headline: '',
          avatarUrl: user.avatarUrl
        }
        window.localStorage.setItem('sharebase.userKey', user.login)
        window.localStorage.setItem('sharebase.nickname', user.name || user.login)
        window.localStorage.setItem('sharebase.role', 'user')
      } catch (err) {
        // 未登录或其他错误时保持未初始化的访客状态
        this.profile = null
      } finally {
        this.initialized = true
      }
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
      window.localStorage.removeItem('sharebase.userKey')
      window.localStorage.removeItem('sharebase.adminToken')
      this.profile = null
    }
  }
})
