import { defineStore } from 'pinia'

export const useRoadmapStore = defineStore('roadmap', {
  state: () => ({
    keyword: '',
    selectedTag: '全部',
    pageNum: 1,
    pageSize: 6
  })
})
