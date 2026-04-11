export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  msg?: string
  code?: number | string
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface UserProfileDto {
  id: number
  login: string
  name: string
  avatarFileId?: string | null
  avatarUrl?: string | null
  status: string
  isAdmin?: boolean
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
  draftNoteCount: number
  generatedResumeCount: number
}

export interface StoredFileDto {
  id: string
  owner: string
  category: 'AVATAR' | 'RESOURCE_ATTACHMENT' | 'RESUME_PDF'
  referenceType: string
  referenceId: string
  filename: string
  contentType: string
  size: number
  checksum: string
  downloadUrl: string
  createdAt: string
}
