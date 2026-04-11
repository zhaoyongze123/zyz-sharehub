import { apiClient } from './client'
import type { ApiResponse } from './types'

interface UserProfileDto {
  id: number
  login: string
  name: string | null
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

export interface UpdateMyProfilePayload {
  name: string
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

export type MeSummaryData = Pick<MeDashboardData, 'profile' | 'stats'>

function normalizeCount(value: number | null | undefined) {
  return typeof value === 'number' ? value : 0
}

function formatDate(value: string | null | undefined) {
  return value ? value.slice(0, 10) : '未知时间'
}

function mapSummary(me: MeDto): MeSummaryData {
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
    }
  }
}

export async function fetchMeSummary(): Promise<MeSummaryData> {
  const response = await apiClient.get<ApiResponse<MeDto>>('/me')
  return mapSummary(response.data.data)
}

export async function fetchMyRecentResources(pageSize = 5) {
  const response = await apiClient.get<ApiResponse<PagedData<ResourceDto>>>('/me/resources', {
    params: { page: 1, pageSize }
  })
  return (response.data.data.items ?? []).map((item) => ({
    id: item.id,
    title: item.title,
    status: item.status?.trim() || 'UNKNOWN',
    visibility: item.visibility?.trim() || 'UNKNOWN',
    updatedAt: formatDate(item.updatedAt)
  }))
}

export async function fetchMyRecentNotes(pageSize = 5) {
  const response = await apiClient.get<ApiResponse<PagedData<NoteDto>>>('/me/notes', {
    params: { page: 1, pageSize }
  })
  return (response.data.data.items ?? []).map((item) => ({
    id: item.id,
    title: item.title,
    status: item.status?.trim() || 'UNKNOWN'
  }))
}

export async function fetchMyRecentResumes(pageSize = 5) {
  const response = await apiClient.get<ApiResponse<PagedData<ResumeDto>>>('/me/resumes', {
    params: { page: 1, pageSize }
  })
  return (response.data.data.items ?? []).map((item) => ({
    id: item.id,
    templateKey: item.templateKey,
    status: item.status?.trim() || 'UNKNOWN',
    fileName: item.fileName?.trim() || `resume-${item.id}.pdf`,
    updatedAt: formatDate(item.fileUpdatedAt)
  }))
}

export async function fetchMeDashboard(): Promise<MeDashboardData> {
  const [summary, recentResources, recentNotes, recentResumes] = await Promise.all([
    fetchMeSummary(),
    fetchMyRecentResources(5),
    fetchMyRecentNotes(5),
    fetchMyRecentResumes(5)
  ])

  return {
    profile: summary.profile,
    stats: summary.stats,
    recentResources,
    recentNotes,
    recentResumes
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

export async function updateMyProfile(payload: UpdateMyProfilePayload) {
  const response = await apiClient.put<ApiResponse<UserProfileDto>>('/me/profile', payload)
  return response.data.data
}

export async function deleteMyAvatar() {
  const response = await apiClient.delete<ApiResponse<UserProfileDto>>('/me/avatar')
  return response.data.data
}
