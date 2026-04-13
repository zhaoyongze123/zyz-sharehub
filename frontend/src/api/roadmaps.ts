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
  enrollment: RoadmapEnrollment | null
}

export interface RoadmapEnrollment {
  roadmapId: number
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'QUIT'
  startedAt: string | null
  completedAt: string | null
}

export interface RoadmapTimelineItem {
  id: number
  title: string
  summary: string
  description: string
  tasks: string[]
  resourceId: number | null
  noteId: number | null
  attachments: RoadmapNodeAttachment[]
}

export interface RoadmapNodeAttachment {
  id: string
  filename: string
  contentType: string
  size: number
  downloadUrl: string
  createdAt: string | null
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
  description?: string
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
  description: string | null
  orderNo: number | null
  resourceId: number | null
  noteId: number | null
  attachments: RoadmapNodeAttachment[] | null
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
  enrollment: RoadmapEnrollment | null
}

const ROADMAP_PROGRESS_STORAGE_PREFIX = 'ShareHub.roadmapProgress.'

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
      description: node.description?.trim() || '',
      summary: node.children?.length ? `包含 ${node.children.length} 个子节点` : '当前阶段暂未补充更多子任务',
      tasks: (node.children ?? [])
        .sort((left, right) => (left.orderNo ?? Number.MAX_SAFE_INTEGER) - (right.orderNo ?? Number.MAX_SAFE_INTEGER))
        .map((child) => child.title),
      resourceId: node.resourceId,
      noteId: node.noteId,
      attachments: Array.isArray(node.attachments) ? node.attachments : []
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
    relatedResources,
    enrollment: response.data.data.enrollment ?? null
  }
}

export async function fetchMyEnrolledRoadmaps(query: { page: number; pageSize: number; status?: string }) {
  const response = await apiClient.get<ApiResponse<{
    items: Array<RoadmapItem & {
      visibility?: string
      status?: string
      nodeCount?: number
      completedNodeCount?: number
      progressPercent?: number
      enrollmentStatus?: string | null
      startedAt?: string | null
      completedAt?: string | null
    }>
    total: number
    page: number
    pageSize: number
  }>>('/me/roadmaps', {
    params: {
      page: Math.max(1, query.page),
      pageSize: query.pageSize,
      status: query.status?.trim() || undefined
    }
  })
  return response.data.data
}

export async function fetchMyAuthoredRoadmaps(query: { page: number; pageSize: number; status?: string }) {
  const response = await apiClient.get<ApiResponse<{
    items: Array<RoadmapItem & {
      visibility?: string
      status?: string
      nodeCount?: number
      completedNodeCount?: number
      progressPercent?: number
      enrollmentStatus?: string | null
      startedAt?: string | null
      completedAt?: string | null
    }>
    total: number
    page: number
    pageSize: number
  }>>('/me/authored-roadmaps', {
    params: {
      page: Math.max(1, query.page),
      pageSize: query.pageSize,
      status: query.status?.trim() || undefined
    }
  })
  return response.data.data
}

export async function enrollRoadmap(id: string | number) {
  const response = await apiClient.post<ApiResponse<RoadmapEnrollment>>(`/roadmaps/${id}/enrollment`)
  return response.data.data
}

export async function fetchRoadmapEnrollment(id: string | number) {
  const response = await apiClient.get<ApiResponse<RoadmapEnrollment | null>>(`/roadmaps/${id}/enrollment`)
  return response.data.data
}

export async function pauseRoadmapEnrollment(id: string | number) {
  const response = await apiClient.post<ApiResponse<RoadmapEnrollment>>(`/roadmaps/${id}/enrollment/pause`)
  return response.data.data
}

export async function resumeRoadmapEnrollment(id: string | number) {
  const response = await apiClient.post<ApiResponse<RoadmapEnrollment>>(`/roadmaps/${id}/enrollment/resume`)
  return response.data.data
}

export async function completeRoadmapEnrollment(id: string | number) {
  const response = await apiClient.post<ApiResponse<RoadmapEnrollment>>(`/roadmaps/${id}/enrollment/complete`)
  return response.data.data
}

function getRoadmapProgressStorageKey(roadmapId: string | number) {
  return `${ROADMAP_PROGRESS_STORAGE_PREFIX}${roadmapId}`
}

function normalizeVisitedNodeIds(nodeIds: number[], validNodeIds: number[]) {
  const validIdSet = new Set(validNodeIds)
  return [...new Set(nodeIds.filter((nodeId) => Number.isFinite(nodeId) && validIdSet.has(nodeId)))]
}

export function getVisitedRoadmapNodeIds(roadmapId: string | number, validNodeIds: number[]) {
  if (typeof window === 'undefined') {
    return []
  }

  try {
    const rawValue = window.localStorage.getItem(getRoadmapProgressStorageKey(roadmapId))
    if (!rawValue) {
      return []
    }

    const parsedValue = JSON.parse(rawValue)
    if (!Array.isArray(parsedValue)) {
      return []
    }

    const normalizedNodeIds = normalizeVisitedNodeIds(
      parsedValue.map((item) => Number(item)),
      validNodeIds
    )

    if (normalizedNodeIds.length !== parsedValue.length) {
      window.localStorage.setItem(
        getRoadmapProgressStorageKey(roadmapId),
        JSON.stringify(normalizedNodeIds)
      )
    }

    return normalizedNodeIds
  } catch {
    return []
  }
}

export function markRoadmapNodeVisited(
  roadmapId: string | number,
  nodeId: number,
  validNodeIds: number[]
) {
  if (typeof window === 'undefined') {
    return []
  }

  const normalizedNodeIds = normalizeVisitedNodeIds(
    [...getVisitedRoadmapNodeIds(roadmapId, validNodeIds), nodeId],
    validNodeIds
  )

  window.localStorage.setItem(
    getRoadmapProgressStorageKey(roadmapId),
    JSON.stringify(normalizedNodeIds)
  )

  return normalizedNodeIds
}

export function calculateRoadmapProgressPercent(totalNodeCount: number, visitedNodeCount: number) {
  if (!totalNodeCount) {
    return 0
  }

  return Math.round((visitedNodeCount / totalNodeCount) * 100)
}

export async function createRoadmap(payload: CreateRoadmapPayload) {
  const response = await apiClient.post<ApiResponse<RoadmapDto>>('/roadmaps', payload)
  return response.data.data
}

export async function addRoadmapNode(id: string | number, payload: CreateRoadmapNodePayload) {
  const response = await apiClient.post<ApiResponse<RoadmapNodeTreeDto[]>>(`/roadmaps/${id}/nodes`, payload)
  return response.data.data
}

export async function uploadRoadmapNodeAttachment(
  roadmapId: string | number,
  nodeId: string | number,
  file: File
) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<ApiResponse<RoadmapNodeAttachment>>(
    `/roadmaps/${roadmapId}/nodes/${nodeId}/attachments`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  )

  return response.data.data
}
