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

export const useAppStore = defineStore('app', {
  state: () => ({
    globalLoading: false,
    toasts: [] as ToastPayload[],
    dialog: {
      visible: false,
      title: '',
      description: ''
    } as DialogState,
    isMobileMenuOpen: false
  }),
  actions: {
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
