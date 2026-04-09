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

interface AdminUserDto {
  id: number
  login: string
  name: string | null
  status: string | null
}

interface AdminUserPageData {
  items: AdminUserDto[]
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

export interface AdminUserItem {
  id: number
  nickname: string
  login: string
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

function normalizeUserStatus(status: string | null) {
  if (status === 'ACTIVE') return '正常'
  if (status === 'BANNED') return '已封禁'
  return status?.trim() || '未知状态'
}

function normalizeUser(dto: AdminUserDto): AdminUserItem {
  return {
    id: dto.id,
    nickname: dto.name?.trim() || dto.login,
    login: dto.login,
    status: normalizeUserStatus(dto.status)
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

export async function fetchAdminUsers(page = 1, pageSize = 20) {
  const response = await apiClient.get<ApiResponse<AdminUserPageData>>('/admin/users', {
    params: {
      page,
      pageSize
    }
  })

  return {
    items: response.data.data.items.map(normalizeUser),
    total: response.data.data.total,
    page: response.data.data.page,
    pageSize: response.data.data.pageSize
  }
}

export async function banAdminUser(userId: number) {
  await apiClient.post(`/admin/users/${userId}/ban`)
}

export async function unbanAdminUser(userId: number) {
  await apiClient.post(`/admin/users/${userId}/unban`)
}
