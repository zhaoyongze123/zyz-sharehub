import { defineStore } from 'pinia'

type ToastType = 'success' | 'error' | 'info'

interface ToastPayload {
  id: number
  title: string
  message: string
  type: ToastType
}

interface DialogState {
  visible: boolean
  title: string
  description: string
}

type ThemeMode = '跟随系统' | '浅色模式' | '深色模式'

export const useAppStore = defineStore('app', {
  state: () => ({
    globalLoading: false,
    theme: (window.localStorage.getItem('ShareHub.theme') as ThemeMode) || '跟随系统',
    toasts: [] as ToastPayload[],
    dialog: {
      visible: false,
      title: '',
      description: ''
    } as DialogState,
    isMobileMenuOpen: false
  }),
  actions: {
    initTheme() {
      this.applyTheme(this.theme)
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (this.theme === '跟随系统') {
          this.applyTheme('跟随系统')
        }
      })
    },
    setTheme(newTheme: ThemeMode) {
      this.theme = newTheme
      window.localStorage.setItem('ShareHub.theme', newTheme)
      this.applyTheme(newTheme)
    },
    applyTheme(theme: ThemeMode) {
      const isDark = theme === '深色模式' || (theme === '跟随系统' && window.matchMedia('(prefers-color-scheme: dark)').matches)
      if (isDark) {
        document.documentElement.classList.add('dark')
        document.documentElement.setAttribute('data-theme', 'dark')
      } else {
        document.documentElement.classList.remove('dark')
        document.documentElement.setAttribute('data-theme', 'light')
      }
    },
    showToast(title: string, message: string, type: ToastType = 'success') {
      const toast = {
        id: Date.now(),
        title,
        message,
        type
      }
      this.toasts.push(toast)
      window.setTimeout(() => {
        this.toasts = this.toasts.filter((item) => item.id !== toast.id)
      }, 2400)
    },
    openDialog(title: string, description: string) {
      this.dialog = {
        visible: true,
        title,
        description
      }
    },
    closeDialog() {
      this.dialog.visible = false
    }
  }
})
