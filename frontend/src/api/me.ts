import { apiClient } from './client'
import type { ApiResponse } from './types'

interface UserProfileDto {
  id: number
  login: string
  name: string | null
  bio: string | null
  avatarUrl: string | null
  status: string | null
}

interface MeDto {
  profile: UserProfileDto | null
  myResourceCount: number | null
  myFavoriteCount: number | null
  myRoadmapCount: number | null
  myNoteCount: number | null
  myResumeCount: number | null
  recentResourceCount: number | null
  publishedResourceCount: number | null
  draftNoteCount: number | null
  generatedResumeCount: number | null
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

interface StoredFileDto {
  id: string
  downloadUrl: string
}

export interface MeDashboardData {
  profile: {
    id: number
    login: string
    displayName: string
    bio: string
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

export async function fetchMeDashboard(): Promise<MeDashboardData> {
  const [meResponse, resourcesResponse, notesResponse, resumesResponse] = await Promise.all([
    apiClient.get<ApiResponse<MeDto>>('/me'),
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
      bio: profile?.bio?.trim() || '',
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
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  return response.data.data
}

export async function updateMyProfile(payload: { displayName: string; bio: string }) {
  const response = await apiClient.put<ApiResponse<MeDto>>('/me/profile', payload)
  return response.data.data
}
