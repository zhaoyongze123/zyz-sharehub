export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface BackendResponse<T> {
  success: boolean
  code: string
  data: T
  message: string
}

export interface PageData<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}
