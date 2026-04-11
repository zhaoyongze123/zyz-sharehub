import { apiClient } from './client'
import type { ApiResponse } from './types'

export interface ResourceItem {
  id: number
  title: string
  summary: string
  externalUrl: string
  objectKey: string
  author: string
  updatedAt: string
  category: string
  fileType: string
  tags: string[]
  likes: number
  favorites: number
  downloadCount: number
}

interface ResourceDto {
  id: number
  title: string
  type: string | null
  category: string | null
  summary: string | null
  externalUrl?: string | null
  tags: string[] | null
  updatedAt: string | null
  author: string | null
  likes: number | null
  favorites: number | null
  downloadCount: number | null
  status?: string | null
  visibility?: string | null
  objectKey?: string | null
}

interface ResourcePageData {
  items: ResourceDto[]
  total: number
  page: number
  pageSize: number
}

export interface ResourceListQuery {
  page: number
  pageSize: number
  keyword?: string
  category?: string
  tag?: string
  sortBy?: 'latest' | 'hot'
}

export const resourceCategoryOptions = ['全部', 'PDF', 'Repo', 'Markdown', 'JSON', 'ZIP', 'Sheet']

interface StoredFileDto {
  id: string
  filename: string
  contentType: string
  downloadUrl: string
}

interface CreateResourcePayload {
  title: string
  type: string
  category: string
  summary: string
  tags: string[]
  visibility: 'PUBLIC' | 'PRIVATE'
  status: 'DRAFT' | 'PUBLISHED'
  externalUrl?: string
}

function normalizeResource(dto: ResourceDto): ResourceItem {
  const category = dto.category?.trim() || dto.type?.trim() || '未分类'

  return {
    id: dto.id,
    title: dto.title,
    summary: dto.summary?.trim() || '暂无简介',
    externalUrl: dto.externalUrl?.trim() || '',
    objectKey: dto.objectKey?.trim() || '',
    author: dto.author?.trim() || '未知作者',
    updatedAt: dto.updatedAt ? dto.updatedAt.slice(0, 10) : '未知时间',
    category,
    fileType: dto.type?.trim() || category,
    tags: Array.isArray(dto.tags) ? dto.tags.filter((tag) => Boolean(tag?.trim())) : [],
    likes: dto.likes ?? 0,
    favorites: dto.favorites ?? 0,
    downloadCount: dto.downloadCount ?? 0
  }
}

export async function fetchResources(query: ResourceListQuery) {
  const response = await apiClient.get<ApiResponse<ResourcePageData>>('/resources', {
    params: {
      page: Math.max(0, query.page),
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      category: query.category && query.category !== '全部' ? query.category : undefined,
      tag: query.tag && query.tag !== '全部' ? query.tag : undefined,
      sortBy: query.sortBy || 'latest'
    }
  })

  return {
    items: response.data.data.items.map(normalizeResource),
    total: response.data.data.total,
    page: response.data.data.page,
    pageSize: response.data.data.pageSize
  }
}

export async function fetchResourceDetail(id: string | number) {
  const response = await apiClient.get<ApiResponse<ResourceDto>>(`/resources/${id}`)
  return normalizeResource(response.data.data)
}

export async function fetchRelatedResources(id: string | number) {
  const response = await apiClient.get<ApiResponse<ResourceDto[]>>(`/resources/${id}/related`)
  return response.data.data.map(normalizeResource)
}

export async function createResource(payload: CreateResourcePayload) {
  const response = await apiClient.post<ApiResponse<ResourceDto>>('/resources', payload)
  return response.data.data
}

export async function uploadResourceAttachment(id: string | number, file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<ApiResponse<{ resourceId: number, file: StoredFileDto }>>(
    `/resources/${id}/attachment`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  )

  return response.data.data
}

export async function publishResource(id: string | number) {
  const response = await apiClient.post<ApiResponse<ResourceDto>>(`/resources/${id}/publish`)
  return response.data.data
}
