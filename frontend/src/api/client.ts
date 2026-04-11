import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15000
})

apiClient.interceptors.request.use((config) => {
  const savedRole = window.localStorage.getItem('sharehub.role')
  const savedNickname = window.localStorage.getItem('sharehub.nickname')
  const savedUserKey = window.localStorage.getItem('sharehub.userKey')
  const savedAdminToken = window.localStorage.getItem('sharehub.adminToken')

  if (savedRole === 'admin' && savedAdminToken) {
    config.headers['X-Admin-Token'] = savedAdminToken
  }

  if (savedUserKey) {
    config.headers['X-User-Key'] = savedUserKey
  } else if (savedRole === 'user' || savedRole === 'admin') {
    config.headers['X-User-Key'] = savedNickname || 'frontend-local-user'
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const authStore = useAuthStore()
    const appStore = useAppStore()
    const skipAuthError = (error.config as any)?.skipAuthError

    if (error.response?.status === 401 && !skipAuthError) {
      authStore.logout()
      appStore.showToast('登录已失效', '请重新登录后继续操作', 'error')
    } else if (!skipAuthError) {
      appStore.showToast('请求失败', error.response?.data?.msg ?? '服务暂时不可用，请稍后再试', 'error')
    }

    return Promise.reject(error)
  }
)
