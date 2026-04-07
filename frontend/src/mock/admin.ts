export const adminStats = [
  { label: '待审核内容', value: '28', description: '资料、路线、笔记混合队列' },
  { label: '待处理举报', value: '9', description: '重点关注侵权与失效外链' },
  { label: '近 7 日新增用户', value: '416', description: '比上周提升 12%' }
]

export const reviewItems = [
  { id: 1, title: 'Agent 工程师 30 天路线', type: '路线', author: 'Alex', status: '待审核' },
  { id: 2, title: 'MCP 鉴权接入踩坑记录', type: '笔记', author: 'Mina', status: '待审核' }
]

export const reportItems = [
  { id: 11, reason: '外链失效', target: 'Prompt 入门资料包', reporter: 'User A', status: '待处理' },
  { id: 12, reason: '内容疑似侵权', target: '面试题合集', reporter: 'User B', status: '处理中' }
]

export const adminUsers = [
  { id: 21, nickname: 'Alex', role: 'user', status: '正常' },
  { id: 22, nickname: 'Admin Zoe', role: 'admin', status: '正常' },
  { id: 23, nickname: 'Luna', role: 'user', status: '已封禁' }
]

export const taxonomyItems = [
  { id: 31, name: 'RAG', type: '标签' },
  { id: 32, name: 'Agent', type: '标签' },
  { id: 33, name: '资料包', type: '分类' }
]
