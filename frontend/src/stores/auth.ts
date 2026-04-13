import axios from 'axios'
import { defineStore } from 'pinia'

export interface UserProfile {
  id: number
  nickname: string
  role: 'guest' | 'user' | 'admin'
  headline: string
  avatarUrl?: string
}

interface AuthMeResponse {
  success: boolean
  data?: {
    id?: number
    login?: string
    name?: string | null
    avatarUrl?: string | null
    status?: string
    isAdmin?: boolean
  }
}

const DEV_MODE_KEY = 'ShareHub.devMode'

function buildDevHeaders() {
  const devMode = window.localStorage.getItem(DEV_MODE_KEY)
  const savedNickname = window.localStorage.getItem('ShareHub.nickname')
  const headers: Record<string, string> = {}

  if (devMode === 'admin') {
    headers['X-Admin-Token'] = window.localStorage.getItem('ShareHub.adminToken') || 'dev-admin-token'
  }

  if (devMode === 'user' || devMode === 'admin') {
    headers['X-User-Key'] = window.localStorage.getItem('ShareHub.userKey') || savedNickname || 'frontend-local-user'
  }

  return headers
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
    async syncProfileFromServer() {
      const response = await axios.get<AuthMeResponse>('/api/auth/me', {
        headers: buildDevHeaders()
      })
      const currentUser = response.data?.data

      if (response.data?.success && currentUser?.login) {
        const nickname = currentUser.name?.trim() || currentUser.login
        const role: UserProfile['role'] = currentUser.isAdmin ? 'admin' : 'user'
        this.profile = {
          id: currentUser.id ?? 0,
          nickname,
          role,
          headline: window.localStorage.getItem('ShareHub.headline') || 'ShareHub 用户',
          avatarUrl: currentUser.avatarUrl || undefined
        }
        window.localStorage.setItem('ShareHub.nickname', nickname)
        window.localStorage.setItem('ShareHub.userKey', currentUser.login)
        window.localStorage.removeItem(DEV_MODE_KEY)
        return this.profile
      }

      this.profile = null
      return null
    },
    async bootstrap() {
      if (this.initialized) {
        return
      }

      try {
        await this.syncProfileFromServer()
      } catch (error) {
        this.profile = null
      } finally {
        this.initialized = true
      }
    },
    loginAs(role: 'user' | 'admin') {
      const nickname = role === 'admin' ? 'Admin Zoe' : 'Alex Chen'
      const headline = role === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者'
      window.localStorage.setItem(DEV_MODE_KEY, role)
      window.localStorage.setItem('ShareHub.nickname', nickname)
      window.localStorage.setItem('ShareHub.headline', headline)
      window.localStorage.setItem('ShareHub.userKey', nickname)
      if (role === 'admin') {
        window.localStorage.setItem('ShareHub.adminToken', window.localStorage.getItem('ShareHub.adminToken') || 'dev-admin-token')
      }
      this.profile = {
        id: 1,
        nickname,
        role,
        headline
      }
      this.initialized = true
    },
    updateProfile(data: Partial<UserProfile>) {
      if (this.profile) {
        Object.assign(this.profile, data)
        if (data.nickname) {
          window.localStorage.setItem('ShareHub.nickname', data.nickname)
        }
        if (data.headline) {
          window.localStorage.setItem('ShareHub.headline', data.headline)
        }
      }
    },
    logout() {
      window.localStorage.removeItem(DEV_MODE_KEY)
      window.localStorage.removeItem('ShareHub.nickname')
      window.localStorage.removeItem('ShareHub.headline')
      window.localStorage.removeItem('ShareHub.userKey')
      window.localStorage.removeItem('ShareHub.adminToken')
      this.profile = null
      this.initialized = true
    }
  }
})
