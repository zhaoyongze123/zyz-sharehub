export interface RoadmapItem {
  id: number
  title: string
  summary: string
  author: string
  level: string
  stageCount: number
  followers: string
  tags: string[]
}

export const roadmapTags = ['全部', 'Agent', 'RAG', 'Prompt', 'MLOps']

export const roadmaps: RoadmapItem[] = [
  {
    id: 201,
    title: 'Agent 工程师 30 天路线',
    summary: '从协议、工具链到工作流编排和线上监控，四阶段跑通一条完整主线。',
    author: 'Alex',
    level: '进阶',
    stageCount: 4,
    followers: '1.2k',
    tags: ['Agent', 'MCP']
  },
  {
    id: 202,
    title: 'RAG 系统设计路线图',
    summary: '覆盖检索、改写、排序、评测与反馈学习的系统性路径。',
    author: 'Mina',
    level: '中高阶',
    stageCount: 5,
    followers: '932',
    tags: ['RAG']
  },
  {
    id: 203,
    title: 'Prompt Engineering 入门',
    summary: '帮助新手快速建立高质量提示词结构和验证习惯。',
    author: 'Luna',
    level: '新手',
    stageCount: 3,
    followers: '2.1k',
    tags: ['Prompt']
  }
]

export const roadmapTimeline = [
  {
    title: '阶段 1：协议与接入',
    summary: '认识 GitHub OAuth、MCP server 通讯方式和最小鉴权。',
    tasks: ['完成登录回调闭环', '搭建最小可用工具服务']
  },
  {
    title: '阶段 2：工作流编排',
    summary: '把计划、执行、反馈拆进稳定状态机。',
    tasks: ['补重试与兜底逻辑', '记录 cost / latency / success rate']
  },
  {
    title: '阶段 3：知识沉淀',
    summary: '路线、资源和笔记互相连接，形成可复用体系。',
    tasks: ['输出复盘笔记', '挂接关联资料']
  }
]
