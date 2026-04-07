export interface NoteItem {
  id: number
  title: string
  summary: string
  updatedAt: string
  status: string
  tags: string[]
  content: string
  outline: string[]
}

export const noteTags = ['全部', 'RAG', 'Agent', '面试', '复盘']

export const notes: NoteItem[] = [
  {
    id: 301,
    title: 'RAG 评测体系复盘',
    summary: '把检索、生成和业务指标拆层验证，避免一次引入过多变量。',
    updatedAt: '2026-04-07',
    status: '已发布',
    tags: ['RAG', '复盘'],
    content: '# RAG 评测体系复盘\n\n## 为什么拆层\n先分检索层、生成层、业务层，再分别收集失败样本。\n\n## 迭代方法\n先跑通基线，再增加 rerank、rewrite 和反馈学习。\n\n## 验收\n把失败样本沉淀回提示词、路由和知识库治理中。',
    outline: ['为什么拆层', '迭代方法', '验收']
  },
  {
    id: 302,
    title: 'Agent 系统设计答题框架',
    summary: '用于面试场景的需求拆解、状态机设计与故障兜底模板。',
    updatedAt: '2026-04-06',
    status: '草稿',
    tags: ['Agent', '面试'],
    content: '# Agent 系统设计\n\n## 目标\n明确问题边界和核心 SLA。\n\n## 架构\n采用计划、执行、复盘三段式。\n\n## 风险\n成本、时延和工具失败率。',
    outline: ['目标', '架构', '风险']
  },
  {
    id: 303,
    title: 'MCP 鉴权接入踩坑记录',
    summary: '记录 OAuth 回调、Token 续签与代理层配置的真实故障。',
    updatedAt: '2026-04-04',
    status: '已发布',
    tags: ['MCP', '复盘'],
    content: '# MCP 鉴权接入踩坑记录\n\n## 现象\n生产环境回调地址不一致。\n\n## 解决\n统一后端配置和前端回跳地址。',
    outline: ['现象', '解决']
  }
]

export const noteComments = [
  { author: 'Lina', createdAt: '今天', content: '这种拆层写法很适合做团队复盘模板。' }
]
