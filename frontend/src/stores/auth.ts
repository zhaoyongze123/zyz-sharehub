import { defineStore } from 'pinia'
import { fetchMe, uploadAvatar, type MeDto, type UserProfileDto } from '@/api/me'
import { useAppStore } from '@/stores/app'

export interface UserProfile {
  id: number
  login: string
  name?: string | null
  avatarFileId?: string | null
  avatarUrl?: string | null
  status?: string | null
  // 兼容旧前端字段
  nickname?: string
  headline?: string
  role?: 'guest' | 'user' | 'admin'
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    profile: null as UserProfile | null,
    me: null as MeDto | null,
    loading: false
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
        this.me = null
      } finally {
        this.initialized = true
      }
    },
    hydrateFromMe(me: MeDto) {
      const { profile } = me
      this.me = me
      this.profile = this.mapProfile(profile)
    },
    async refreshProfile() {
      this.loading = true
      const appStore = useAppStore()
      try {
        const me = await fetchMe()
        this.hydrateFromMe(me)
      } catch (error) {
        this.profile = null
        this.me = null
        appStore.showToast('获取用户信息失败', '请检查登录或联调头 X-User-Key', 'error')
      } finally {
        this.loading = false
      }
    },
    mapProfile(profile: UserProfileDto): UserProfile {
      return {
        id: profile.id,
        login: profile.login,
        name: profile.name,
        avatarFileId: profile.avatarFileId ?? undefined,
        avatarUrl: profile.avatarUrl ?? undefined,
        status: profile.status ?? undefined,
        nickname: profile.name ?? profile.login,
        headline: profile.status ?? undefined,
        role: 'user'
      }
    },
    // 联调便捷登录，本地设置 userKey/adminToken 后即可访问 /api/me
    loginAs(role: 'user' | 'admin' = 'user') {
      const pseudoUserKey = role === 'admin' ? 'dev-admin' : 'dev-user'
      window.localStorage.setItem('sharebase.userKey', pseudoUserKey)
      if (role === 'admin') {
        window.localStorage.setItem('sharebase.adminToken', 'dev-admin-token')
      }
      this.refreshProfile()
    },
    // 兼容旧逻辑的占位方法，实际后端未提供更新接口
    updateProfile(data: Partial<UserProfile>) {
      if (this.profile) {
        Object.assign(this.profile, data)
      }
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
      this.me = null
    }
  }
})
