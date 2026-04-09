import { apiClient } from '@/api/client'

interface ApiResponse<T> {
  success: boolean
  code: string
  data: T
  message: string
}

interface ReportDto {
  id: number
  targetType: string | null
  targetId: number | null
  reason: string | null
  reporter: string | null
  status: string | null
}

export interface CreateReportPayload {
  resourceId: number | string
  reason?: string
}

export interface ReportItem {
  id: number
  targetType: string
  targetId: number | null
  reason: string
  reporter: string
  status: string
}

function normalizeReport(dto: ReportDto): ReportItem {
  return {
    id: dto.id,
    targetType: dto.targetType?.trim() || 'RESOURCE',
    targetId: dto.targetId ?? null,
    reason: dto.reason?.trim() || '无',
    reporter: dto.reporter?.trim() || '未知举报人',
    status: dto.status?.trim() || 'OPEN'
  }
}

export async function createReport(payload: CreateReportPayload) {
  const response = await apiClient.post<ApiResponse<ReportDto>>('/reports', payload)
  return normalizeReport(response.data.data)
}
