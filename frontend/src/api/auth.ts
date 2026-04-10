import { apiClient } from '@/api/client'

interface BackendApiResponse<T> {
  success: boolean
  code: string
  data: T
  message: string
}

export interface AuthUserProfileDto {
  id: number
  login: string
  name: string
  avatarFileId: string | null
  avatarUrl: string | null
  status: string
}

export interface StoredFileDto {
  id: string
  owner: string
  category: string
  referenceType: string
  referenceId: string
  filename: string
  contentType: string
  size: number
  checksum: string
  downloadUrl: string
  createdAt: string
}

export async function fetchCurrentUser() {
  const response = await apiClient.get<BackendApiResponse<AuthUserProfileDto>>('/auth/me')
  return response.data.data
}

export async function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<BackendApiResponse<StoredFileDto>>('/auth/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  return response.data.data
}
