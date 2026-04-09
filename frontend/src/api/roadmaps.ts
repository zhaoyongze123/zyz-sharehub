import { apiClient } from './client'
import type { ApiResponse } from './types'
import { fetchResourceDetail, type ResourceItem } from './resources'

export interface RoadmapItem {
  id: number
  title: string
  summary: string
  author: string
  level: string
  stageCount: number
  followers: string
  category: string
}

export interface RoadmapDetail {
  id: number
  title: string
  summary: string
  visibility: string
  status: string
  progressPercent: number
  timeline: RoadmapTimelineItem[]
  relatedResources: ResourceItem[]
}

export interface RoadmapTimelineItem {
  id: number
  title: string
  summary: string
  tasks: string[]
  resourceId: number | null
  noteId: number | null
}

interface CreateRoadmapPayload {
  title: string
  description: string
  visibility: 'PUBLIC' | 'PRIVATE'
  status: 'DRAFT' | 'PUBLISHED'
}

interface CreateRoadmapNodePayload {
  parentId?: number | null
  title: string
  orderNo: number
  resourceId?: number | null
  noteId?: number | null
}

interface RoadmapDto {
  id: number
  title: string
  description: string | null
  visibility: string | null
  status: string | null
}

interface RoadmapNodeTreeDto {
  id: number
  parentId: number | null
  title: string
  orderNo: number | null
  resourceId: number | null
  noteId: number | null
  children: RoadmapNodeTreeDto[] | null
}

interface RoadmapListData {
  items: RoadmapDto[]
  total: number
  page: number
  pageSize: number
}

interface RoadmapDetailData {
  roadmap: RoadmapDto
  nodes: RoadmapNodeTreeDto[]
  progress: Record<string, unknown> | null
}

function inferCategory(dto: RoadmapDto) {
  const text = `${dto.title} ${dto.description ?? ''}`.toLowerCase()
  if (text.includes('backend') || text.includes('java') || text.includes('spring')) return '后端'
  if (text.includes('frontend') || text.includes('vue') || text.includes('react')) return '前端'
  return '基础'
}

function normalizeRoadmap(dto: RoadmapDto, stageCount = 0): RoadmapItem {
  return {
    id: dto.id,
    title: dto.title,
    summary: dto.description?.trim() || '暂无路线简介',
    author: dto.visibility === 'PUBLIC' ? 'ShareHub' : '仅作者可见',
    level: dto.status?.trim() || 'PUBLISHED',
    stageCount,
    followers: '开放学习',
    category: inferCategory(dto)
  }
}

function flattenNodes(nodes: RoadmapNodeTreeDto[]): RoadmapNodeTreeDto[] {
  return nodes.flatMap((node) => [node, ...flattenNodes(node.children ?? [])])
}

function normalizeTimeline(nodes: RoadmapNodeTreeDto[]): RoadmapTimelineItem[] {
  return flattenNodes(nodes)
    .sort((left, right) => (left.orderNo ?? Number.MAX_SAFE_INTEGER) - (right.orderNo ?? Number.MAX_SAFE_INTEGER))
    .map((node) => ({
      id: node.id,
      title: node.title,
      summary: node.children?.length ? `包含 ${node.children.length} 个子节点` : '当前阶段暂未补充更多子任务',
      tasks: (node.children ?? [])
        .sort((left, right) => (left.orderNo ?? Number.MAX_SAFE_INTEGER) - (right.orderNo ?? Number.MAX_SAFE_INTEGER))
        .map((child) => child.title),
      resourceId: node.resourceId,
      noteId: node.noteId
    }))
}

async function fetchRelatedResourcesByNodes(nodes: RoadmapTimelineItem[]) {
  const resourceIds = [...new Set(nodes.map((node) => node.resourceId).filter((id): id is number => typeof id === 'number'))]

  if (!resourceIds.length) {
    return []
  }

  const results = await Promise.allSettled(resourceIds.map((id) => fetchResourceDetail(id)))
  return results.flatMap((result) => (result.status === 'fulfilled' ? [result.value] : []))
}

export async function fetchRoadmaps(query: { page: number, pageSize: number }) {
  const response = await apiClient.get<ApiResponse<RoadmapListData>>('/roadmaps', {
    params: {
      page: Math.max(1, query.page),
      pageSize: query.pageSize
    }
  })

  return {
    items: response.data.data.items.map((item) => normalizeRoadmap(item)),
    total: response.data.data.total,
    page: response.data.data.page,
    pageSize: response.data.data.pageSize
  }
}

export async function fetchRoadmapDetail(id: string | number): Promise<RoadmapDetail> {
  const response = await apiClient.get<ApiResponse<RoadmapDetailData>>(`/roadmaps/${id}`)
  const timeline = normalizeTimeline(response.data.data.nodes ?? [])
  const relatedResources = await fetchRelatedResourcesByNodes(timeline)
  const progressPercent = typeof response.data.data.progress?.percent === 'number'
    ? response.data.data.progress.percent
    : 0

  return {
    ...normalizeRoadmap(response.data.data.roadmap, timeline.length),
    visibility: response.data.data.roadmap.visibility?.trim() || 'PUBLIC',
    status: response.data.data.roadmap.status?.trim() || 'PUBLISHED',
    progressPercent,
    timeline,
    relatedResources
  }
}

export async function createRoadmap(payload: CreateRoadmapPayload) {
  const response = await apiClient.post<ApiResponse<RoadmapDto>>('/roadmaps', payload)
  return response.data.data
}

export async function addRoadmapNode(id: string | number, payload: CreateRoadmapNodePayload) {
  const response = await apiClient.post<ApiResponse<RoadmapNodeTreeDto[]>>(`/roadmaps/${id}/nodes`, payload)
  return response.data.data
}
