import { apiClient } from './client'
import type { ApiResponse } from './types'

export interface MeProfileDto {
  id: number
  login: string
  name?: string | null
  avatarFileId?: string | null
  avatarUrl?: string | null
  status?: string | null
}

export interface MeData {
  profile: MeProfileDto
  myResourceCount?: number
  myFavoriteCount?: number
  myRoadmapCount?: number
  myNoteCount?: number
  myResumeCount?: number
  recentResourceCount?: number
  publishedResourceCount?: number
  draftNoteCount?: number
  generatedResumeCount?: number
}

interface ResourceDto {
  id: number
  title: string
  status: string | null
  visibility: string | null
  updatedAt: string | null
}

interface NoteDto {
  id: number
  title: string
  status: string | null
}

interface ResumeDto {
  id: number
  templateKey: string
  status: string
  fileName: string | null
  fileUpdatedAt: string | null
}

interface PagedData<T> {
  items: T[]
}

export interface StoredFileDto {
  id: string
  owner?: string
  category?: string
  referenceType?: string
  referenceId?: string
  filename?: string
  contentType?: string
  size?: number
  checksum?: string
  downloadUrl?: string
}

export interface MeDashboardData {
  profile: {
    id: number
    login: string
    displayName: string
    avatarUrl?: string
    status: string
  }
  stats: {
    resources: number
    favorites: number
    roadmaps: number
    notes: number
    resumes: number
    recentResources: number
    publishedResources: number
    draftNotes: number
    generatedResumes: number
  }
  recentResources: Array<{
    id: number
    title: string
    status: string
    visibility: string
    updatedAt: string
  }>
  recentNotes: Array<{
    id: number
    title: string
    status: string
  }>
  recentResumes: Array<{
    id: number
    templateKey: string
    status: string
    fileName: string
    updatedAt: string
  }>
}

function normalizeCount(value: number | null | undefined) {
  return typeof value === 'number' ? value : 0
}

function formatDate(value: string | null | undefined) {
  return value ? value.slice(0, 10) : '未知时间'
}

export async function fetchMe() {
  const response = await apiClient.get<ApiResponse<MeData>>('/me')
  return response.data.data
}

export async function fetchMeDashboard(): Promise<MeDashboardData> {
  const [meResponse, resourcesResponse, notesResponse, resumesResponse] = await Promise.all([
    apiClient.get<ApiResponse<MeData>>('/me'),
    apiClient.get<ApiResponse<PagedData<ResourceDto>>>('/me/resources', {
      params: { page: 1, pageSize: 5 }
    }),
    apiClient.get<ApiResponse<PagedData<NoteDto>>>('/me/notes', {
      params: { page: 1, pageSize: 5 }
    }),
    apiClient.get<ApiResponse<PagedData<ResumeDto>>>('/me/resumes', {
      params: { page: 1, pageSize: 5 }
    })
  ])

  const me = meResponse.data.data
  const profile = me.profile

  return {
    profile: {
      id: profile?.id ?? 0,
      login: profile?.login ?? '',
      displayName: profile?.name?.trim() || profile?.login || '未命名用户',
      avatarUrl: profile?.avatarUrl || undefined,
      status: profile?.status?.trim() || 'ACTIVE'
    },
    stats: {
      resources: normalizeCount(me.myResourceCount),
      favorites: normalizeCount(me.myFavoriteCount),
      roadmaps: normalizeCount(me.myRoadmapCount),
      notes: normalizeCount(me.myNoteCount),
      resumes: normalizeCount(me.myResumeCount),
      recentResources: normalizeCount(me.recentResourceCount),
      publishedResources: normalizeCount(me.publishedResourceCount),
      draftNotes: normalizeCount(me.draftNoteCount),
      generatedResumes: normalizeCount(me.generatedResumeCount)
    },
    recentResources: (resourcesResponse.data.data.items ?? []).map((item) => ({
      id: item.id,
      title: item.title,
      status: item.status?.trim() || 'UNKNOWN',
      visibility: item.visibility?.trim() || 'UNKNOWN',
      updatedAt: formatDate(item.updatedAt)
    })),
    recentNotes: (notesResponse.data.data.items ?? []).map((item) => ({
      id: item.id,
      title: item.title,
      status: item.status?.trim() || 'UNKNOWN'
    })),
    recentResumes: (resumesResponse.data.data.items ?? []).map((item) => ({
      id: item.id,
      templateKey: item.templateKey,
      status: item.status?.trim() || 'UNKNOWN',
      fileName: item.fileName?.trim() || `resume-${item.id}.pdf`,
      updatedAt: formatDate(item.fileUpdatedAt)
    }))
  }
}

export async function uploadMyAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post<ApiResponse<StoredFileDto>>('/auth/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data.data
}
