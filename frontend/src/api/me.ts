import { apiClient } from '@/api/client'

interface BackendApiResponse<T> {
  success: boolean
  code?: string
  data: T
  message?: string
}

export interface UserProfileDto {
  id: number
  login: string
  name?: string | null
  avatarFileId?: string | null
  avatarUrl?: string | null
  status?: string | null
}

export interface MeDto {
  profile: UserProfileDto
  myResourceCount: number
  myFavoriteCount: number
  myRoadmapCount: number
  myNoteCount: number
  myResumeCount: number
  recentResourceCount: number
  publishedResourceCount: number
  generatedResumeCount?: number
}

export interface StoredFileDto {
  id: string
  fileName?: string
  downloadUrl?: string
  contentType?: string
}

export async function fetchMe() {
  const res = await apiClient.get<BackendApiResponse<MeDto>>('/me', {
    // 避免 401 时弹出全局 toast 和强制登出
    skipAuthError: true
  })
  return res.data.data
}

export async function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await apiClient.post<BackendApiResponse<StoredFileDto>>('/auth/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

  return res.data.data
}
