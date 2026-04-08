import { apiClient } from '@/api/client'

interface BackendApiResponse<T> {
  success: boolean
  code: string
  data: T
  message: string
}

interface BackendPageResponse<T> {
  items?: T[]
  list?: T[]
  page?: number
  pageNum?: number
  pageSize?: number
  total?: number
}

export interface RoadmapListItem {
  id: number
  title: string
  summary?: string | null
  description?: string | null
  visibility?: string | null
  status?: string | null
  nodeCount?: number | null
  stageCount?: number | null
  completedNodeCount?: number | null
  progressPercent?: number | null
  ownerName?: string | null
  owner?: string | null
  tags?: string[]
  category?: string | null
  followers?: number | string | null
}

export type Roadmap = RoadmapListItem

export interface RoadmapNode {
  id?: number | string
  title: string
  summary?: string | null
  description?: string | null
  tasks?: string[]
  order?: number | null
  parentId?: number | null
  resourceId?: number | null
  noteId?: number | null
  children?: RoadmapNode[]
}

export interface RoadmapDetail extends RoadmapListItem {
  nodes?: RoadmapNode[]
  progress?: {
    progressPercent?: number
    completedNodeCount?: number
    completedNodeIds?: Array<number | string>
    [key: string]: any
  }
  relatedResources?: any[]
}

export interface CreateRoadmapPayload {
  title: string
  summary?: string | null
  description?: string | null
  visibility?: string | null
  status?: string | null
  nodes?: Array<{ title: string; summary?: string; description?: string }>
}

export interface AddRoadmapNodePayload {
  title: string
  parentId?: number | null
  orderNo?: number | null
  resourceId?: number | null
  noteId?: number | null
  description?: string | null
}

export interface UpdateRoadmapProgressPayload {
  progressPercent?: number
  completedNodeIds?: Array<number | string>
  progress?: number
}

function normalizeRoadmapItem(item: any): RoadmapListItem {
  return {
    id: Number(item?.id ?? 0),
    title: item?.title ?? '未命名路线',
    description: item?.description ?? item?.summary ?? '',
    summary: item?.summary ?? item?.description ?? '',
    visibility: item?.visibility ?? null,
    status: item?.status ?? null,
    nodeCount: item?.nodeCount ?? item?.stageCount ?? item?.nodes?.length ?? null,
    stageCount: item?.stageCount ?? item?.nodeCount ?? null,
    completedNodeCount: item?.completedNodeCount ?? item?.progress?.completedNodeCount ?? null,
    progressPercent: item?.progressPercent ?? item?.progress?.progressPercent ?? null,
    ownerName: item?.ownerName ?? item?.ownerNickname ?? item?.owner ?? null,
    owner: item?.owner ?? item?.ownerName ?? null,
    tags: item?.tags ?? item?.labels ?? [],
    category: item?.category ?? null,
    followers: item?.followers ?? item?.followerCount ?? item?.stats?.followers ?? null
  }
}

function normalizeNodes(nodes: any[] = []): RoadmapNode[] {
  return nodes.map((node, index) => ({
    id: node?.id ?? node?.nodeId ?? index,
    title: node?.title ?? node?.name ?? `阶段 ${index + 1}`,
    summary: node?.summary ?? node?.description ?? '',
    description: node?.description ?? node?.summary ?? '',
    tasks: node?.tasks ?? node?.checklist ?? node?.milestones ?? [],
    order: node?.order ?? node?.orderNo ?? index,
    parentId: node?.parentId ?? null,
    resourceId: node?.resourceId,
    noteId: node?.noteId,
    children: node?.children ? normalizeNodes(node.children) : undefined
  }))
}

function normalizeDetail(data: any): RoadmapDetail {
  const normalized = normalizeRoadmapItem(data?.roadmap ?? data)
  const nodes = normalizeNodes(data?.nodes ?? data?.stages ?? data?.nodes ?? [])
  const progress =
    data?.progress ??
    data?.roadmap?.progress ?? {
      progressPercent: normalized.progressPercent ?? undefined,
      completedNodeCount: normalized.completedNodeCount ?? undefined
    }

  return {
    ...normalized,
    nodes,
    progress,
    relatedResources: data?.relatedResources ?? data?.resources ?? []
  }
}

export async function fetchRoadmaps(params: { page?: number; pageSize?: number; status?: string } = {}) {
  const response = await apiClient.get<BackendApiResponse<BackendPageResponse<RoadmapListItem> | RoadmapListItem[]>>('/roadmaps', {
    params: { page: params.page ?? 1, pageSize: params.pageSize ?? 20, status: params.status }
  })

  const raw = response.data?.data
  const pageData: BackendPageResponse<RoadmapListItem> = Array.isArray(raw)
    ? { items: raw, total: raw.length }
    : raw || {}

  const list = (pageData.items ?? pageData.list ?? []).map((item) => normalizeRoadmapItem(item))

  return {
    list,
    total: pageData.total ?? list.length,
    page: pageData.page ?? pageData.pageNum ?? params.page ?? 1,
    pageSize: pageData.pageSize ?? params.pageSize ?? 20
  }
}

export async function fetchRoadmapDetail(id: number | string) {
  const response = await apiClient.get<BackendApiResponse<RoadmapDetail>>(`/roadmaps/${id}`)
  return normalizeDetail(response.data?.data ?? {})
}

export async function createRoadmap(payload: CreateRoadmapPayload) {
  const response = await apiClient.post<BackendApiResponse<RoadmapDetail>>('/roadmaps', payload)
  return normalizeDetail(response.data?.data ?? {})
}

export async function addRoadmapNode(roadmapId: number | string, payload: AddRoadmapNodePayload) {
  const response = await apiClient.post<BackendApiResponse<RoadmapNode[]>>(`/roadmaps/${roadmapId}/nodes`, payload)
  return (response.data?.data || []).map((node: any, index: number) => ({
    ...normalizeNodes([node])[0],
    order: node?.orderNo ?? node?.order ?? index
  }))
}

export async function updateRoadmapProgress(id: number | string, payload: UpdateRoadmapProgressPayload | number) {
  const body =
    typeof payload === 'number'
      ? { progress: payload }
      : {
          progress: payload.progress,
          progressPercent: payload.progressPercent,
          completedNodeIds: payload.completedNodeIds
        }

  await apiClient.post(`/roadmaps/${id}/progress`, body)
}
