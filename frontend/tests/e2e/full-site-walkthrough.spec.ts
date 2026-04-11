import { expect, test, type APIRequestContext, type Page, type Response } from '@playwright/test'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const userKey = process.env.PLAYWRIGHT_USER_KEY || 'playwright-user'
const adminToken = process.env.PLAYWRIGHT_ADMIN_TOKEN || 'dev-admin-token'

async function loginAs(page: Page, role: 'user' | 'admin') {
  await page.addInitScript(({ selectedRole, adminTokenValue }) => {
    window.localStorage.setItem('sharehub.role', selectedRole)
    window.localStorage.setItem('sharehub.nickname', selectedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen')
    window.localStorage.setItem('sharehub.headline', selectedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者')
    window.localStorage.setItem('sharehub.userKey', 'playwright-user')
    if (selectedRole === 'admin') {
      window.localStorage.setItem('sharehub.adminToken', adminTokenValue)
    }
  }, { selectedRole: role, adminTokenValue: adminToken })
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

async function createPublishedNote(request: APIRequestContext, title: string) {
  const response = await request.post(`${apiBaseUrl}/api/notes`, {
    headers: {
      'X-User-Key': userKey,
      'Content-Type': 'application/json'
    },
    data: {
      title,
      contentMd: `# ${title}\n\n这是 walkthrough 创建的真实正文。\n\n## 小节\nWalkthrough detail paragraph`,
      visibility: 'PUBLIC',
      status: 'PUBLISHED'
    }
  })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  return body.data.id as number
}

async function waitForResponses(page: Page, predicate: (response: Response) => boolean, count: number) {
  return new Promise<Response[]>((resolve) => {
    const responses: Response[] = []
    const handler = (response: Response) => {
      if (!predicate(response)) {
        return
      }
      responses.push(response)
      if (responses.length >= count) {
        page.off('response', handler)
        resolve(responses)
      }
    }
    page.on('response', handler)
  })
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
  const noteId = await createPublishedNote(request, noteTitle)

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

test('后台模块走查', async ({ page, request }) => {
  const reportNoteTitle = `Walkthrough Admin Note ${Date.now()}`
  const reportReason = `Walkthrough 举报 ${Date.now()}`
  const noteId = await createPublishedNote(request, reportNoteTitle)
  const reportResponse = await request.post(`${apiBaseUrl}/api/reports`, {
    headers: {
      'X-User-Key': userKey,
      'Content-Type': 'application/json'
    },
    data: {
      targetType: 'NOTE',
      noteId,
      reason: reportReason
    }
  })
  expect(reportResponse.ok()).toBeTruthy()
  const reportBody = await reportResponse.json()
  const reportId = reportBody.data.id as number

  await loginAs(page, 'admin')
  await page.goto('/admin')
  await expect(page.getByText('管理中心仪表盘')).toBeVisible()
  await expect(page.getByTestId('admin-dashboard-stat-open-reports')).toBeVisible()
  await expect(page.getByTestId('admin-dashboard-stat-users')).toBeVisible()
  await expect(page.locator('main')).toContainText(`NOTE #${noteId}`)

  await page.goto('/admin/reports')
  await expect(page.getByRole('heading', { name: '举报处理' })).toBeVisible()
  await expect(page.locator('tbody')).toContainText(reportReason)
  await expect(page.locator('tbody')).toContainText(`NOTE #${noteId}`)

  await page.goto('/admin/reviews')
  await expect(page.getByRole('heading', { name: '内容审核' })).toBeVisible()
  await expect(page.getByRole('button', { name: '驳回未开放' })).toBeDisabled()
  await expect(page.getByTestId(`admin-reviews-row-${reportId}`)).toContainText(reportReason)
  const resolveResponsePromise = page.waitForResponse((response) =>
    response.url().includes(`/api/admin/reports/${reportId}/resolve`) &&
    response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '完成处理' }).click()
  const resolveResponse = await resolveResponsePromise
  expect(resolveResponse.ok()).toBeTruthy()
  await expect(page.getByTestId(`admin-reviews-row-${reportId}`)).toContainText('已处理')

  await page.goto('/admin/audit-logs')
  await expect(page.getByRole('heading', { name: '审计日志' })).toBeVisible()
  await expect(page.locator('main')).toContainText('关联对象')
  await expect(page.locator('tbody')).toContainText(`REPORT #${reportId}`)
  await expect(page.locator('tbody')).toContainText('Resolve Report')

  await page.goto('/admin/users')
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()

  await page.goto('/admin/taxonomy')
  await expect(page.getByRole('heading', { name: '标签分类管理' })).toBeVisible()
  await expect(page.getByTestId('admin-taxonomy-summary')).toBeVisible()

  await loginAs(page, 'user')
  await page.goto('/admin')
  await expect(page.getByText('当前账号无权限访问')).toBeVisible()
})

test('发布页走查', async ({ page, request }) => {
  await loginAs(page, 'user')

  const resourceTitle = `Walkthrough Resource ${Date.now()}`
  await page.goto('/publish/resource')
  await page.getByTestId('publish-resource-title').fill(resourceTitle)
  await page.getByTestId('publish-resource-tags').fill('walkthrough,resource')
  await page.getByTestId('publish-resource-summary').fill('通过全站走查验证资料真实发布闭环。')
  await page.getByTestId('publish-resource-file').setInputFiles({
    name: 'walkthrough-guide.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('walkthrough resource file')
  })

  const resourceCreateResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/resources') &&
    response.request().method() === 'POST' &&
    !response.url().includes('/publish') &&
    !response.url().includes('/attachment')
  )
  const resourceAttachmentResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/attachment') &&
    response.request().method() === 'POST'
  )
  const resourcePublishResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/publish') &&
    response.request().method() === 'POST'
  )

  await page.getByTestId('publish-resource-submit').click()

  const resourceCreateResponse = await resourceCreateResponsePromise
  expect(resourceCreateResponse.ok()).toBeTruthy()
  const resourceCreateBody = await resourceCreateResponse.json()
  const resourceId = resourceCreateBody.data.id as number

  const resourceAttachmentResponse = await resourceAttachmentResponsePromise
  expect(resourceAttachmentResponse.ok()).toBeTruthy()

  const resourcePublishResponse = await resourcePublishResponsePromise
  expect(resourcePublishResponse.ok()).toBeTruthy()

  await expect(page.getByTestId('publish-resource-result')).toContainText(`资源 ID：${resourceId}`)
  await page.getByRole('link', { name: '查看详情页' }).click()
  await expect(page).toHaveURL(new RegExp(`/resources/${resourceId}$`))
  await expect(page.getByRole('heading', { name: resourceTitle })).toBeVisible()

  const roadmapTitle = `Walkthrough Roadmap ${Date.now()}`
  await page.goto('/publish/roadmap')
  await expect(page.getByText('当前真实接口仅写入节点标题和顺序。')).toBeVisible()
  await page.getByTestId('publish-roadmap-title').fill(roadmapTitle)
  await page.getByTestId('publish-roadmap-summary').fill('通过全站走查验证路线创建与节点追加闭环。')
  await page.getByTestId('publish-roadmap-node-title-0').fill('阶段 1：创建主体')
  await page.getByTestId('publish-roadmap-node-title-1').fill('阶段 2：追加节点')

  const roadmapCreateResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/roadmaps') &&
    response.request().method() === 'POST' &&
    !response.url().includes('/nodes')
  )
  const nodeResponsesPromise = waitForResponses(page, (response) =>
    response.url().includes('/nodes') &&
    response.request().method() === 'POST'
  , 2)

  await page.getByTestId('publish-roadmap-submit').click()

  const roadmapCreateResponse = await roadmapCreateResponsePromise
  expect(roadmapCreateResponse.ok()).toBeTruthy()
  const roadmapCreateBody = await roadmapCreateResponse.json()
  const roadmapId = roadmapCreateBody.data.id as number

  const nodeResponses = await nodeResponsesPromise
  expect(nodeResponses).toHaveLength(2)
  for (const nodeResponse of nodeResponses) {
    expect(nodeResponse.url()).toContain(`/api/roadmaps/${roadmapId}/nodes`)
    expect(nodeResponse.ok()).toBeTruthy()
  }

  await expect(page.getByTestId('publish-roadmap-result')).toContainText(`路线 ID：${roadmapId}`)
  await page.getByRole('link', { name: '查看详情页' }).click()
  await expect(page).toHaveURL(new RegExp(`/roadmaps/${roadmapId}$`))
  await expect(page.getByRole('heading', { name: roadmapTitle })).toBeVisible()

  const roadmapDetailResponse = await apiFetch(request, `/api/roadmaps/${roadmapId}`)
  expect(roadmapDetailResponse.ok).toBeTruthy()
  expect(roadmapDetailResponse.json?.data?.nodes?.length).toBeGreaterThan(0)
})
