import { defineStore } from 'pinia'
import { fetchMe, uploadAvatar, type MeDto } from '@/api/me'

export interface UserProfile {
  id: number
  nickname: string
  role: 'guest' | 'user' | 'admin'
  headline: string
  login?: string
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
      if (this.initialized) return

      try {
        const me = await fetchMe()
        this.hydrateFromMe(me)
      } catch (error: any) {
        // 未登录时后端返回 401，保持未登录态
        this.profile = null
      } finally {
        this.initialized = true
      }
    },
    hydrateFromMe(me: MeDto) {
      const { profile } = me
      this.profile = {
        id: profile.id,
        nickname: profile.name || profile.login,
        role: 'user',
        headline: '',
        login: profile.login,
        avatarUrl: profile.avatarUrl
      }
    },
    async refreshMe() {
      const me = await fetchMe()
      this.hydrateFromMe(me)
    },
    loginAs(role: 'user' | 'admin' = 'user') {
      // 保留本地联调入口：写入 userKey/adminToken，方便后端用 header 识别
      const pseudoUserKey = role === 'admin' ? 'dev-admin' : 'dev-user'
      window.localStorage.setItem('sharebase.userKey', pseudoUserKey)
      if (role === 'admin') {
        window.localStorage.setItem('sharebase.adminToken', 'dev-admin-token')
      }
      this.profile = {
        id: 0,
        nickname: role === 'admin' ? 'Admin Zoe' : 'Alex Chen',
        role,
        headline: role === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者',
        login: pseudoUserKey,
        avatarUrl: null
      }
    },
    async updateProfile(data: Partial<UserProfile>) {
      if (!this.profile) return
      // 前端暂未有更新接口，先本地更新
      Object.assign(this.profile, data)
    },
    async updateAvatar(file: File) {
      const stored = await uploadAvatar(file)
      if (this.profile) {
        this.profile.avatarUrl = stored.downloadUrl || this.profile.avatarUrl
      }
      return stored
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
