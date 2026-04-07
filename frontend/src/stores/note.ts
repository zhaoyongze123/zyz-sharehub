import { defineStore } from 'pinia'

export const useNoteStore = defineStore('note', {
  state: () => ({
    keyword: '',
    selectedTab: 'latest',
    pageNum: 1,
    pageSize: 6,
    draftTitle: '',
    draftContent: ''
  })
})
