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

type RawNoteDTO = NoteDTO & {
  created_at?: string | null
  updated_at?: string | null
  owner_key?: string
  owner_name?: string | null
  owner_avatar_url?: string | null
  is_official?: boolean
  is_pinned?: boolean
}

export interface NoteDTO {
  id: number
  title: string
  contentMd: string
  visibility: string | null
  status: string | null
  category?: string | null
  tags: string[]
  ownerKey?: string
  ownerName?: string | null
  ownerAvatarUrl?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  isOfficial?: boolean
  isPinned?: boolean
}

export interface RelatedNoteItem {
  id: number
  title: string
  summary: string
  updatedAt: string
  status: string
  category?: string | null
  tags: string[]
  ownerKey?: string
  ownerName?: string | null
  ownerAvatarUrl?: string | null
  favorites?: number
  favorited?: boolean
}

export interface NoteInteractionSummary {
  noteId: number
  favorites: number
  likes: number
  reports: number
}

interface NoteInteractionMutationData {
  noteId: number
  likes?: number
  favorites?: number
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
  category?: string | null
  tags?: string[]
  isPinned?: boolean
}

export interface UpdateNotePayload extends CreateNotePayload {}

function normalizeNoteDto(dto: RawNoteDTO): NoteDTO {
  return {
    ...dto,
    ownerKey: dto.ownerKey ?? dto.owner_key,
    ownerName: dto.ownerName ?? dto.owner_name,
    ownerAvatarUrl: dto.ownerAvatarUrl ?? dto.owner_avatar_url,
    createdAt: dto.createdAt ?? dto.created_at ?? null,
    updatedAt: dto.updatedAt ?? dto.updated_at ?? null,
    isOfficial: dto.isOfficial ?? dto.is_official ?? false,
    isPinned: dto.isPinned ?? dto.is_pinned ?? false,
    tags: Array.isArray(dto.tags) ? dto.tags.filter((tag) => Boolean(tag?.trim())) : []
  }
}

export async function fetchNotes(params: FetchNotesParams) {
  const response = await apiClient.get<BackendApiResponse<BackendPageResponse<RawNoteDTO>>>('/notes', {
    params
  })
  const data = response.data.data
  return {
    list: (data.items || []).map(normalizeNoteDto),
    total: data.total ?? 0,
    pageNum: data.page ?? params.page ?? 1,
    pageSize: data.pageSize ?? params.pageSize ?? 10
  }
}

export async function fetchCommunityNotes(params: FetchNotesParams) {
  const response = await apiClient.get<BackendApiResponse<BackendPageResponse<RawNoteDTO>>>('/notes/community', {
    params
  })
  const data = response.data.data
  return {
    list: (data.items || []).map(normalizeNoteDto),
    total: data.total ?? 0,
    pageNum: data.page ?? params.page ?? 1,
    pageSize: data.pageSize ?? params.pageSize ?? 10
  }
}

export async function createNote(payload: CreateNotePayload) {
  const response = await apiClient.post<BackendApiResponse<RawNoteDTO>>('/notes', payload)
  return normalizeNoteDto(response.data.data)
}

export async function fetchNoteDetail(id: number) {
  const response = await apiClient.get<BackendApiResponse<RawNoteDTO>>(`/notes/${id}`)
  return normalizeNoteDto(response.data.data)
}

export async function updateNote(id: number, payload: UpdateNotePayload) {
  const response = await apiClient.put<BackendApiResponse<RawNoteDTO>>(`/notes/${id}`, payload)
  return normalizeNoteDto(response.data.data)
}

export async function deleteNote(id: number) {
  const response = await apiClient.delete<BackendApiResponse<string>>(`/notes/${id}`)
  return response.data.data
}

export async function fetchRelatedNotes(id: number) {
  const response = await apiClient.get<BackendApiResponse<RelatedNoteItem[]>>(`/notes/${id}/related`)
  return (response.data.data || []).map((item) => ({
    ...item,
    updatedAt: item.updatedAt ? item.updatedAt.slice(0, 10) : '未知时间',
    status: item.status || '未标记',
    tags: Array.isArray(item.tags) ? item.tags : [],
    favorites: item.favorites ?? 0,
    favorited: item.favorited ?? false
  }))
}

export async function fetchNoteInteractions(id: number) {
  const response = await apiClient.get<BackendApiResponse<NoteInteractionSummary>>(`/notes/${id}/interactions`)
  return response.data.data
}

export async function likeNote(id: number) {
  const response = await apiClient.post<BackendApiResponse<NoteInteractionMutationData>>(`/notes/${id}/like`)
  return response.data.data
}

export async function favoriteNote(id: number) {
  const response = await apiClient.post<BackendApiResponse<NoteInteractionMutationData>>(`/notes/${id}/favorite`)
  return response.data.data
}

export async function unfavoriteNote(id: number) {
  const response = await apiClient.delete<BackendApiResponse<NoteInteractionMutationData>>(`/notes/${id}/favorite`)
  return response.data.data
}

async function fetchMeNotePage(path: string, params: FetchNotesParams = {}) {
  const response = await apiClient.get<BackendApiResponse<BackendPageResponse<RelatedNoteItem>>>(path, {
    params
  })
  const data = response.data.data
  return {
    list: (data.items || []).map((item) => ({
      ...item,
      updatedAt: item.updatedAt ? item.updatedAt.slice(0, 10) : '未知时间',
      status: item.status || '未标记',
      tags: Array.isArray(item.tags) ? item.tags : [],
      favorites: item.favorites ?? 0,
      favorited: item.favorited ?? false
    })),
    total: data.total ?? 0,
    pageNum: data.page ?? params.page ?? 1,
    pageSize: data.pageSize ?? params.pageSize ?? 10
  }
}

export async function fetchMyFavoriteNotes(params: FetchNotesParams = {}) {
  return fetchMeNotePage('/me/favorite-notes', params)
}

export async function fetchMyNoteHistory(params: FetchNotesParams = {}) {
  return fetchMeNotePage('/me/note-history', params)
}
