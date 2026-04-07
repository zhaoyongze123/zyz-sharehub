import { defineStore } from 'pinia'

export const useResourceStore = defineStore('resource', {
  state: () => ({
    keyword: '',
    category: '全部',
    sortBy: 'latest',
    pageNum: 1,
    pageSize: 6
  })
})
