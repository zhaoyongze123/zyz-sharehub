import { expect, test, type Page } from '@playwright/test'

const enabledModules = new Set(
  (process.env.PLAYWRIGHT_MODULES || 'public').split(',').map((item) => item.trim()).filter(Boolean)
)

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const userKey = process.env.PLAYWRIGHT_USER_KEY || 'playwright-user'
const adminToken = process.env.PLAYWRIGHT_ADMIN_TOKEN || 'dev-admin-token'

async function loginAs(page: Page, role: 'user' | 'admin') {
  await page.addInitScript(({ selectedRole, adminTokenValue }) => {
    window.localStorage.setItem('sharebase.role', selectedRole)
    window.localStorage.setItem('sharebase.nickname', selectedRole === 'admin' ? 'Admin Zoe' : 'Alex Chen')
    window.localStorage.setItem('sharebase.headline', selectedRole === 'admin' ? '治理中台负责人' : 'Agent / RAG 工程实践者')
    window.localStorage.setItem('sharebase.userKey', 'playwright-user')
    if (selectedRole === 'admin') {
      window.localStorage.setItem('sharebase.adminToken', adminTokenValue)
    }
  }, { selectedRole: role, adminTokenValue: adminToken })
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

test('登录回调页通过 auth/me 恢复真实身份', async ({ page }) => {
  test.skip(!shouldRun('profile'))
  await loginAs(page, 'user')
  const authMeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/me') && response.request().method() === 'GET'
  )
  await page.goto('/auth/callback?redirect=/me')
  const authMeResponse = await authMeResponsePromise
  expect(authMeResponse.ok()).toBeTruthy()
  const authMeBody = await authMeResponse.json()
  expect(authMeBody.success).toBeTruthy()
  expect(authMeBody.data?.login).toBeTruthy()
  await expect(page).toHaveURL(/\/me$/)
  await expect(page.getByText(`@${authMeBody.data.login}`)).toBeVisible()
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

test('笔记详情真实读取 smoke', async ({ page, request }) => {
  test.skip(!shouldRun('notes'))
  const noteTitle = `Smoke Note ${Date.now()}`
  const createResponse = await request.post(`${apiBaseUrl}/api/notes`, {
    headers: {
      'X-User-Key': userKey,
      'Content-Type': 'application/json'
    },
    data: {
      title: noteTitle,
      contentMd: `# ${noteTitle}\n\n这是 smoke 用例写入的真实正文。\n\n## 小节\nSmoke detail paragraph`,
      visibility: 'PUBLIC',
      status: 'PUBLISHED'
    }
  })
  expect(createResponse.ok()).toBeTruthy()
  const createBody = await createResponse.json()
  const noteId = createBody.data.id as number

  await loginAs(page, 'user')
  await page.goto(`/notes/${noteId}`)
  await expect(page.getByTestId('note-detail-page')).toBeVisible()
  await expect(page.getByTestId('note-detail-main').getByRole('heading', { name: noteTitle }).first()).toBeVisible()
  await expect(page.getByText('这是 smoke 用例写入的真实正文。').first()).toBeVisible()
  await expect(page.getByText('Smoke detail paragraph').first()).toBeVisible()
  await expect(page.getByTestId('note-detail-content')).toContainText('这是 smoke 用例写入的真实正文。')
  await expect(page.getByTestId('note-detail-content')).toContainText('Smoke detail paragraph')
  await expect(page.getByTestId('note-outline')).toContainText('小节')
  await expect(page.getByTestId('note-detail-side')).toContainText('当前状态 PUBLISHED，可见性 PUBLIC')

  const reportResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/reports') && response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '举报' }).first().click()
  await page.getByLabel('举报原因').fill('Smoke note detail report')
  await page.getByRole('button', { name: '提交举报' }).click()
  const reportResponse = await reportResponsePromise
  expect(reportResponse.ok()).toBeTruthy()
  const reportBody = await reportResponse.json()
  expect(reportBody.success).toBeTruthy()
  expect(reportBody.data.targetType).toBe('NOTE')
  expect(reportBody.data.targetId).toBe(noteId)
  expect(reportBody.data.reason).toBe('Smoke note detail report')

  const adminReportsResponse = await request.get(`${apiBaseUrl}/api/admin/reports?page=1&pageSize=20`, {
    headers: {
      'X-Admin-Token': adminToken
    }
  })
  expect(adminReportsResponse.ok()).toBeTruthy()
  const adminReportsBody = await adminReportsResponse.json()
  const createdReport = (adminReportsBody.data.items as Array<{ targetType: string, targetId: number, reason: string }>).find((item) =>
    item.targetType === 'NOTE' && item.targetId === noteId && item.reason === 'Smoke note detail report'
  )
  expect(createdReport).toBeTruthy()
})

test('简历模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('resumes'))
  await loginAs(page, 'user')
  const authMeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/me') && response.request().method() === 'GET'
  )
  await page.goto('/resume')
  const authMeResponse = await authMeResponsePromise
  expect(authMeResponse.ok()).toBeTruthy()
  const authMeBody = await authMeResponse.json()
  expect(authMeBody.success).toBeTruthy()
  expect(authMeBody.data?.login).toBeTruthy()
  await expect(page.getByRole('button', { name: '导出 PDF' })).toBeVisible()
  const workbenchResponse = await browserFetch(page, '/api/resumes/workbench', {
    'X-User-Key': userKey
  })
  expect(workbenchResponse.ok).toBeTruthy()
  expect(workbenchResponse.status).toBe(200)
  expect(workbenchResponse.json?.success).toBeTruthy()
  expect(Array.isArray(workbenchResponse.json?.data?.recentItems)).toBeTruthy()
  await expect(page.getByTestId('resume-workbench-summary')).toContainText(`累计 ${workbenchResponse.json?.data?.total ?? 0}`)

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

  const firstWorkbenchResume = workbenchResponse.json?.data?.recentItems?.[0]
  if (firstWorkbenchResume) {
    await expect(page.getByTestId(`resume-server-item-${firstWorkbenchResume.id}`)).toContainText(
      firstWorkbenchResume.fileName || `resume-${firstWorkbenchResume.id}.pdf`
    )
  }

  const summary = page.getByTestId('resume-workbench-summary')
  const summaryBefore = await summary.textContent()
  const totalBefore = Number(summaryBefore?.match(/累计\s+(\d+)/)?.[1] ?? '0')

  await page.getByRole('combobox').nth(1).selectOption('modern')

  const generateResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/resumes/generate') && response.request().method() === 'POST'
  )
  const reloadWorkbenchResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/resumes/workbench') && response.request().method() === 'GET'
  )
  await page.getByTestId('resume-generate-button').click()

  const generateResponse = await generateResponsePromise
  expect(generateResponse.ok()).toBeTruthy()
  const generateBody = await generateResponse.json()
  expect(generateBody.success).toBeTruthy()
  const generatedId = generateBody.data?.id as number
  expect(Number.isFinite(generatedId)).toBeTruthy()

  const reloadWorkbenchResponse = await reloadWorkbenchResponsePromise
  expect(reloadWorkbenchResponse.ok()).toBeTruthy()
  await expect(summary).toContainText(`累计 ${totalBefore + 1}`)
  await expect(page.getByTestId(`resume-server-item-${generatedId}`)).toContainText('resume-modern.pdf')

  const downloadPromise = page.waitForEvent('download')
  await page.getByTestId(`resume-download-${generatedId}`).click()
  const browserDownload = await downloadPromise
  expect(browserDownload.suggestedFilename()).toContain('resume-modern')

  const deleteResponsePromise = page.waitForResponse((response) =>
    response.url().includes(`/api/resumes/${generatedId}`) && response.request().method() === 'DELETE'
  )
  const reloadListAfterDeletePromise = page.waitForResponse((response) =>
    response.url().includes('/api/resumes?page=1&pageSize=10') && response.request().method() === 'GET'
  )
  await page.getByTestId(`resume-delete-${generatedId}`).click()
  const deleteResponse = await deleteResponsePromise
  expect(deleteResponse.ok()).toBeTruthy()
  await reloadListAfterDeletePromise
  await expect(page.getByTestId(`resume-server-item-${generatedId}`)).toHaveCount(0)

  const reloadAuthMeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/me') && response.request().method() === 'GET'
  )
  await page.reload()
  const reloadAuthMeResponse = await reloadAuthMeResponsePromise
  expect(reloadAuthMeResponse.ok()).toBeTruthy()
  const reloadAuthMeBody = await reloadAuthMeResponse.json()
  expect(reloadAuthMeBody.success).toBeTruthy()
  expect(reloadAuthMeBody.data?.login).toBe(authMeBody.data.login)
  await expect(page.getByTestId('resume-workbench-summary')).toContainText('累计')
  if (firstResume) {
    await expect(page.getByTestId(`resume-server-item-${firstResume.id}`)).toBeVisible()
  }
})

test('个人中心模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('profile'))
  await loginAs(page, 'user')
  const authMeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/me') && response.request().method() === 'GET'
  )
  await page.goto('/me')
  const authMeResponse = await authMeResponsePromise
  expect(authMeResponse.ok()).toBeTruthy()
  const authMeBody = await authMeResponse.json()
  expect(authMeBody.success).toBeTruthy()
  expect(authMeBody.data?.login).toBeTruthy()
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
  expect(meResponse.json?.data?.profile?.login).toBe(authMeBody.data.login)
  const expectedDisplayName = meResponse.json?.data?.profile?.name?.trim() || meResponse.json?.data?.profile?.login
  await expect(page.getByText(`@${meResponse.json.data.profile.login}`)).toBeVisible()
  await expect(page.getByTestId('console-sidebar-name')).toHaveText(expectedDisplayName)
  await expect(page.getByTestId('profile-avatar-upload')).toBeEnabled()
  await expect(page.getByTestId('profile-stat-value-resources')).toHaveText(String(meResponse.json?.data?.myResourceCount ?? 0))
  await expect(page.getByTestId('profile-stat-desc-resources')).toHaveText(
    `近 7 天新增 ${meResponse.json?.data?.recentResourceCount ?? 0} 条，已发布 ${meResponse.json?.data?.publishedResourceCount ?? 0} 条`
  )
  await expect(page.getByTestId('profile-stat-value-notes')).toHaveText(String(meResponse.json?.data?.myNoteCount ?? 0))
  await expect(page.getByTestId('profile-stat-desc-notes')).toHaveText(
    `当前草稿 ${meResponse.json?.data?.draftNoteCount ?? 0} 条`
  )
  await expect(page.getByTestId('profile-stat-value-resumes')).toHaveText(String(meResponse.json?.data?.myResumeCount ?? 0))
  await expect(page.getByTestId('profile-stat-desc-resumes')).toHaveText(
    `已生成 ${meResponse.json?.data?.generatedResumeCount ?? 0} 份`
  )
  await expect(page.getByTestId('profile-stat-value-favorites-roadmaps')).toHaveText(
    `${meResponse.json?.data?.myFavoriteCount ?? 0} / ${meResponse.json?.data?.myRoadmapCount ?? 0}`
  )
  await expect(page.getByRole('button', { name: '资料编辑待接写接口' })).toBeDisabled()

  const avatarResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/avatar') && response.request().method() === 'POST'
  )
  await page.getByTestId('profile-avatar-input').setInputFiles({
    name: 'avatar.png',
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7Z0ZkAAAAASUVORK5CYII=',
      'base64'
    )
  })
  const avatarResponse = await avatarResponsePromise
  expect(avatarResponse.ok()).toBeTruthy()
  const avatarBody = await avatarResponse.json()
  expect(avatarBody.success).toBeTruthy()
  expect(avatarBody.data?.downloadUrl).toContain('/api/files/')
  await expect(page.getByTestId('profile-avatar').locator('img')).toHaveAttribute('src', /\/api\/files\//)
  await expect(page.getByTestId('console-sidebar-avatar')).toHaveAttribute('src', /\/api\/files\//)

  const resourcesResponse = await browserFetch(page, '/api/me/resources?page=1&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(resourcesResponse.ok).toBeTruthy()
  expect(Array.isArray(resourcesResponse.json?.data?.items)).toBeTruthy()
  if (resourcesResponse.json?.data?.items?.length) {
    await expect(page.getByRole('link', { name: resourcesResponse.json.data.items[0].title }).first()).toBeVisible()
  }

  const notesResponse = await browserFetch(page, '/api/me/notes?page=1&pageSize=5', {
    'X-User-Key': userKey
  })
  expect(notesResponse.ok).toBeTruthy()
  expect(Array.isArray(notesResponse.json?.data?.items)).toBeTruthy()
  if (notesResponse.json?.data?.items?.length) {
    await expect(page.getByRole('link', { name: notesResponse.json.data.items[0].title }).first()).toBeVisible()
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

  const reloadAuthMeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/me') && response.request().method() === 'GET'
  )
  await page.reload()
  const reloadAuthMeResponse = await reloadAuthMeResponsePromise
  expect(reloadAuthMeResponse.ok()).toBeTruthy()
  const reloadAuthMeBody = await reloadAuthMeResponse.json()
  expect(reloadAuthMeBody.success).toBeTruthy()
  expect(reloadAuthMeBody.data?.login).toBe(authMeBody.data.login)
  await expect(page.getByText(`@${meResponse.json.data.profile.login}`)).toBeVisible()
  await expect(page.getByTestId('console-sidebar-name')).toHaveText(expectedDisplayName)
  await expect(page.getByTestId('profile-avatar').locator('img')).toHaveAttribute('src', /\/api\/files\//)
  await expect(page.getByTestId('console-sidebar-avatar')).toHaveAttribute('src', /\/api\/files\//)
})

test('个人中心在 auth/me 失效时回跳登录页', async ({ page }) => {
  test.skip(!shouldRun('profile'))
  await loginAs(page, 'user')
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({
        success: false,
        code: 'UNAUTHORIZED',
        msg: 'unauthorized'
      })
    })
  })

  await page.goto('/me')
  await expect(page).toHaveURL(/\/login\?redirect=\/me$/)
  await expect(page.getByRole('heading', { name: '用内容构建你的学习资产' })).toBeVisible()
})

test('简历工作台在 auth/me 失效时回跳登录页', async ({ page }) => {
  test.skip(!shouldRun('resumes'))
  await loginAs(page, 'user')
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({
        success: false,
        code: 'UNAUTHORIZED',
        msg: 'unauthorized'
      })
    })
  })

  await page.goto('/resume')
  await expect(page).toHaveURL(/\/login\?redirect=\/resume$/)
  await expect(page.getByRole('heading', { name: '用内容构建你的学习资产' })).toBeVisible()
})

test('后台模块 smoke', async ({ page }) => {
  test.skip(!shouldRun('admin'))
  await loginAs(page, 'admin')
  await page.goto('/admin')
  await expect(page.getByText('管理中心仪表盘')).toBeVisible()

  const apiResponse = await browserFetch(page, '/api/admin/reports?page=1&pageSize=20', {
    'X-Admin-Token': adminToken
  })
  expect(apiResponse.ok).toBeTruthy()
  expect(apiResponse.status).toBe(200)
  expect(apiResponse.json?.success).toBeTruthy()
  expect(Array.isArray(apiResponse.json?.data?.items)).toBeTruthy()
  const reportItems = apiResponse.json?.data?.items ?? []
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
    'X-Admin-Token': adminToken
  })
  expect(auditResponse.ok).toBeTruthy()
  expect(auditResponse.status).toBe(200)
  expect(auditResponse.json?.success).toBeTruthy()
  expect(Array.isArray(auditResponse.json?.data?.items)).toBeTruthy()

  const usersResponse = await browserFetch(page, '/api/admin/users?page=1&pageSize=20', {
    'X-Admin-Token': adminToken
  })
  expect(usersResponse.ok).toBeTruthy()
  expect(usersResponse.status).toBe(200)
  expect(usersResponse.json?.success).toBeTruthy()
  expect(Array.isArray(usersResponse.json?.data?.items)).toBeTruthy()
  const managedUsers = usersResponse.json?.data?.items ?? []
  const bannedUsers = managedUsers.filter((item: { status?: string }) =>
    item.status === 'BANNED' || item.status === '已封禁'
  )
  const userStatCard = page.getByTestId('admin-dashboard-stat-users')
  await expect(userStatCard).toContainText('后台可管用户')
  await expect(userStatCard).toContainText(`已封禁 ${bannedUsers.length}`)
  await expect(userStatCard.locator('.stat-card__value')).toContainText(/^\d+$/)

  const resourceResponse = await browserFetch(page, '/api/resources?page=0&pageSize=100', {
    'X-User-Key': userKey
  })
  expect(resourceResponse.ok).toBeTruthy()
  expect(resourceResponse.json?.success).toBeTruthy()
  const firstResource = resourceResponse.json?.data?.items?.[0]
  expect(firstResource).toBeTruthy()

  await page.goto('/admin/taxonomy')
  await expect(page.getByRole('heading', { name: '标签分类管理' })).toBeVisible()
  await expect(page.getByTestId('admin-taxonomy-summary')).toContainText(
    `已读取 ${resourceResponse.json?.data?.items?.length ?? 0} / ${resourceResponse.json?.data?.total ?? 0} 条真实资料`
  )
  await expect(page.getByRole('cell', { name: firstResource.category || firstResource.type || '未分类' }).first()).toBeVisible()
  await expect(page.getByTestId('admin-taxonomy-readonly').first()).toBeVisible()

  await page.goto('/admin/audit-logs')
  await expect(page.getByRole('heading', { name: '审计日志' })).toBeVisible()
  const firstAuditLog = auditResponse.json?.data?.items?.[0]
  if (firstAuditLog) {
    await expect(page.getByTestId(`admin-audit-log-row-${firstAuditLog.id}`)).toContainText(
      `${firstAuditLog.targetType} #${firstAuditLog.targetId}`
    )
  } else {
    await expect(page.getByText('暂无审计日志')).toBeVisible()
  }

  await page.goto('/admin/users')
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
