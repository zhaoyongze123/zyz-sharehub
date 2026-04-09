import { expect, test, type Page } from '@playwright/test'

const enabledModules = new Set(
  (process.env.PLAYWRIGHT_MODULES || 'public').split(',').map((item) => item.trim()).filter(Boolean)
)

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const userKey = process.env.PLAYWRIGHT_USER_KEY || 'playwright-user'
const adminToken = process.env.PLAYWRIGHT_ADMIN_TOKEN || 'dev-admin-token'

async function loginAs(page: Page, role: 'user' | 'admin') {
  await page.addInitScript((selectedRole) => {
    window.localStorage.setItem('sharebase.role', selectedRole)
    window.localStorage.setItem('sharebase.nickname', selectedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen')
    window.localStorage.setItem('sharebase.headline', selectedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者')
    window.localStorage.setItem('sharebase.userKey', 'playwright-user')
    if (selectedRole === 'admin') {
      window.localStorage.setItem('sharebase.adminToken', 'dev-admin-token')
    }
  }, role)
}

function shouldRun(name: string) {
  return enabledModules.has('all') || enabledModules.has(name)
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

test('backend health 可用', async ({ request }) => {
  test.skip(!shouldRun('backend'))
  const response = await request.get(`${apiBaseUrl}/actuator/health`)
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  expect(body.status).toBe('UP')
})

test('公开页 smoke', async ({ page }) => {
  test.skip(!shouldRun('public'))
  await page.goto('/')
  await expect(page.getByText('专为 Agent、MCP 与 RAG 开发者打造。极简记录，生成动态履历。')).toBeVisible()
  await expect(page.getByRole('link', { name: '开始使用' })).toBeVisible()

  await page.goto('/resources')
  await expect(page.locator('main').getByRole('heading', { name: '资料广场' })).toBeVisible()
})

test('资源模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('resources'))
  await page.goto('/resources')
  await expect(page.locator('main').getByRole('heading', { name: '资料广场' })).toBeVisible()

  const apiResponse = await browserFetch(page, '/api/resources?page=0&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.status).toBe(200)
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()
  expect(apiResponse.json?.data?.items.length).toBeGreaterThan(0)

  const resource = apiResponse.json.data.items[0] as {
    id: number
    title: string
    downloadCount: number
  }

  await expect(page.locator('article', { hasText: resource.title }).first()).toBeVisible()
  await page.locator('article', { hasText: resource.title }).first().getByRole('link', { name: '查看详情' }).click()
  await expect(page.getByRole('heading', { name: resource.title })).toBeVisible()
  await expect(page.getByText(`下载 ${resource.downloadCount}`)).toBeVisible()
})

test('路线模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('roadmaps'))
  await page.goto('/roadmaps')
  await expect(page.getByText('学习路线图')).toBeVisible()

  const apiResponse = await browserFetch(page, '/api/roadmaps?page=1&pageSize=5')
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.status).toBe(200)
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()
  expect(apiResponse.json?.data?.items.length).toBeGreaterThan(0)

  const roadmap = apiResponse.json.data.items[0] as {
    id: number
    title: string
    description?: string | null
  }

  const roadmapCard = page.locator('article', { hasText: roadmap.title }).first()
  await expect(roadmapCard).toBeVisible()
  if (roadmap.description?.trim()) {
    await expect(roadmapCard.getByText(roadmap.description.trim())).toBeVisible()
  }
  await roadmapCard.getByRole('link', { name: '进入路线' }).click()
  await expect(page.getByRole('heading', { name: roadmap.title })).toBeVisible()
  await expect(page.getByText('节点进度结构')).toBeVisible()
})

test('社区笔记模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('notes'))
  await page.goto('/community')
  await expect(page.getByText('AI 资源类别')).toBeVisible()

  const apiResponse = await browserFetch(page, '/api/me/notes?page=1&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.status).toBe(200)
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()
})

test('简历模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('resumes'))
  await loginAs(page, 'user')
  await page.goto('/resume')
  await expect(page.getByRole('button', { name: '导出 PDF' })).toBeVisible()
  await expect(page.getByTestId('resume-workbench-summary')).toContainText('累计')

  const apiResponse = await browserFetch(page, '/api/resumes?page=1&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.status).toBe(200)
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()

  const firstResume = apiResponse.json?.data?.items?.[0]
  if (firstResume) {
    await expect(page.getByText(firstResume.fileName || `resume-${firstResume.id}.pdf`).first()).toBeVisible()
    await expect(page.getByTestId(`resume-server-item-${firstResume.id}`)).toBeVisible()
  }
})

test('个人中心模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('profile'))
  await loginAs(page, 'user')
  await page.goto('/me')
  await expect(page.getByRole('heading', { name: '个人资料' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '个人帐户' })).toBeVisible()

  const meResponse = await browserFetch(page, '/api/me', {
    'X-User-Key': userKey
  })
  expect(meResponse.ok).toBeTruthy()
  expect(meResponse.status).toBe(200)
  expect(meResponse.json?.success).toBeTruthy()
  expect(meResponse.json?.data).toBeTruthy()
  expect(meResponse.json?.data?.profile?.login).toBeTruthy()
  await expect(page.getByText(`@${meResponse.json.data.profile.login}`)).toBeVisible()

  const resourcesResponse = await browserFetch(page, '/api/me/resources?page=1&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(resourcesResponse.ok).toBeTruthy()
  expect(Array.isArray(resourcesResponse.json?.data?.items)).toBeTruthy()
  if (resourcesResponse.json?.data?.items?.length) {
    await expect(page.getByRole('link', { name: resourcesResponse.json.data.items[0].title }).first()).toBeVisible()
  }

  const resumesResponse = await browserFetch(page, '/api/me/resumes?page=1&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(resumesResponse.ok).toBeTruthy()
  expect(Array.isArray(resumesResponse.json?.data?.items)).toBeTruthy()
  if (resumesResponse.json?.data?.items?.length) {
    const firstResume = resumesResponse.json.data.items[0]
    await expect(page.getByText(firstResume.fileName || `resume-${firstResume.id}.pdf`).first()).toBeVisible()
  }
})

test('后台模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('admin'))
  await loginAs(page, 'admin')
  await page.goto('/admin')
  await expect(page.getByText('管理中心仪表盘')).toBeVisible()

  const apiResponse = await browserFetch(page, '/api/admin/reports?page=1&pageSize=5', {
    'X-Admin-Token': adminToken
  })
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.status).toBe(200)
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()
})

test('管理员拦截 smoke', async ({ page }) => {
  test.skip(!shouldRun('admin'))
  await loginAs(page, 'user')
  await page.goto('/admin')
  await expect(page.getByText('当前账号无权限访问')).toBeVisible()
})
