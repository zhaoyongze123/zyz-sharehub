import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

declare module 'axios' {
  // 允许在单个请求上控制是否静默处理 401
  export interface AxiosRequestConfig {
    skipAuthError?: boolean
  }
}

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15000
})

apiClient.interceptors.request.use((config) => {
  const adminToken = window.localStorage.getItem('sharebase.adminToken')
  const userKey = window.localStorage.getItem('sharebase.userKey') || window.localStorage.getItem('sharehub.userKey')

  config.headers = config.headers || {}

  if (adminToken) {
    config.headers['X-Admin-Token'] = adminToken
  }

  if (userKey) {
    config.headers['X-User-Key'] = userKey
  }

  // 其余身份依赖 OAuth Session Cookie

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response, config } = error
    const authStore = useAuthStore()
    const appStore = useAppStore()

    const skipAuthError = (config as typeof config & { skipAuthError?: boolean })?.skipAuthError

    if (response?.status === 401) {
      if (!skipAuthError) {
        authStore.logout()
        appStore.showToast('登录已失效', '请重新登录后继续操作', 'error')
      }
    } else if (response) {
      const message = (response.data as any)?.message || (response.data as any)?.msg || '服务暂时不可用，请稍后再试'
      appStore.showToast('请求失败', message, 'error')
    } else {
      appStore.showToast('网络异常', '请检查网络后重试', 'error')
    }

    return Promise.reject(error)
  }
)
