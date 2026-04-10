import { apiClient } from '@/api/client'

interface BackendApiResponse<T> {
  success: boolean
  code: string
  data: T
  message: string
}

interface BackendPageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface NoteDTO {
  id: number
  title: string
  contentMd: string
  visibility: string | null
  status: string | null
}

export interface FetchNotesParams {
  page?: number
  pageSize?: number
  status?: string
}

export interface CreateNotePayload {
  title: string
  contentMd: string
  visibility?: string | null
  status?: string | null
}

export interface UpdateNotePayload extends CreateNotePayload {}

export async function fetchNotes(params: FetchNotesParams) {
  const response = await apiClient.get<BackendApiResponse<BackendPageResponse<NoteDTO>>>('/notes', {
    params
  })
  const data = response.data.data
  return {
    list: data.items || [],
    total: data.total ?? 0,
    pageNum: data.page ?? params.page ?? 1,
    pageSize: data.pageSize ?? params.pageSize ?? 10
  }
}

export async function createNote(payload: CreateNotePayload) {
  const response = await apiClient.post<BackendApiResponse<NoteDTO>>('/notes', payload)
  return response.data.data
}

export async function fetchNoteDetail(id: number) {
  const response = await apiClient.get<BackendApiResponse<NoteDTO>>(`/notes/${id}`)
  return response.data.data
}

export async function updateNote(id: number, payload: UpdateNotePayload) {
  const response = await apiClient.put<BackendApiResponse<NoteDTO>>(`/notes/${id}`, payload)
  return response.data.data
}

export async function deleteNote(id: number) {
  const response = await apiClient.delete<BackendApiResponse<string>>(`/notes/${id}`)
  return response.data.data
}
