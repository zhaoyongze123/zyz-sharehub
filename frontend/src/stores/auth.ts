import { defineStore } from 'pinia'
import { apiClient } from '@/api/client'

export interface UserProfile {
  id: number
  nickname: string
  role: 'guest' | 'user' | 'admin'
  headline?: string
  avatarUrl?: string
  website?: string
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

      await this.refreshProfile()
      this.initialized = true
    },
    normalizeProfile(raw: any): UserProfile {
      return {
        id: raw?.id ?? raw?.userId ?? 0,
        nickname: raw?.nickname ?? raw?.username ?? '未命名用户',
        role: (raw?.role as UserProfile['role']) ?? 'user',
        headline: raw?.headline ?? raw?.bio ?? '',
        avatarUrl: raw?.avatarUrl ?? raw?.avatar ?? raw?.avatar_url ?? '',
        website: raw?.website ?? raw?.site ?? ''
      }
    },
    hydrateFromLocal() {
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
      } else {
        this.profile = null
      }
    },
    async refreshProfile() {
      try {
        const response = await apiClient.get('/auth/me')
        const payload = response.data?.data ?? response.data
        if (payload) {
          this.profile = this.normalizeProfile(payload)
          if (this.profile.nickname) {
            window.localStorage.setItem('sharebase.nickname', this.profile.nickname)
          }
          if (this.profile.role) {
            window.localStorage.setItem('sharebase.role', this.profile.role)
          }
          if (this.profile.headline) {
            window.localStorage.setItem('sharebase.headline', this.profile.headline)
          }
          return this.profile
        }
      } catch (error) {
        // 若后端不可用，保持现有的联调假数据逻辑，避免前端完全失效
        this.hydrateFromLocal()
      }
      return this.profile
    },
    async loginAs(role: 'user' | 'admin') {
      window.localStorage.setItem('sharebase.role', role)
      await this.refreshProfile()
      if (!this.profile) {
        this.hydrateFromLocal()
      }
    },
    async updateProfile(data: Partial<UserProfile>) {
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
