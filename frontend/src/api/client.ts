import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15000
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const authStore = useAuthStore()
    const appStore = useAppStore()

    if (error.response?.status === 401) {
      authStore.logout()
      appStore.showToast('登录已失效', '请重新登录后继续操作', 'error')
    } else {
      appStore.showToast('请求失败', error.response?.data?.msg ?? '服务暂时不可用，请稍后再试', 'error')
    }

    return Promise.reject(error)
  }
)
