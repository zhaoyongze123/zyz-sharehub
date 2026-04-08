import { apiClient } from './client'
import type { BackendResponse, PageData } from './types'

export interface Roadmap {
  id: number
  title: string
  description?: string | null
  visibility?: string | null
  status?: string | null
  owner?: string | null
  stageCount?: number | null
  followers?: number | null
  category?: string | null
  tags?: string[]
}

export interface RoadmapNode {
  id: number
  title: string
  parentId?: number | null
  orderNo?: number | null
  resourceId?: number | null
  noteId?: number | null
  children?: RoadmapNode[]
  summary?: string | null
  description?: string | null
  tasks?: string[]
}

export interface RoadmapDetailResponse {
  roadmap: Roadmap
  nodes: RoadmapNode[]
  progress: Record<string, any>
}

export async function fetchRoadmaps(params: { page?: number; pageSize?: number } = {}): Promise<PageData<Roadmap>> {
  const { data } = await apiClient.get<BackendResponse<PageData<Roadmap>>>('/roadmaps', {
    params: { page: params.page ?? 1, pageSize: params.pageSize ?? 20 }
  })
  return data.data
}

export async function fetchRoadmapDetail(id: string | number): Promise<RoadmapDetailResponse> {
  const { data } = await apiClient.get<BackendResponse<RoadmapDetailResponse>>(`/roadmaps/${id}`)
  return data.data
}

export async function createRoadmap(payload: { title: string; description?: string | null; visibility?: string | null; status?: string | null }) {
  const { data } = await apiClient.post<BackendResponse<Roadmap>>('/roadmaps', payload)
  return data.data
}

export async function addRoadmapNode(
  roadmapId: number | string,
  payload: { title: string; parentId?: number | null; orderNo?: number | null; resourceId?: number | null; noteId?: number | null; description?: string | null }
) {
  const { data } = await apiClient.post<BackendResponse<RoadmapNode[]>>(`/roadmaps/${roadmapId}/nodes`, payload)
  return data.data
}

export async function updateRoadmapProgress(roadmapId: number | string, progress: number) {
  const { data } = await apiClient.post<BackendResponse<Record<string, any>>>(`/roadmaps/${roadmapId}/progress`, { progress })
  return data.data
}
