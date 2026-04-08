import { defineStore } from 'pinia'
import { apiClient } from '@/api/client'

export interface UserProfile {
  id: number | null
  login: string
  name: string | null
  avatarFileId?: string | null
  avatarUrl?: string | null
  status?: string | null
  nickname?: string
  role?: 'guest' | 'user' | 'admin'
  headline?: string
}

export interface MeSummary {
  myResourceCount: number
  myFavoriteCount: number
  myRoadmapCount: number
  myNoteCount: number
  myResumeCount: number
  recentResourceCount: number
  publishedResourceCount: number
  draftNoteCount: number
  generatedResumeCount: number
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    profile: null as UserProfile | null,
    meSummary: null as MeSummary | null
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

      // 确保联调环境至少有一个 userKey，避免 /api/me 401
      if (!window.localStorage.getItem('sharebase.userKey')) {
        const fallback = window.localStorage.getItem('sharebase.nickname') || 'frontend-local-user'
        window.localStorage.setItem('sharebase.userKey', fallback)
      }

      try {
        await this.fetchMe()
      } catch (e) {
        // 保持为空，由路由守卫处理未登录场景
        this.profile = null
        this.meSummary = null
      } finally {
        this.initialized = true
      }
    },
    loginAs(role: 'user' | 'admin') {
      window.localStorage.setItem('sharebase.role', role)
      const devUserKey = role === 'admin' ? 'dev-admin' : 'dev-user'
      window.localStorage.setItem('sharebase.userKey', devUserKey)
      this.profile = {
        id: null,
        login: devUserKey,
        name: role === 'admin' ? 'Admin Zoe' : 'Alex Chen',
        nickname: role === 'admin' ? 'Admin Zoe' : 'Alex Chen',
        role,
        headline: role === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者',
        status: 'ACTIVE'
      }
    },
    async fetchMe() {
      const { data } = await apiClient.get('/me')
      const meData = data?.data
      if (meData?.profile) {
        this.profile = mapProfileFromDto(meData.profile)
      } else {
        this.profile = null
      }

      if (meData) {
        this.meSummary = {
          myResourceCount: meData.myResourceCount ?? 0,
          myFavoriteCount: meData.myFavoriteCount ?? 0,
          myRoadmapCount: meData.myRoadmapCount ?? 0,
          myNoteCount: meData.myNoteCount ?? 0,
          myResumeCount: meData.myResumeCount ?? 0,
          recentResourceCount: meData.recentResourceCount ?? 0,
          publishedResourceCount: meData.publishedResourceCount ?? 0,
          draftNoteCount: meData.draftNoteCount ?? 0,
          generatedResumeCount: meData.generatedResumeCount ?? 0
        }
      } else {
        this.meSummary = null
      }
    },
    updateProfile(data: Partial<UserProfile>) {
      if (this.profile) {
        Object.assign(this.profile, data)
      }
    },
    logout() {
      window.localStorage.removeItem('sharebase.role')
      window.localStorage.removeItem('sharebase.nickname')
      window.localStorage.removeItem('sharebase.headline')
      window.localStorage.removeItem('sharebase.userKey')
      window.localStorage.removeItem('sharebase.adminToken')
      this.profile = null
      this.meSummary = null
    }
  }
})

function mapProfileFromDto(dto: any): UserProfile {
  return {
    id: dto?.id ?? null,
    login: dto?.login ?? '',
    name: dto?.name ?? null,
    avatarFileId: dto?.avatarFileId ?? null,
    avatarUrl: dto?.avatarUrl ?? null,
    status: dto?.status ?? null,
    nickname: dto?.name ?? dto?.login ?? '',
    role: 'user'
  }
}
