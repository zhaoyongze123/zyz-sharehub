import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const userKey = process.env.PLAYWRIGHT_USER_KEY || 'playwright-user'

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

async function apiFetch(request: APIRequestContext, path: string, headers: Record<string, string> = {}) {
  const response = await request.get(`${apiBaseUrl}${path}`, {
    headers
  })
  const text = await response.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    json = null
  }
  return {
    ok: response.ok(),
    status: response.status(),
    text,
    json
  }
}

test.describe.configure({ mode: 'serial' })

test('公开页走查', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByText('专为 Agent、MCP 与 RAG 开发者打造。极简记录，生成动态履历。')).toBeVisible()
  await expect(page.getByRole('link', { name: '开始使用' })).toBeVisible()

  await page.goto('/resources')
  await expect(page.locator('main').getByRole('heading', { name: '资料广场' })).toBeVisible()

  await page.goto('/roadmaps')
  await expect(page.getByText('学习路线图')).toBeVisible()
})

test('资源模块走查', async ({ page, request }) => {
  const apiResponse = await apiFetch(request, '/api/resources?page=0&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()
  expect(apiResponse.json?.data?.items.length).toBeGreaterThan(0)

  const resource = apiResponse.json.data.items[0] as {
    id: number
    title: string
  }

  await page.goto('/resources')
  const resourceCard = page.locator('article', { hasText: resource.title }).first()
  await expect(resourceCard).toBeVisible()
  await expect(resourceCard.getByRole('link', { name: '查看详情' })).toBeVisible()
  await resourceCard.getByRole('link', { name: '查看详情' }).click()
  await expect(page).toHaveURL(new RegExp(`/resources/${resource.id}$`))
  await expect(page.getByRole('heading', { name: resource.title })).toBeVisible()
  await expect(page.getByRole('link', { name: '查看详情' }).first()).toBeVisible()
})

test('路线模块走查', async ({ page, request }) => {
  const apiResponse = await apiFetch(request, '/api/roadmaps?page=1&pageSize=5')
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()
  expect(apiResponse.json?.data?.items.length).toBeGreaterThan(0)

  const roadmap = apiResponse.json.data.items[0] as {
    id: number
    title: string
  }

  await page.goto('/roadmaps')
  const roadmapCard = page.locator('article', { hasText: roadmap.title }).first()
  await expect(roadmapCard).toBeVisible()
  await expect(roadmapCard.getByRole('link', { name: '进入路线' })).toBeVisible()
  await roadmapCard.getByRole('link', { name: '进入路线' }).click()
  await expect(page).toHaveURL(new RegExp(`/roadmaps/${roadmap.id}$`))
  await expect(page.getByRole('heading', { name: roadmap.title })).toBeVisible()
  await expect(page.getByText('节点进度结构')).toBeVisible()
})

test('笔记模块走查', async ({ page, request }) => {
  const noteTitle = `Walkthrough Note ${Date.now()}`
  const createResponse = await request.post(`${apiBaseUrl}/api/notes`, {
    headers: {
      'X-User-Key': userKey,
      'Content-Type': 'application/json'
    },
    data: {
      title: noteTitle,
      contentMd: `# ${noteTitle}\n\n这是 walkthrough 创建的真实正文。\n\n## 小节\nWalkthrough detail paragraph`,
      visibility: 'PUBLIC',
      status: 'PUBLISHED'
    }
  })
  expect(createResponse.ok()).toBeTruthy()
  const createBody = await createResponse.json()
  const noteId = createBody.data.id as number

  await loginAs(page, 'user')
  await page.goto('/community')
  await expect(page.getByText('AI 资源类别')).toBeVisible()

  await page.goto(`/notes/${noteId}`)
  await expect(page.locator('.detail-main').getByRole('heading', { name: noteTitle }).first()).toBeVisible()
  await expect(page.locator('.markdown-panel')).toContainText('这是 walkthrough 创建的真实正文。')
  await expect(page.locator('.outline')).toContainText('小节')
})

test('个人中心走查', async ({ page }) => {
  await loginAs(page, 'user')
  const authMeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/me') && response.request().method() === 'GET'
  )
  await page.goto('/auth/callback?redirect=/me')
  const authMeResponse = await authMeResponsePromise
  expect(authMeResponse.ok()).toBeTruthy()

  await expect(page).toHaveURL(/\/me$/)
  await expect(page.getByRole('heading', { name: '个人资料' })).toBeVisible()
  await expect(page.getByRole('button', { name: '资料编辑待接写接口' })).toBeDisabled()
  await expect(page.getByTestId('profile-avatar-upload')).toBeEnabled()

  await page.goto('/resume')
  await expect(page.getByRole('button', { name: '导出 PDF' })).toBeVisible()
  await expect(page.getByRole('button', { name: '导入简历 PDF' })).toBeVisible()
  await expect(page.getByTestId('resume-generate-button')).toBeVisible()
})

test('后台模块走查', async ({ page }) => {
  await loginAs(page, 'admin')
  await page.goto('/admin')
  await expect(page.getByText('管理中心仪表盘')).toBeVisible()
  await expect(page.getByTestId('admin-dashboard-stat-open-reports')).toBeVisible()
  await expect(page.getByTestId('admin-dashboard-stat-users')).toBeVisible()

  await page.goto('/admin/reports')
  await expect(page.getByRole('heading', { name: '举报处理' })).toBeVisible()

  await page.goto('/admin/reviews')
  await expect(page.getByRole('heading', { name: '内容审核' })).toBeVisible()

  await page.goto('/admin/users')
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()

  await page.goto('/admin/taxonomy')
  await expect(page.getByRole('heading', { name: '标签分类管理' })).toBeVisible()
  await expect(page.getByTestId('admin-taxonomy-summary')).toBeVisible()

  await loginAs(page, 'user')
  await page.goto('/admin')
  await expect(page.getByText('当前账号无权限访问')).toBeVisible()
})
