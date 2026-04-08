import { defineStore } from 'pinia'
import { apiClient } from '@/api/client'

export interface UserProfile {
  id: number
  nickname: string
  role: 'guest' | 'user' | 'admin'
  headline?: string
  bio?: string
  website?: string
  avatarUrl?: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    loading: false,
    profile: null as UserProfile | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.profile),
    isAdmin: (state) => state.profile?.role === 'admin'
  },
  actions: {
    normalizeProfile(payload: any): UserProfile {
      const role = (payload?.role ?? payload?.userRole ?? 'user') as UserProfile['role']
      return {
        id: Number(payload?.id ?? payload?.userId ?? 0),
        nickname: payload?.nickname ?? payload?.name ?? payload?.username ?? '未命名用户',
        role: role === 'admin' || role === 'guest' ? role : 'user',
        headline: payload?.headline ?? payload?.bio ?? '',
        bio: payload?.bio ?? payload?.headline ?? '',
        website: payload?.website ?? payload?.blog ?? payload?.url ?? payload?.site ?? '',
        avatarUrl: payload?.avatarUrl ?? payload?.avatar ?? payload?.avatar_url ?? payload?.photo ?? payload?.downloadUrl ?? ''
      }
    },
    setProfile(profile: UserProfile | null) {
      this.profile = profile

      if (profile) {
        window.localStorage.setItem('sharebase.role', profile.role)
        window.localStorage.setItem('sharebase.nickname', profile.nickname)
        window.localStorage.setItem('sharebase.headline', profile.headline || '')
        if (profile.avatarUrl) {
          window.localStorage.setItem('sharebase.avatar', profile.avatarUrl)
        }
      }
    },
    restoreFromLocal() {
      const savedRole = window.localStorage.getItem('sharebase.role')
      if (savedRole === 'user' || savedRole === 'admin') {
        const savedNickname = window.localStorage.getItem('sharebase.nickname')
        const savedHeadline = window.localStorage.getItem('sharebase.headline')
        const savedAvatar = window.localStorage.getItem('sharebase.avatar')
        this.profile = {
          id: 1,
          nickname: savedNickname || (savedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen'),
          role: savedRole,
          headline: savedHeadline || (savedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者'),
          avatarUrl: savedAvatar || undefined
        }
      }
    },
    async bootstrap() {
      if (this.initialized) {
        return
      }

      await this.fetchProfile().catch(() => {
        // 本地兜底，便于离线或联调失败时维持旧数据
        this.restoreFromLocal()
      })
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
      if (!this.profile) return

      Object.assign(this.profile, data)
      if (data.nickname) {
        window.localStorage.setItem('sharebase.nickname', data.nickname)
      }
      if (data.headline) {
        window.localStorage.setItem('sharebase.headline', data.headline)
      }
      if (data.avatarUrl) {
        window.localStorage.setItem('sharebase.avatar', data.avatarUrl)
      }
    },
    async uploadAvatar(file: File) {
      const formData = new FormData()
      formData.append('file', file)

      const resp = await apiClient.post('/auth/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      const payload = resp.data?.data ?? resp.data
      const downloadUrl = payload?.file?.downloadUrl || payload?.downloadUrl || payload?.url
      if (downloadUrl) {
        this.updateProfile({ avatarUrl: downloadUrl })
      }
      return downloadUrl
    },
    async fetchProfile(force = false) {
      if (this.loading) return
      if (this.initialized && this.profile && !force) return

      this.loading = true
      try {
        const resp = await apiClient.get('/me')
        const raw = resp.data?.data ?? resp.data
        const profilePayload = raw?.profile ?? raw?.user ?? raw
        if (profilePayload) {
          const profile = this.normalizeProfile(profilePayload)
          this.setProfile(profile)
        } else {
          this.restoreFromLocal()
        }
      } finally {
        this.loading = false
      }
    },
    logout() {
      window.localStorage.removeItem('sharebase.role')
      window.localStorage.removeItem('sharebase.nickname')
      window.localStorage.removeItem('sharebase.headline')
      window.localStorage.removeItem('sharebase.avatar')
      this.profile = null
    }
  }
})
