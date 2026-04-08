import { apiClient } from './client'

export interface NoteDto {
  id: number
  title: string
  contentMd: string
  visibility?: string | null
  status?: string | null
}

export interface PageData<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface ApiEnvelope<T> {
  success: boolean
  code: string
  data: T
  message: string
}

export async function fetchNotes(params: { page?: number; pageSize?: number; status?: string }) {
  const { data } = await apiClient.get<ApiEnvelope<PageData<NoteDto>>>('/notes', { params })
  if (!data.success) {
    throw new Error(data.message || data.code || '加载笔记列表失败')
  }
  return data.data
}

export async function fetchNoteDetail(id: number | string) {
  const { data } = await apiClient.get<ApiEnvelope<NoteDto>>(`/notes/${id}`)
  if (!data.success) {
    throw new Error(data.message || data.code || '加载笔记详情失败')
  }
  return data.data
}

export async function createNote(payload: { title: string; contentMd: string; visibility?: string | null; status?: string | null }) {
  const { data } = await apiClient.post<ApiEnvelope<NoteDto>>('/notes', payload)
  if (!data.success) {
    throw new Error(data.message || data.code || '创建笔记失败')
  }
  return data.data
}
