import { defineStore } from 'pinia'

export const useResumeStore = defineStore('resume', {
  state: () => ({
    template: 'aurora',
    exporting: false
  })
})
