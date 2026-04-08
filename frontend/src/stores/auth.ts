import { defineStore } from 'pinia'
import { apiClient } from '@/api/client'
import type { ApiResponse, MeDto, StoredFileDto, UserProfileDto } from '@/api/types'

export interface UserProfile {
  id: number
  login: string
  name: string
  nickname: string
  role: 'guest' | 'user' | 'admin'
  avatarUrl?: string | null
  avatarFileId?: string | null
  status?: string
  headline?: string
}

export interface MeOverview extends Omit<MeDto, 'profile'> {
  profile: UserProfile
}

function normalizeProfile(payload: Partial<UserProfileDto & Record<string, any>>, roleFallback: UserProfile['role']): UserProfile {
  const role = (payload?.role ?? payload?.userRole ?? roleFallback ?? 'user') as UserProfile['role']
  const nickname = payload?.nickname ?? payload?.name ?? payload?.login ?? payload?.username ?? '未命名用户'
  const login = payload?.login ?? payload?.username ?? nickname

  return {
    id: Number(payload?.id ?? payload?.userId ?? 0),
    login,
    name: payload?.name ?? nickname,
    nickname,
    role: role === 'admin' || role === 'guest' ? role : 'user',
    avatarUrl: payload?.avatarUrl ?? payload?.avatar ?? payload?.avatar_url ?? payload?.photo ?? payload?.downloadUrl ?? payload?.file?.downloadUrl ?? null,
    avatarFileId: (payload as any)?.avatarFileId ?? (payload as any)?.avatar_file_id ?? null,
    status: payload?.status,
    headline: payload?.headline ?? payload?.bio ?? ''
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    loading: false,
    profile: null as UserProfile | null,
    overview: null as MeOverview | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.profile),
    isAdmin: (state) => state.profile?.role === 'admin'
  },
  actions: {
    setProfile(profile: UserProfile | null) {
      this.profile = profile
      if (profile) {
        window.localStorage.setItem('sharebase.role', profile.role)
        window.localStorage.setItem('sharebase.nickname', profile.nickname)
        window.localStorage.setItem('sharebase.userKey', profile.login)
        if (profile.headline) {
          window.localStorage.setItem('sharebase.headline', profile.headline)
        }
        if (profile.avatarUrl) {
          window.localStorage.setItem('sharebase.avatar', profile.avatarUrl)
        }
      }
    },
    restoreFromLocal() {
      const savedRole = window.localStorage.getItem('sharebase.role') as UserProfile['role'] | null
      const savedNickname = window.localStorage.getItem('sharebase.nickname')
      const savedHeadline = window.localStorage.getItem('sharebase.headline')
      const savedAvatar = window.localStorage.getItem('sharebase.avatar')
      if (savedRole === 'user' || savedRole === 'admin') {
        this.profile = {
          id: 0,
          login: savedNickname || 'local-user',
          name: savedNickname || 'local-user',
          nickname: savedNickname || 'local-user',
          role: savedRole,
          avatarUrl: savedAvatar,
          headline: savedHeadline || ''
        }
      }
    },
    applyMeData(me: MeDto) {
      const storedRole = (window.localStorage.getItem('sharebase.role') as UserProfile['role']) || 'user'
      const profile = normalizeProfile(me.profile, storedRole)
      this.profile = profile
      this.overview = { ...me, profile }

      window.localStorage.setItem('sharebase.userKey', profile.login)
      window.localStorage.setItem('sharebase.nickname', profile.nickname)
      if (!window.localStorage.getItem('sharebase.role')) {
        window.localStorage.setItem('sharebase.role', profile.role)
      }
    },
    async bootstrap(force = false) {
      if (this.loading || (this.initialized && !force)) {
        return
      }

      this.loading = true
      try {
        const response = await apiClient.get<ApiResponse<MeDto>>('/me')
        this.applyMeData(response.data.data)
      } catch (error) {
        this.profile = null
        this.overview = null
        this.restoreFromLocal()
      } finally {
        this.initialized = true
        this.loading = false
      }
    },
    async fetchProfile(force = false) {
      await this.bootstrap(force)
    },
    async loginAs(role: 'user' | 'admin') {
      window.localStorage.setItem('sharebase.role', role)
      window.localStorage.setItem('sharebase.userKey', role === 'admin' ? 'dev-admin-user' : 'dev-frontend-user')
      await this.bootstrap(true)
    },
    updateProfile(data: Partial<UserProfile>) {
      if (!this.profile) return
      this.profile = { ...this.profile, ...data }
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

      const resp = await apiClient.post<ApiResponse<StoredFileDto>>('/auth/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      const payload = resp.data?.data
      const downloadUrl = (payload as any)?.file?.downloadUrl || payload?.downloadUrl
      if (downloadUrl) {
        this.updateProfile({ avatarUrl: downloadUrl })
      }
      await this.bootstrap(true)
      return downloadUrl
    },
    logout() {
      window.localStorage.removeItem('sharebase.role')
      window.localStorage.removeItem('sharebase.nickname')
      window.localStorage.removeItem('sharebase.headline')
      window.localStorage.removeItem('sharebase.avatar')
      window.localStorage.removeItem('sharebase.userKey')
      this.profile = null
      this.overview = null
      this.initialized = false
    }
  }
})
