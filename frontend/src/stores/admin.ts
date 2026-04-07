import { defineStore } from 'pinia'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    reviewFilter: 'pending',
    reportFilter: 'open'
  })
})
