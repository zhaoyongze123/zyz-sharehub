import { apiClient } from './client'
import type { ApiResponse } from './types'

interface ResumeDto {
  id: number
  templateKey: string | null
  status: string | null
  fileUrl: string | null
  fileName: string | null
  fileSize: number | null
  fileCreatedAt: string | null
  fileUpdatedAt: string | null
}

interface ResumeWorkbenchDto {
  total: number | null
  generatedCount: number | null
  templateBreakdown: Array<{
    templateKey: string | null
    count: number | null
  }> | null
  recentItems: ResumeDto[] | null
}

interface ResumePageData {
  items: ResumeDto[]
  total: number
  page: number
  pageSize: number
}

export interface ResumeItem {
  id: number
  templateKey: string
  status: string
  fileUrl: string
  fileName: string
  fileSize: number
  createdAt: string
  updatedAt: string
}

export interface ResumeWorkbenchData {
  total: number
  generatedCount: number
  templateBreakdown: Array<{
    templateKey: string
    count: number
  }>
  recentItems: ResumeItem[]
}

function formatDate(value: string | null | undefined) {
  return value ? value.slice(0, 16).replace('T', ' ') : '未知时间'
}

function normalizeResume(dto: ResumeDto): ResumeItem {
  return {
    id: dto.id,
    templateKey: dto.templateKey?.trim() || 'default',
    status: dto.status?.trim() || 'UNKNOWN',
    fileUrl: dto.fileUrl?.trim() || `/api/resumes/${dto.id}/download`,
    fileName: dto.fileName?.trim() || `resume-${dto.id}.pdf`,
    fileSize: dto.fileSize ?? 0,
    createdAt: formatDate(dto.fileCreatedAt),
    updatedAt: formatDate(dto.fileUpdatedAt)
  }
}

export async function fetchResumeWorkbench() {
  const response = await apiClient.get<ApiResponse<ResumeWorkbenchDto>>('/resumes/workbench')
  const data = response.data.data

  return {
    total: data.total ?? 0,
    generatedCount: data.generatedCount ?? 0,
    templateBreakdown: (data.templateBreakdown ?? []).map((item) => ({
      templateKey: item.templateKey?.trim() || 'default',
      count: item.count ?? 0
    })),
    recentItems: (data.recentItems ?? []).map(normalizeResume)
  } satisfies ResumeWorkbenchData
}

export async function fetchResumes(page = 1, pageSize = 10) {
  const response = await apiClient.get<ApiResponse<ResumePageData>>('/resumes', {
    params: { page, pageSize }
  })

  return {
    items: response.data.data.items.map(normalizeResume),
    total: response.data.data.total,
    page: response.data.data.page,
    pageSize: response.data.data.pageSize
  }
}

export async function generateResume(templateKey: string) {
  const response = await apiClient.post<ApiResponse<ResumeDto>>('/resumes/generate', {
    templateKey
  })
  return normalizeResume(response.data.data)
}

export async function deleteResume(id: number) {
  await apiClient.delete<ApiResponse<string>>(`/resumes/${id}`)
}

export async function downloadResume(id: number, fallbackName?: string) {
  const response = await apiClient.get<Blob>(`/resumes/${id}/download`, {
    responseType: 'blob'
  })

  const disposition = (response.headers['content-disposition'] || response.headers['Content-Disposition']) as string | undefined
  const matchedFileName = disposition?.match(/filename\*?=([^;]+)/i)?.[1]
  const sanitizedName = matchedFileName?.replace(/UTF-8''/i, '').replace(/['"]/g, '')
  const fileName = decodeURIComponent(sanitizedName ?? '').trim() || fallbackName?.trim() || `resume-${id}.pdf`

  return {
    blob: response.data,
    fileName
  }
}
