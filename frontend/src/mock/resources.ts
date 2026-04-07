export interface ResourceItem {
  id: number
  title: string
  summary: string
  author: string
  updatedAt: string
  category: string
  fileType: string
  tags: string[]
  likes: number
  favorites: number
  downloadCount: number
}

export const resourceCategories = ['全部', '资料包', '仓库模板', '面试题集', '课程总结']
export const resourceTags = ['RAG', 'MCP', 'Agent', 'OAuth', 'LangGraph', 'System Design']

export const resources: ResourceItem[] = [
  {
    id: 101,
    title: 'MCP Server 鉴权模板仓库',
    summary: '覆盖 OAuth、权限策略、审计日志和本地联调脚本的一套完整模板。',
    author: 'Luo',
    updatedAt: '2026-04-07',
    category: '仓库模板',
    fileType: 'Repo',
    tags: ['MCP', 'OAuth', 'Agent'],
    likes: 235,
    favorites: 612,
    downloadCount: 2341
  },
  {
    id: 102,
    title: 'RAG 评测基线与样本集',
    summary: '包含召回评测、答案一致性和失败样本回灌的完整基线模板。',
    author: 'Mina',
    updatedAt: '2026-04-06',
    category: '资料包',
    fileType: 'ZIP',
    tags: ['RAG', 'LangGraph'],
    likes: 198,
    favorites: 421,
    downloadCount: 1548
  },
  {
    id: 103,
    title: 'Agentic Coding 面试高频题集',
    summary: '整理系统设计、提示工程、智能体协作与工程化面试题。',
    author: 'Kiki',
    updatedAt: '2026-04-05',
    category: '面试题集',
    fileType: 'PDF',
    tags: ['Agent', 'System Design'],
    likes: 320,
    favorites: 701,
    downloadCount: 3124
  },
  {
    id: 104,
    title: 'Spring Boot + GitHub OAuth 接入清单',
    summary: '聚焦后端登录链路、回调地址和前后端对齐细节。',
    author: 'Zoe',
    updatedAt: '2026-04-04',
    category: '课程总结',
    fileType: 'Markdown',
    tags: ['OAuth', 'MCP'],
    likes: 128,
    favorites: 276,
    downloadCount: 998
  },
  {
    id: 105,
    title: '多 Agent 调度提示词库',
    summary: '覆盖计划、执行、验收、回滚和冲突处理的提示词模板。',
    author: 'Alex',
    updatedAt: '2026-04-03',
    category: '资料包',
    fileType: 'JSON',
    tags: ['Agent', 'MCP'],
    likes: 256,
    favorites: 518,
    downloadCount: 1670
  },
  {
    id: 106,
    title: '向量数据库选型与成本估算表',
    summary: '从吞吐、检索时延、冷启动和成本侧比较主流向量库。',
    author: 'Ivy',
    updatedAt: '2026-04-02',
    category: '资料包',
    fileType: 'Sheet',
    tags: ['RAG'],
    likes: 143,
    favorites: 287,
    downloadCount: 1208
  }
]

export const resourceComments = [
  { author: 'Mina', createdAt: '2 小时前', content: '模板里的审计字段设计很完整，适合直接联调。' },
  { author: 'Alex', createdAt: '昨天', content: '如果能补一份 Nginx 反代说明就更好了。' }
]
