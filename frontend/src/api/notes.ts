import { apiClient } from './client'
import type { ApiResponse, PageResult } from './types'

export interface NoteItemDto {
  id: number | string
  title: string
  summary?: string
  content?: string
  status?: string
  tags?: string[]
  outline?: string[]
  updatedAt?: string
  updated_at?: string
  createdAt?: string
  created_at?: string
}

export interface NoteListParams {
  page?: number
  pageSize?: number
  status?: string
}

function unwrap<T>(resp: any): T {
  if (resp?.data?.data !== undefined) return resp.data.data as T
  return resp?.data as T
}

export const fetchNoteList = (params: NoteListParams = {}) =>
  apiClient.get<ApiResponse<PageResult<NoteItemDto>>>('/notes', {
    params
  })

export async function fetchNotes(params: NoteListParams = {}) {
  const resp = await fetchNoteList(params)
  return unwrap<PageResult<NoteItemDto>>(resp)
}

export const fetchNoteDetail = (id: string | number) => apiClient.get<ApiResponse<NoteItemDto>>(`/notes/${id}`)
export async function fetchNote(id: string | number) {
  const resp = await fetchNoteDetail(id)
  return unwrap<NoteItemDto>(resp)
}

export const createNote = (payload: Partial<NoteItemDto>) => apiClient.post<ApiResponse<NoteItemDto>>('/notes', payload)
export async function createNoteAndUnwrap(payload: Partial<NoteItemDto>) {
  const resp = await createNote(payload)
  return unwrap<NoteItemDto>(resp)
}

export const updateNote = (id: string | number, payload: Partial<NoteItemDto>) =>
  apiClient.put<ApiResponse<NoteItemDto>>(`/notes/${id}`, payload)

export const deleteNote = (id: string | number) => apiClient.delete<ApiResponse<void>>(`/notes/${id}`)
