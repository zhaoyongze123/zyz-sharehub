import { defineStore } from 'pinia'
import { fetchMe, type MeData } from '@/api/me'

export interface UserProfile {
  id: number
  login: string
  name?: string | null
  nickname?: string
  role?: 'guest' | 'user' | 'admin'
  headline?: string
  avatarUrl?: string | null
  avatarFileId?: string | null
  status?: string | null
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
    normalizeProfile(profile?: MeData['profile']): UserProfile | null {
      if (!profile) return null

      const role = (window.localStorage.getItem('sharebase.role') as UserProfile['role']) || 'user'
      const nickname = profile.name ?? profile.login

      const normalized: UserProfile = {
        id: profile.id,
        login: profile.login,
        name: profile.name ?? null,
        nickname,
        role,
        headline: this.profile?.headline ?? '',
        avatarUrl: profile.avatarUrl ?? null,
        avatarFileId: profile.avatarFileId ?? null,
        status: profile.status ?? null
      }

      window.localStorage.setItem('sharebase.nickname', normalized.nickname || normalized.login)
      window.localStorage.setItem('sharebase.userKey', normalized.login)
      if (!window.localStorage.getItem('sharebase.role')) {
        window.localStorage.setItem('sharebase.role', role)
      }

      return normalized
    },
    hydrateFromLocal() {
      const savedRole = window.localStorage.getItem('sharebase.role') as UserProfile['role']
      const savedNickname = window.localStorage.getItem('sharebase.nickname')
      const savedHeadline = window.localStorage.getItem('sharebase.headline')

      if (savedRole === 'user' || savedRole === 'admin') {
        this.profile = {
          id: 1,
          login: savedNickname || 'frontend-local-user',
          nickname: savedNickname || (savedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen'),
          role: savedRole,
          headline: savedHeadline || (savedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者')
        }
      }
    },
    async refreshMe() {
      const data = await fetchMe()
      this.me = data
      this.profile = this.normalizeProfile(data?.profile)
    },
    async bootstrap() {
      if (this.initialized) return

      this.loading = true
      try {
        const data = await fetchMe()
        this.me = data
        this.profile = this.normalizeProfile(data.profile)
      } catch (error) {
        this.hydrateFromLocal()
        console.warn('fetchMe failed, fallback to local profile', error)
      } finally {
        this.initialized = true
        this.loading = false
      }
    },
    loginAs(role: 'user' | 'admin') {
      const nickname = role === 'admin' ? 'Admin Zoe' : 'Alex Chen'
      const headline = role === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者'
      window.localStorage.setItem('sharebase.role', role)
      window.localStorage.setItem('sharebase.userKey', role === 'admin' ? 'dev-admin' : 'dev-user')
      this.profile = {
        id: 1,
        login: role === 'admin' ? 'admin.local' : 'user.local',
        nickname: role === 'admin' ? 'Admin Zoe' : 'Alex Chen',
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
      this.me = null
      this.initialized = true
    }
  }
})
