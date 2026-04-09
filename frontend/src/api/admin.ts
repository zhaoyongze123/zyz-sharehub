import { apiClient } from './client'
import type { ApiResponse } from './types'

interface AdminReportDto {
  id: number
  targetType: string | null
  targetId: number | null
  reason: string | null
  reporter: string | null
  status: string | null
}

interface AdminReportPageData {
  items: AdminReportDto[]
  total: number
  page: number
  pageSize: number
}

export interface AdminReportItem {
  id: number
  reason: string
  target: string
  reporter: string
  status: string
}

function normalizeReportStatus(status: string | null) {
  if (status === 'OPEN') return '待处理'
  if (status === 'RESOLVED') return '已处理'
  return status?.trim() || '未知状态'
}

function normalizeReport(dto: AdminReportDto): AdminReportItem {
  const targetType = dto.targetType?.trim() || 'UNKNOWN'
  const targetId = dto.targetId ?? '-'

  return {
    id: dto.id,
    reason: dto.reason?.trim() || '未填写原因',
    target: `${targetType} #${targetId}`,
    reporter: dto.reporter?.trim() || '未知举报人',
    status: normalizeReportStatus(dto.status)
  }
}

export async function fetchAdminReports(page = 1, pageSize = 20) {
  const response = await apiClient.get<ApiResponse<AdminReportPageData>>('/admin/reports', {
    params: {
      page,
      pageSize
    }
  })

  return {
    items: response.data.data.items.map(normalizeReport),
    total: response.data.data.total,
    page: response.data.data.page,
    pageSize: response.data.data.pageSize
  }
}
