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

const AUTH_CACHE_KEY = 'sharehub.authProfileCache'
const AUTH_CACHE_TTL_MS = 5 * 60 * 1000

interface CachedAuthProfile {
  expiresAt: number
  profile: UserProfile
}

function getSavedRole(): UserProfile['role'] {
  return window.localStorage.getItem('sharehub.role') === 'admin' ? 'admin' : 'user'
}

function buildDevHeaders() {
  const savedRole = window.localStorage.getItem('sharehub.role')
  const savedNickname = window.localStorage.getItem('sharehub.nickname')
  const headers: Record<string, string> = {}
  const savedAdminToken = window.localStorage.getItem('sharehub.adminToken')

  if (savedRole === 'admin' && savedAdminToken) {
    headers['X-Admin-Token'] = savedAdminToken
  }

  if (savedRole === 'user' || savedRole === 'admin') {
    headers['X-User-Key'] = window.localStorage.getItem('sharehub.userKey') || savedNickname || 'frontend-local-user'
  }

  return headers
}

function readCachedAuthProfile(): UserProfile | null {
  const raw = window.localStorage.getItem(AUTH_CACHE_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as CachedAuthProfile
    if (!parsed || typeof parsed.expiresAt !== 'number' || !parsed.profile) {
      return null
    }
    if (parsed.expiresAt < Date.now()) {
      return null
    }
    return parsed.profile
  } catch {
    return null
  }
}

function writeCachedAuthProfile(profile: UserProfile) {
  const payload: CachedAuthProfile = {
    expiresAt: Date.now() + AUTH_CACHE_TTL_MS,
    profile
  }
  window.localStorage.setItem(AUTH_CACHE_KEY, JSON.stringify(payload))
}

function clearCachedAuthProfile() {
  window.localStorage.removeItem(AUTH_CACHE_KEY)
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
        const role = currentUser.isAdmin ? 'admin' : getSavedRole()
        this.profile = {
          id: currentUser.id ?? 0,
          nickname,
          role,
          headline: window.localStorage.getItem('sharehub.headline') || 'ShareHub 用户',
          avatarUrl: currentUser.avatarUrl || undefined
        }
        writeCachedAuthProfile(this.profile)
        window.localStorage.setItem('sharehub.nickname', nickname)
        window.localStorage.setItem('sharehub.userKey', currentUser.login)
        window.localStorage.setItem('sharehub.role', role)
        return this.profile
      }

      this.profile = null
      return null
    },
    async bootstrap() {
      if (this.initialized) {
        return
      }

      const cachedProfile = readCachedAuthProfile()
      if (cachedProfile) {
        this.profile = cachedProfile
        this.initialized = true
        void this.syncProfileFromServer().catch(() => {
          // 缓存命中场景下后台刷新失败不阻塞页面进入
        })
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
      window.localStorage.setItem('sharehub.role', role)
      window.localStorage.setItem('sharehub.nickname', nickname)
      window.localStorage.setItem('sharehub.headline', headline)
      window.localStorage.setItem('sharehub.userKey', nickname)
      this.profile = {
        id: 1,
        nickname,
        role,
        headline
      }
      writeCachedAuthProfile(this.profile)
      this.initialized = true
    },
    updateProfile(data: Partial<UserProfile>) {
      if (this.profile) {
        Object.assign(this.profile, data)
        writeCachedAuthProfile(this.profile)
        if (data.nickname) {
          window.localStorage.setItem('sharehub.nickname', data.nickname)
        }
        if (data.headline) {
          window.localStorage.setItem('sharehub.headline', data.headline)
        }
      }
    },
    logout() {
      window.localStorage.removeItem('sharehub.role')
      window.localStorage.removeItem('sharehub.nickname')
      window.localStorage.removeItem('sharehub.headline')
      window.localStorage.removeItem('sharehub.userKey')
      window.localStorage.removeItem('sharehub.adminToken')
      clearCachedAuthProfile()
      this.profile = null
      this.initialized = true
    }
  }
})
