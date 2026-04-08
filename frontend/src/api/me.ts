import { apiClient } from '@/api/client'

interface BackendApiResponse<T> {
  success: boolean
  data: T
  message?: string
  code?: string
}

export interface MeDto {
  profile: {
    id: number
    login: string
    name?: string | null
    avatarFileId?: string | null
    avatarUrl?: string | null
    status?: string | null
  }
  myResourceCount: number
  myFavoriteCount: number
  myRoadmapCount: number
  myNoteCount: number
  myResumeCount: number
  recentResourceCount: number
  publishedResourceCount: number
  draftNoteCount: number
  generatedResumeCount: number
}

export async function fetchMe() {
  const response = await apiClient.get<BackendApiResponse<MeDto>>('/me')
  return response.data.data
}

export async function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  const response = await apiClient.post<BackendApiResponse<{ file: { downloadUrl: string } }>>(
    '/auth/avatar',
    form,
    {
      headers: { 'Content-Type': 'multipart/form-data' }
    }
  )
  return response.data.data?.file
}
