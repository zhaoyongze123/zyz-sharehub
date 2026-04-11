import { expect, test, type Page } from '@playwright/test'

const enabledModules = new Set(
  (process.env.PLAYWRIGHT_MODULES || 'admin,backend').split(',').map((item) => item.trim()).filter(Boolean)
)

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const userKey = process.env.PLAYWRIGHT_USER_KEY || 'playwright-user'
const adminUserKey = process.env.PLAYWRIGHT_ADMIN_USER_KEY || 'playwright-admin'

function shouldRun(name: string) {
  return enabledModules.has('all') || enabledModules.has(name)
}

async function loginAs(page: Page, role: 'user' | 'admin') {
  await page.addInitScript(({ selectedRole, currentUserKey }) => {
    window.localStorage.setItem('sharehub.role', selectedRole)
    window.localStorage.setItem('sharehub.nickname', selectedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen')
    window.localStorage.setItem('sharehub.headline', selectedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者')
    window.localStorage.setItem('sharehub.userKey', currentUserKey)
    window.localStorage.removeItem('sharehub.adminToken')
  }, { selectedRole: role, currentUserKey: role === 'admin' ? adminUserKey : userKey })
}

async function browserFetch(page: Page, path: string, headers: Record<string, string> = {}) {
  return page.evaluate(async ({ requestPath, requestHeaders }) => {
    const response = await fetch(requestPath, {
      headers: requestHeaders
    })
    const text = await response.text()
    let json = null
    try {
      json = text ? JSON.parse(text) : null
    } catch {
      json = null
    }
    return {
      ok: response.ok,
      status: response.status,
      text,
      json
    }
  }, {
    requestPath: path,
    requestHeaders: headers
  })
}

function isAdminApiGet(responseUrl: string, pathname: string) {
  return responseUrl.includes(`/api${pathname}`) || responseUrl.includes(pathname)
}

async function waitForAdminApiGet(page: Page, pathname: string) {
  const response = await page.waitForResponse((candidate) =>
    candidate.request().method() === 'GET' && isAdminApiGet(candidate.url(), pathname)
  )
  expect(response.ok()).toBeTruthy()
  return response
}

test('admin backend health 可用', async ({ request }) => {
  test.skip(!shouldRun('backend'))
  const response = await request.get(`${apiBaseUrl}/actuator/health`)
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  expect(body.status).toBe('UP')
})

test('后台专项 smoke', async ({ page }) => {
  test.skip(!shouldRun('admin'))
  await loginAs(page, 'admin')

  const dashboardReportsResponsePromise = waitForAdminApiGet(page, '/admin/reports')
  const dashboardAuditResponsePromise = waitForAdminApiGet(page, '/admin/audit-logs')
  const dashboardUsersResponsePromise = waitForAdminApiGet(page, '/admin/users')
  await page.goto('/admin')
  await Promise.all([
    dashboardReportsResponsePromise,
    dashboardAuditResponsePromise,
    dashboardUsersResponsePromise
  ])
  await expect(page.getByText('管理中心仪表盘')).toBeVisible()

  const reportsResponse = await browserFetch(page, '/api/admin/reports?page=1&pageSize=20', {
    'X-User-Key': adminUserKey
  })
  expect(reportsResponse.ok).toBeTruthy()
  expect(reportsResponse.status).toBe(200)
  expect(reportsResponse.json?.success).toBeTruthy()
  expect(Array.isArray(reportsResponse.json?.data?.items)).toBeTruthy()
  const reportItems = reportsResponse.json?.data?.items ?? []
  const openReports = reportItems.filter((item: { status?: string }) =>
    item.status === 'OPEN' || item.status === '待处理'
  )
  await expect(page.getByTestId('admin-dashboard-stat-open-reports')).toContainText(String(openReports.length))
  if (openReports[0]) {
    await expect(page.getByTestId('admin-dashboard-stat-top-target')).toContainText(
      `${openReports[0].targetType} #${openReports[0].targetId}`
    )
  } else {
    await expect(page.getByTestId('admin-dashboard-stat-top-target')).toContainText('暂无')
  }

  const auditResponse = await browserFetch(page, '/api/admin/audit-logs?page=1&pageSize=5', {
    'X-User-Key': adminUserKey
  })
  expect(auditResponse.ok).toBeTruthy()
  expect(auditResponse.status).toBe(200)
  expect(auditResponse.json?.success).toBeTruthy()
  expect(Array.isArray(auditResponse.json?.data?.items)).toBeTruthy()
  const auditItems = auditResponse.json?.data?.items ?? []
  const firstAuditLog = auditItems[0]
  const expectedLatestAction = firstAuditLog
    ? firstAuditLog.action
      .split('_')
      .filter(Boolean)
      .map((segment: string) => `${segment[0]}${segment.slice(1).toLowerCase()}`)
      .join(' ')
    : '暂无治理动作'
  await expect(page.getByTestId('admin-dashboard-stat-audit-actions-value')).toContainText(String(Math.min(auditItems.length, 5)))
  await expect(page.getByTestId('admin-dashboard-stat-audit-actions-detail')).toContainText(expectedLatestAction)

  const usersResponse = await browserFetch(page, '/api/admin/users?page=1&pageSize=20', {
    'X-User-Key': adminUserKey
  })
  expect(usersResponse.ok).toBeTruthy()
  expect(usersResponse.status).toBe(200)
  expect(usersResponse.json?.success).toBeTruthy()
  expect(Array.isArray(usersResponse.json?.data?.items)).toBeTruthy()
  const managedUsers = usersResponse.json?.data?.items ?? []
  const bannedUsers = managedUsers.filter((item: { status?: string }) =>
    item.status === 'BANNED' || item.status === '已封禁'
  )
  const expectedMeta = firstAuditLog
    ? `最近动作 ${expectedLatestAction} · 已加载 ${reportItems.length} 条举报`
    : `已加载 ${reportItems.length} 条举报 / ${managedUsers.length} 个用户`
  await expect(page.getByTestId('admin-dashboard-meta')).toContainText(expectedMeta)
  const userStatCard = page.getByTestId('admin-dashboard-stat-users')
  await expect(userStatCard).toContainText('后台可管用户')
  await expect(userStatCard).toContainText(`已封禁 ${bannedUsers.length}`)
  await expect(page.getByTestId('admin-dashboard-stat-users-value')).toContainText(String(managedUsers.length))

  const reviewsPageResponsePromise = waitForAdminApiGet(page, '/admin/reports')
  await page.goto('/admin/reviews')
  await reviewsPageResponsePromise
  await expect(page.getByRole('heading', { name: '内容审核' })).toBeVisible()
  await expect(page.getByRole('button', { name: '驳回未开放' })).toBeDisabled()
  const firstReview = reportItems[0]
  if (firstReview) {
    const reviewRow = page.getByTestId(`admin-reviews-row-${firstReview.id}`)
    await expect(reviewRow).toContainText(firstReview.reason)
    await expect(reviewRow).toContainText(firstReview.reporter)
    await expect(reviewRow).toContainText(`${firstReview.targetType} #${firstReview.targetId}`)
  } else {
    await expect(page.getByText('暂无审核数据')).toBeVisible()
  }

  const reportsPageResponsePromise = waitForAdminApiGet(page, '/admin/reports')
  await page.goto('/admin/reports')
  await reportsPageResponsePromise
  await expect(page.getByRole('heading', { name: '举报处理' })).toBeVisible()
  const firstReport = reportItems[0]
  if (firstReport) {
    const reportRow = page.getByTestId(`admin-report-row-${firstReport.id}`)
    await expect(reportRow).toContainText(firstReport.reason)
    await expect(reportRow).toContainText(firstReport.reporter)
    await expect(reportRow).toContainText(`${firstReport.targetType} #${firstReport.targetId}`)

    if (firstReport.status === 'OPEN') {
      const resolveResponsePromise = page.waitForResponse((response) =>
        response.url().includes(`/api/admin/reports/${firstReport.id}/resolve`) && response.request().method() === 'POST'
      )
      await page.getByTestId(`admin-report-resolve-${firstReport.id}`).click()
      const resolveResponse = await resolveResponsePromise
      expect(resolveResponse.ok()).toBeTruthy()
      const resolveBody = await resolveResponse.json()
      expect(resolveBody.success).toBeTruthy()
      await expect(reportRow).toContainText('已处理')
    }
  } else {
    await expect(page.getByText('暂无举报数据')).toBeVisible()
  }

  const auditPageResponsePromise = waitForAdminApiGet(page, '/admin/audit-logs')
  await page.goto('/admin/audit-logs')
  await auditPageResponsePromise
  await expect(page.getByRole('heading', { name: '审计日志' })).toBeVisible()
  if (firstAuditLog) {
    await expect(page.getByTestId(`admin-audit-log-row-${firstAuditLog.id}`)).toContainText(
      `${firstAuditLog.targetType} #${firstAuditLog.targetId}`
    )
  } else {
    await expect(page.getByText('暂无审计日志')).toBeVisible()
  }

  const usersPageResponsePromise = waitForAdminApiGet(page, '/admin/users')
  await page.goto('/admin/users')
  await usersPageResponsePromise
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
  const firstUser = usersResponse.json?.data?.items?.[0]
  if (firstUser) {
    await expect(page.locator('tbody tr').filter({ hasText: firstUser.login }).first()).toContainText(firstUser.login)
  } else {
    await expect(page.getByText('暂无用户数据')).toBeVisible()
  }
})

test('管理员拦截 smoke', async ({ page }) => {
  test.skip(!shouldRun('admin'))
  await loginAs(page, 'user')
  await page.goto('/admin')
  await expect(page.getByText('当前账号无权限访问')).toBeVisible()
})
