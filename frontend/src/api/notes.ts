import { apiClient } from './client'
import type { ApiResponse, PageResult } from './types'

export interface NoteItemDto {
  id: number
  title: string
  summary?: string
  content?: string
  status?: string
  tags?: string[]
  outline?: string[]
  updatedAt?: string
  createdAt?: string
}

export interface NoteListParams {
  page?: number
  pageSize?: number
  status?: string
}

export const fetchNoteList = (params: NoteListParams = {}) =>
  apiClient.get<ApiResponse<PageResult<NoteItemDto>>>('/notes', {
    params
  })

export const fetchNoteDetail = (id: string | number) =>
  apiClient.get<ApiResponse<NoteItemDto>>(`/notes/${id}`)

export const createNote = (payload: Partial<NoteItemDto>) =>
  apiClient.post<ApiResponse<NoteItemDto>>('/notes', payload)

export const updateNote = (id: string | number, payload: Partial<NoteItemDto>) =>
  apiClient.put<ApiResponse<NoteItemDto>>(`/notes/${id}`, payload)

export const deleteNote = (id: string | number) => apiClient.delete<ApiResponse<void>>(`/notes/${id}`)
