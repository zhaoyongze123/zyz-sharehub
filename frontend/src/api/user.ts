import { apiClient } from './client'

export interface ApiEnvelope<T> {
  success?: boolean
  code?: string
  data: T
  message?: string
}

export interface CurrentUserDto {
  id: number
  nickname: string
  role: 'guest' | 'user' | 'admin'
  headline?: string
  avatarUrl?: string
  username?: string
  website?: string
  bio?: string
}

export interface MeOverviewDto {
  profile: CurrentUserDto
  resourceCount?: number
  favoriteCount?: number
  roadmapCount?: number
  noteCount?: number
  resumeCount?: number
  weeklyNewResourceCount?: number
  publishedResourceCount?: number
  draftNoteCount?: number
  generatedResumeCount?: number
  [key: string]: unknown
}

export interface StoredFileDto {
  id: string
  downloadUrl?: string
  contentType?: string
  fileName?: string
}

export async function fetchCurrentUser() {
  const response = await apiClient.get<ApiEnvelope<CurrentUserDto>>('/auth/me')
  return response.data.data ?? (response.data as unknown as CurrentUserDto)
}

export async function fetchMeOverview() {
  const response = await apiClient.get<ApiEnvelope<MeOverviewDto>>('/me')
  return response.data.data ?? (response.data as unknown as MeOverviewDto)
}

export async function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<ApiEnvelope<StoredFileDto>>('/auth/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  return response.data.data
}
