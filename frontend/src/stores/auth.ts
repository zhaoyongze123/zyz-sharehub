import { defineStore } from 'pinia'
import { apiClient } from '@/api/client'

export type UserRole = 'guest' | 'user' | 'admin'

export interface UserProfile {
  id: number | string | null
  login?: string
  name?: string | null
  avatarFileId?: string | null
  avatarUrl?: string | null
  status?: string | null
  nickname?: string
  role?: UserRole
  headline?: string
  username?: string | null
  website?: string | null
  bio?: string | null
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

function normalizeRole(role: unknown): UserRole {
  const value = String(role || '').toLowerCase()
  if (value.includes('admin')) return 'admin'
  if (value.includes('user')) return 'user'
  return 'guest'
}

function fallbackProfileFromLocal(): UserProfile | null {
  const savedRole = window.localStorage.getItem('sharebase.role')
  if (savedRole === 'user' || savedRole === 'admin') {
    const savedNickname = window.localStorage.getItem('sharebase.nickname')
    const savedHeadline = window.localStorage.getItem('sharebase.headline')
    return {
      id: 1,
      nickname: savedNickname || (savedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen'),
      role: normalizeRole(savedRole),
      headline: savedHeadline || (savedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者'),
      username: savedNickname || ''
    }
  }
  return null
}

function normalizeProfile(raw: any): UserProfile | null {
  if (!raw) return null
  return {
    id: raw.id ?? raw.userId ?? raw.uid ?? null,
    login: raw.login ?? raw.username,
    name: raw.name ?? raw.nickname ?? null,
    avatarFileId: raw.avatarFileId ?? null,
    avatarUrl: raw.avatarUrl ?? raw.avatar ?? (raw.avatarId ? `/api/files/${raw.avatarId}` : undefined),
    status: raw.status ?? null,
    nickname: raw.nickname ?? raw.name ?? raw.username ?? '未命名用户',
    role: normalizeRole(raw.role ?? raw.userRole ?? 'user'),
    headline: raw.headline ?? raw.bio ?? raw.intro ?? null,
    username: raw.username ?? raw.handle ?? raw.login ?? raw.nickname ?? null,
    website: raw.website ?? raw.site ?? raw.personalSite ?? null,
    bio: raw.bio ?? raw.headline ?? null
  }
}

function mapProfileFromDto(dto: any): UserProfile {
  return {
    id: dto?.id ?? null,
    login: dto?.login ?? dto?.username ?? undefined,
    name: dto?.name ?? dto?.nickname ?? null,
    avatarFileId: dto?.avatarFileId ?? null,
    avatarUrl: dto?.avatarUrl ?? (dto?.avatarFileId ? `/api/files/${dto.avatarFileId}` : undefined),
    status: dto?.status ?? null,
    nickname: dto?.nickname ?? dto?.name ?? dto?.login ?? '',
    role: normalizeRole(dto?.role ?? dto?.userRole ?? 'user'),
    headline: dto?.headline ?? dto?.bio ?? null,
    username: dto?.username ?? dto?.login ?? null,
    website: dto?.website ?? null,
    bio: dto?.bio ?? null
  }
}

function ensureUserKey() {
  if (!window.localStorage.getItem('sharebase.userKey')) {
    const fallback = window.localStorage.getItem('sharebase.nickname') || 'frontend-local-user'
    window.localStorage.setItem('sharebase.userKey', fallback)
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    loading: false,
    profile: null as UserProfile | null,
    meSummary: null as MeSummary | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.profile),
    isAdmin: (state) => state.profile?.role === 'admin'
  },
  actions: {
    async bootstrap(force = false) {
      if (this.initialized && !force) {
        return
      }
      ensureUserKey()
      try {
        await this.fetchMe()
      } catch (error) {
        console.warn('[auth] bootstrap fetchMe failed, fallback to local profile', error)
        this.profile = this.profile || fallbackProfileFromLocal()
        this.meSummary = null
      } finally {
        this.initialized = true
      }
    },
    async fetchProfile() {
      this.loading = true
      try {
        const response = await apiClient.get('/auth/me')
        const raw = response.data?.data ?? response.data
        const normalized = normalizeProfile(raw?.profile ?? raw?.data ?? raw)
        this.profile = normalized || fallbackProfileFromLocal()
      } catch (error) {
        console.warn('[auth] fetchProfile failed, fallback to local storage', error)
        this.profile = this.profile || fallbackProfileFromLocal()
      } finally {
        this.loading = false
      }
      return this.profile
    },
    async fetchMe() {
      this.loading = true
      try {
        const { data } = await apiClient.get('/me')
        const meData = data?.data ?? data

        if (meData?.profile) {
          this.profile = mapProfileFromDto(meData.profile)
        } else {
          this.profile = this.profile || fallbackProfileFromLocal()
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
      } catch (error) {
        console.warn('[auth] fetchMe failed, fallback to local storage', error)
        this.profile = this.profile || fallbackProfileFromLocal()
        this.meSummary = null
        throw error
      } finally {
        this.loading = false
      }
      return this.profile
    },
    loginAs(role: 'user' | 'admin') {
      const nickname = role === 'admin' ? 'Admin Zoe' : 'Alex Chen'
      const headline = role === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者'
      const devUserKey = role === 'admin' ? 'dev-admin' : 'dev-user'
      window.localStorage.setItem('sharebase.role', role)
      window.localStorage.setItem('sharebase.nickname', nickname)
      window.localStorage.setItem('sharebase.headline', headline)
      window.localStorage.setItem('sharebase.userKey', devUserKey)
      this.profile = {
        id: this.profile?.id ?? null,
        login: devUserKey,
        nickname,
        role,
        headline,
        username: devUserKey,
        status: 'ACTIVE'
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
    setProfile(profile: UserProfile | null) {
      this.profile = profile
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
