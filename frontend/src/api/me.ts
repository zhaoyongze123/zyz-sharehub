import { apiClient } from '@/api/client'

interface BackendApiResponse<T> {
  success: boolean
  data: T
  message?: string
  code?: string
}

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

export async function fetchMe() {
  const response = await apiClient.get<BackendApiResponse<MeData>>('/me')
  return response.data.data
}

export async function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post<BackendApiResponse<StoredFileDto>>(
    '/auth/avatar',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' }
    }
  )
  return response.data.data
}
