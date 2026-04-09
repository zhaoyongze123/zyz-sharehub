import { expect, test } from '@playwright/test'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const enabledModules = new Set(
  (process.env.PLAYWRIGHT_MODULES || 'all')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
)

function shouldRun(moduleName: string) {
  return enabledModules.has('all') || enabledModules.has(moduleName)
}

function userHeaders(userKey = 'playwright-user') {
  return {
    'X-User-Key': userKey,
    'Content-Type': 'application/json'
  }
}

function adminHeaders() {
  return {
    'X-Admin-Token': process.env.PLAYWRIGHT_ADMIN_TOKEN || 'dev-admin-token',
    'Content-Type': 'application/json'
  }
}

async function createPublishedResource(request: Parameters<typeof test>[0]['request'], title: string) {
  const createResponse = await request.post(`${apiBaseUrl}/api/resources`, {
    headers: userHeaders(),
    data: {
      title,
      type: 'PDF',
      category: 'PDF',
      summary: `${title} summary`,
      tags: ['agent', 'playwright'],
      visibility: 'PUBLIC',
      status: 'DRAFT'
    }
  })
  expect(createResponse.ok()).toBeTruthy()
  const createdBody = await createResponse.json()
  const id = createdBody.data.id as number

  const publishResponse = await request.post(`${apiBaseUrl}/api/resources/${id}/publish`, {
    headers: userHeaders()
  })
  expect(publishResponse.ok()).toBeTruthy()
  return id
}

async function createNote(request: Parameters<typeof test>[0]['request'], title: string) {
  const response = await request.post(`${apiBaseUrl}/api/notes`, {
    headers: userHeaders(),
    data: {
      title,
      contentMd: `# ${title}\n\nplaywright`,
      visibility: 'PUBLIC',
      status: 'PUBLISHED'
    }
  })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  return body.data.id as number
}

async function createResume(request: Parameters<typeof test>[0]['request'], templateKey = 'classic') {
  const response = await request.post(`${apiBaseUrl}/api/resumes/generate`, {
    headers: userHeaders(),
    data: { templateKey }
  })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  return body.data.id as number
}

async function createPublicRoadmap(
  request: Parameters<typeof test>[0]['request'],
  title: string,
  nodes: Array<{ title: string, resourceId?: number | null }> = []
) {
  const createResponse = await request.post(`${apiBaseUrl}/api/roadmaps`, {
    headers: userHeaders(),
    data: {
      title,
      description: `${title} description`,
      visibility: 'PUBLIC',
      status: 'PUBLISHED'
    }
  })
  expect(createResponse.ok()).toBeTruthy()
  const createBody = await createResponse.json()
  const id = createBody.data.id as number

  for (const [index, node] of nodes.entries()) {
    const nodeResponse = await request.post(`${apiBaseUrl}/api/roadmaps/${id}/nodes`, {
      headers: userHeaders(),
      data: {
        title: node.title,
        orderNo: index + 1,
        resourceId: node.resourceId ?? null
      }
    })
    expect(nodeResponse.ok()).toBeTruthy()
  }

  return id
}

async function loginAs(page: Parameters<typeof test>[0]['page'], role: 'user' | 'admin') {
  await page.addInitScript((selectedRole) => {
    window.localStorage.setItem('sharebase.role', selectedRole)
    window.localStorage.setItem('sharebase.nickname', selectedRole === 'admin' ? 'Playwright Admin' : 'Playwright User')
    window.localStorage.setItem('sharebase.headline', selectedRole === 'admin' ? 'E2E admin' : 'E2E user')
    window.localStorage.setItem('sharebase.userKey', 'playwright-user')
    if (selectedRole === 'admin') {
      window.localStorage.setItem('sharebase.adminToken', 'dev-admin-token')
    }
  }, role)
}

test.describe.configure({ mode: 'serial' })

test('backend health', async ({ request }) => {
  test.skip(!shouldRun('backend'))
  const response = await request.get(`${apiBaseUrl}/actuator/health`)
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  expect(body.status).toBe('UP')
})

test('resources 模块真接口联调', async ({ page, request }) => {
  test.skip(!shouldRun('resources'))
  const batchId = Date.now()
  const relatedResourceTitle = `Playwright Related Resource ${batchId}`
  const relatedResourceId = await createPublishedResource(request, relatedResourceTitle)
  const resourceTitle = `Playwright Resource ${batchId}`
  const resourceId = await createPublishedResource(request, resourceTitle)

  const listResponse = await request.get(`${apiBaseUrl}/api/resources?page=0&pageSize=12`, {
    headers: userHeaders()
  })
  expect(listResponse.ok()).toBeTruthy()
  const listBody = await listResponse.json()
  expect(Array.isArray(listBody.data.items)).toBeTruthy()
  expect(listBody.data.items.some((item: { id: number }) => item.id === resourceId)).toBeTruthy()

  const detailResponse = await request.get(`${apiBaseUrl}/api/resources/${resourceId}`)
  expect(detailResponse.ok()).toBeTruthy()
  const detailBody = await detailResponse.json()
  expect(detailBody.data.id).toBe(resourceId)

  const relatedResponse = await request.get(`${apiBaseUrl}/api/resources/${resourceId}/related`)
  expect(relatedResponse.ok()).toBeTruthy()
  const relatedBody = await relatedResponse.json()
  expect(Array.isArray(relatedBody.data)).toBeTruthy()
  expect(relatedBody.data.some((item: { id: number }) => item.id === relatedResourceId)).toBeTruthy()

  await page.goto('/resources')
  await expect(page.locator('main').getByRole('heading', { name: '资料广场' })).toBeVisible()
  const resourceCard = page.locator('article', { hasText: resourceTitle }).first()
  await expect(resourceCard).toBeVisible()
  const resourceLink = resourceCard.getByRole('link', { name: '查看详情' })
  await resourceLink.click()
  await expect(page.getByRole('heading', { name: resourceTitle })).toBeVisible()
  await expect(page.getByText(`下载 ${detailBody.data.downloadCount}`)).toBeVisible()
  const relatedCard = page.locator('.related-grid article', { hasText: relatedResourceTitle }).first()
  await expect(relatedCard).toBeVisible()
  await relatedCard.getByRole('link', { name: '查看详情' }).click()
  await expect(page).toHaveURL(new RegExp(`/resources/${relatedResourceId}$`))
  await expect(page.getByRole('heading', { name: relatedResourceTitle })).toBeVisible()
})

test('roadmaps 模块真接口联调', async ({ page, request }) => {
  test.skip(!shouldRun('roadmaps'))
  const linkedResourceTitle = `Roadmap Resource ${Date.now()}`
  const linkedResourceId = await createPublishedResource(request, linkedResourceTitle)
  const roadmapTitle = `Playwright Roadmap ${Date.now()}`
  const roadmapId = await createPublicRoadmap(request, roadmapTitle, [
    { title: '阶段 1：接线', resourceId: linkedResourceId },
    { title: '阶段 2：验证' }
  ])

  const listResponse = await request.get(`${apiBaseUrl}/api/roadmaps?page=1&pageSize=20`)
  expect(listResponse.ok()).toBeTruthy()
  const listBody = await listResponse.json()
  expect(Array.isArray(listBody.data.items)).toBeTruthy()
  expect(listBody.data.items.some((item: { id: number }) => item.id === roadmapId)).toBeTruthy()

  const detailResponse = await request.get(`${apiBaseUrl}/api/roadmaps/${roadmapId}`, {
    headers: userHeaders()
  })
  expect(detailResponse.ok()).toBeTruthy()
  const detailBody = await detailResponse.json()
  expect(detailBody.data.roadmap.id).toBe(roadmapId)
  expect(detailBody.data.nodes.length).toBeGreaterThan(0)

  await page.goto('/roadmaps')
  await expect(page.getByText('学习路线图')).toBeVisible()
  const roadmapCard = page.locator('article', { hasText: roadmapTitle }).first()
  await expect(roadmapCard).toBeVisible()
  await expect(roadmapCard.getByText(`${roadmapTitle} description`)).toBeVisible()
  await roadmapCard.getByRole('link', { name: '进入路线' }).click()
  await expect(page.getByRole('heading', { name: roadmapTitle })).toBeVisible()
  await expect(page.getByText('节点进度结构')).toBeVisible()
  await expect(page.getByText('阶段 1：接线')).toBeVisible()
  await page.locator('.timeline__item', { hasText: '阶段 1：接线' }).click()
  await expect(page.getByRole('heading', { name: '阶段 1：接线 详情' })).toBeVisible()
  const resourcePreview = page.locator('.resource-preview')
  await expect(resourcePreview.getByText(linkedResourceTitle)).toBeVisible()
  await resourcePreview.getByRole('button', { name: '查看' }).click()
  await expect(page).toHaveURL(new RegExp(`/resources/${linkedResourceId}$`))
  await expect(page.getByRole('heading', { name: linkedResourceTitle })).toBeVisible()
})

test('notes 模块真接口联调', async ({ page, request }) => {
  test.skip(!shouldRun('notes'))
  const noteId = await createNote(request, `Playwright Note ${Date.now()}`)

  const listResponse = await request.get(`${apiBaseUrl}/api/notes?page=1&pageSize=10`, {
    headers: userHeaders()
  })
  expect(listResponse.ok()).toBeTruthy()
  const listBody = await listResponse.json()
  expect(Array.isArray(listBody.data.items)).toBeTruthy()
  expect(listBody.data.items.some((item: { id: number }) => item.id === noteId)).toBeTruthy()

  const detailResponse = await request.get(`${apiBaseUrl}/api/notes/${noteId}`, {
    headers: userHeaders()
  })
  expect(detailResponse.ok()).toBeTruthy()

  await page.goto('/community')
  await expect(page.getByText('AI 资源类别')).toBeVisible()
})

test('resumes 模块真接口联调', async ({ page, request }) => {
  test.skip(!shouldRun('resumes'))
  const batchId = Date.now()
  const seedResumeId = await createResume(request, 'classic')

  const workbenchResponse = await request.get(`${apiBaseUrl}/api/resumes/workbench`, {
    headers: userHeaders()
  })
  expect(workbenchResponse.ok()).toBeTruthy()
  const workbenchBody = await workbenchResponse.json()
  expect(workbenchBody.data.total).toBeGreaterThan(0)
  expect(Array.isArray(workbenchBody.data.recentItems)).toBeTruthy()

  const listResponse = await request.get(`${apiBaseUrl}/api/resumes?page=1&pageSize=10`, {
    headers: userHeaders()
  })
  expect(listResponse.ok()).toBeTruthy()
  const listBody = await listResponse.json()
  expect(listBody.data.items.some((item: { id: number }) => item.id === seedResumeId)).toBeTruthy()

  const downloadResponse = await request.get(`${apiBaseUrl}/api/resumes/${seedResumeId}/download`, {
    headers: {
      'X-User-Key': 'playwright-user'
    }
  })
  expect(downloadResponse.ok()).toBeTruthy()

  await loginAs(page, 'user')
  await page.goto('/resume')
  await expect(page.getByText('导出 PDF')).toBeVisible()
  await expect(page.getByTestId('resume-workbench-summary')).toContainText('累计')
  await expect(page.getByTestId(`resume-server-item-${seedResumeId}`)).toBeVisible()
  await expect(page.getByTestId('resume-workbench-summary')).toContainText(`累计 ${workbenchBody.data.total}`)

  const recentWorkbenchItem = workbenchBody.data.recentItems.find((item: { id: number }) => item.id === seedResumeId)
  expect(recentWorkbenchItem).toBeTruthy()
  await expect(page.getByTestId(`resume-server-item-${recentWorkbenchItem.id}`)).toContainText(
    recentWorkbenchItem.fileName || `resume-${recentWorkbenchItem.id}.pdf`
  )

  const resumeRows = page.getByTestId('resume-server-list').locator('[data-testid^="resume-server-item-"]')
  const summary = page.getByTestId('resume-workbench-summary')
  const summaryBefore = await summary.textContent()
  const totalBefore = Number(summaryBefore?.match(/累计\s+(\d+)/)?.[1] ?? '0')
  const firstItemBefore = await resumeRows.first().getAttribute('data-testid')

  await page.getByRole('combobox').nth(1).selectOption('modern')
  await page.getByTestId('resume-generate-button').click()

  await expect(summary).toContainText(`累计 ${totalBefore + 1}`)
  const generatedItem = resumeRows.first()
  const generatedTestId = await generatedItem.getAttribute('data-testid')
  expect(generatedTestId).toBeTruthy()
  expect(generatedTestId).not.toBe(firstItemBefore)
  const generatedId = Number(generatedTestId?.replace('resume-server-item-', ''))
  expect(Number.isFinite(generatedId)).toBeTruthy()
  await expect(generatedItem).toContainText('resume-modern.pdf')

  const downloadPromise = page.waitForEvent('download')
  await page.getByTestId(`resume-download-${generatedId}`).click()
  const browserDownload = await downloadPromise
  expect(browserDownload.suggestedFilename()).toContain('resume-modern')

  const deleteResponsePromise = page.waitForResponse((response) =>
    response.url().includes(`/api/resumes/${generatedId}`) && response.request().method() === 'DELETE'
  )
  const reloadAfterDeletePromise = page.waitForResponse((response) =>
    response.url().includes('/api/resumes?page=1&pageSize=10') && response.request().method() === 'GET'
  )
  await page.getByTestId(`resume-delete-${generatedId}`).click()
  const deleteResponse = await deleteResponsePromise
  expect(deleteResponse.ok()).toBeTruthy()
  await reloadAfterDeletePromise
  await expect(page.getByTestId(`resume-server-item-${generatedId}`)).toHaveCount(0)

  const listAfterDeleteResponse = await request.get(`${apiBaseUrl}/api/resumes?page=1&pageSize=10`, {
    headers: userHeaders()
  })
  expect(listAfterDeleteResponse.ok()).toBeTruthy()
  const listAfterDeleteBody = await listAfterDeleteResponse.json()
  expect(listAfterDeleteBody.data.items.some((item: { id: number }) => item.id === generatedId)).toBeFalsy()
})

test('me 模块真接口联调', async ({ page, request }) => {
  test.skip(!shouldRun('profile'))
  await createPublishedResource(request, `Profile Resource ${Date.now()}`)
  await createNote(request, `Profile Note ${Date.now()}`)
  await createResume(request, 'modern')

  const meResponse = await request.get(`${apiBaseUrl}/api/me`, {
    headers: userHeaders()
  })
  expect(meResponse.ok()).toBeTruthy()
  const meBody = await meResponse.json()

  const resourcesResponse = await request.get(`${apiBaseUrl}/api/me/resources?page=1&pageSize=10`, {
    headers: userHeaders()
  })
  expect(resourcesResponse.ok()).toBeTruthy()
  const resourcesBody = await resourcesResponse.json()

  const notesResponse = await request.get(`${apiBaseUrl}/api/me/notes?page=1&pageSize=10`, {
    headers: userHeaders()
  })
  expect(notesResponse.ok()).toBeTruthy()
  const notesBody = await notesResponse.json()

  const resumesResponse = await request.get(`${apiBaseUrl}/api/me/resumes?page=1&pageSize=10`, {
    headers: userHeaders()
  })
  expect(resumesResponse.ok()).toBeTruthy()
  const resumesBody = await resumesResponse.json()

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
  await expect(page.getByText(`@${meBody.data.profile.login}`)).toBeVisible()
  await expect(page.getByText(String(meBody.data.myResourceCount)).first()).toBeVisible()
  await expect(page.getByRole('button', { name: '资料编辑待接写接口' })).toBeDisabled()
  await expect(page.getByText('已移除页面内个人资料模拟保存动作，改为只读展示')).toBeVisible()

  const firstResource = resourcesBody.data.items[0]
  if (firstResource) {
    await expect(page.getByRole('link', { name: firstResource.title }).first()).toBeVisible()
  }

  const firstNote = notesBody.data.items[0]
  if (firstNote) {
    await expect(page.getByRole('link', { name: firstNote.title }).first()).toBeVisible()
  }

  const firstResume = resumesBody.data.items[0]
  if (firstResume) {
    await expect(page.getByText(firstResume.fileName || `resume-${firstResume.id}.pdf`).first()).toBeVisible()
  }

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
  expect(avatarBody.data.downloadUrl).toContain('/api/files/')
  await expect(page.getByTestId('profile-avatar').locator('img')).toHaveAttribute('src', /\/api\/files\//)
  await expect(page.getByTestId('console-sidebar-avatar')).toHaveAttribute('src', /\/api\/files\//)

  const reloadAuthMeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/me') && response.request().method() === 'GET'
  )
  await page.reload()
  const reloadAuthMeResponse = await reloadAuthMeResponsePromise
  expect(reloadAuthMeResponse.ok()).toBeTruthy()
  await expect(page.getByText(`@${authMeBody.data.login}`)).toBeVisible()
  await expect(page.getByTestId('profile-avatar').locator('img')).toHaveAttribute('src', /\/api\/files\//)
  await expect(page.getByTestId('console-sidebar-avatar')).toHaveAttribute('src', /\/api\/files\//)
})

test('admin 模块真接口联调', async ({ page, request }) => {
  test.skip(!shouldRun('admin'))
  await createPublishedResource(request, `Admin Resource ${Date.now()}`)

  const reportsResponse = await request.get(`${apiBaseUrl}/api/admin/reports?page=1&pageSize=20`, {
    headers: adminHeaders()
  })
  expect(reportsResponse.ok()).toBeTruthy()

  const auditLogsResponse = await request.get(`${apiBaseUrl}/api/admin/audit-logs?page=1&pageSize=20`, {
    headers: adminHeaders()
  })
  expect(auditLogsResponse.ok()).toBeTruthy()

  await loginAs(page, 'admin')
  await page.goto('/admin')
  await expect(page.getByText('管理中心仪表盘')).toBeVisible()
})
