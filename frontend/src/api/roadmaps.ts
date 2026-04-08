import { apiClient } from './client'
import type { BackendResponse, PageData } from './types'

export interface Roadmap {
  id: number
  title: string
  description?: string
  visibility?: string
  status?: string
}

export interface RoadmapNode {
  id: number
  parentId: number | null
  title: string
  orderNo?: number | null
  resourceId?: number | null
  noteId?: number | null
  children?: RoadmapNode[]
}

export interface RoadmapDetailResponse {
  roadmap: Roadmap
  nodes: RoadmapNode[]
  progress: Record<string, any>
}

export async function fetchRoadmapList(page = 1, pageSize = 20): Promise<PageData<Roadmap>> {
  const { data } = await apiClient.get<BackendResponse<PageData<Roadmap>>>('/roadmaps', {
    params: { page, pageSize }
  })
  return data.data
}

export async function fetchRoadmapDetail(id: string | number): Promise<RoadmapDetailResponse> {
  const { data } = await apiClient.get<BackendResponse<RoadmapDetailResponse>>(`/roadmaps/${id}`)
  return data.data
}

export async function createRoadmap(payload: { title: string; description?: string; visibility?: string; status?: string }) {
  const { data } = await apiClient.post<BackendResponse<Roadmap>>('/roadmaps', payload)
  return data.data
}

export async function addRoadmapNode(
  roadmapId: number,
  payload: { title: string; parentId?: number | null; orderNo?: number | null; resourceId?: number | null; noteId?: number | null }
) {
  const { data } = await apiClient.post<BackendResponse<RoadmapNode[]>>(`/roadmaps/${roadmapId}/nodes`, payload)
  return data.data
}

export async function updateRoadmapProgress(roadmapId: number, payload: Record<string, any>) {
  const { data } = await apiClient.post<BackendResponse<Record<string, any>>>(`/roadmaps/${roadmapId}/progress`, payload)
  return data.data
}
