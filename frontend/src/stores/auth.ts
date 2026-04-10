import axios from 'axios'
import { defineStore } from 'pinia'
import { fetchMe, type MeData } from '@/api/me'

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
  }
}

function getSavedRole(): UserProfile['role'] {
  return window.localStorage.getItem('sharebase.role') === 'admin' ? 'admin' : 'user'
}

function buildDevHeaders() {
  const savedRole = window.localStorage.getItem('sharebase.role')
  const savedNickname = window.localStorage.getItem('sharebase.nickname')
  const headers: Record<string, string> = {}

  if (savedRole === 'admin') {
    headers['X-Admin-Token'] = window.localStorage.getItem('sharebase.adminToken') || 'dev-admin-token'
  }

  if (savedRole === 'user' || savedRole === 'admin') {
    headers['X-User-Key'] = window.localStorage.getItem('sharebase.userKey') || savedNickname || 'frontend-local-user'
  }

  return headers
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    profile: null as UserProfile | null,
    me: null as MeData | null,
    loading: false
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
        const role = getSavedRole()
        this.profile = {
          id: currentUser.id ?? 0,
          nickname,
          role,
          headline: window.localStorage.getItem('sharebase.headline') || 'ShareHub 用户',
          avatarUrl: currentUser.avatarUrl || undefined
        }
        window.localStorage.setItem('sharebase.nickname', nickname)
        window.localStorage.setItem('sharebase.userKey', currentUser.login)
        window.localStorage.setItem('sharebase.role', role)
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
      window.localStorage.setItem('sharebase.role', role)
      window.localStorage.setItem('sharebase.nickname', nickname)
      window.localStorage.setItem('sharebase.headline', headline)
      window.localStorage.setItem('sharebase.userKey', nickname)
      if (role === 'admin') {
        window.localStorage.setItem('sharebase.adminToken', window.localStorage.getItem('sharebase.adminToken') || 'dev-admin-token')
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
        if (data.nickname || data.login) {
          window.localStorage.setItem('sharebase.nickname', data.nickname || data.login || '')
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
      this.initialized = true
    }
  }
})
