import { expect, test } from '@playwright/test'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
const targetTotalForPagination = 11

function waitNotesResponseWithQuery(page: Parameters<typeof test>[0]['page'], expected: Record<string, string>) {
  return page.waitForResponse((response) => {
    if (!response.url().includes('/api/notes') || response.request().method() !== 'GET') {
      return false
    }
    const url = new URL(response.url())
    return Object.entries(expected).every(([key, value]) => url.searchParams.get(key) === value)
  })
}

async function seedPublishedNotes(request: Parameters<typeof test>[0]['request'], count: number) {
  const batch = Date.now()
  for (let i = 0; i < count; i += 1) {
    const response = await request.post(`${apiBaseUrl}/api/notes`, {
      headers: {
        'X-User-Key': 'playwright-user',
        'Content-Type': 'application/json'
      },
      data: {
        title: `Playwright 社区分页联动 ${batch}-${i + 1}`,
        contentMd: `# LLM 分页回归 ${i + 1}\n\n这是用于分页联动回归的自动化种子数据。`,
        visibility: 'PUBLIC',
        status: 'PUBLISHED'
      }
    })
    expect(response.ok()).toBeTruthy()
  }
}

async function ensureEnoughPublicNotes(request: Parameters<typeof test>[0]['request']) {
  const listResponse = await request.get(`${apiBaseUrl}/api/notes?page=1&pageSize=1`, {
    headers: {
      'X-User-Key': 'playwright-user'
    }
  })
  expect(listResponse.ok()).toBeTruthy()
  const listBody = await listResponse.json()
  const total = Number(listBody?.data?.total ?? 0)
  const needed = Math.max(0, targetTotalForPagination - total)
  if (needed > 0) {
    await seedPublishedNotes(request, needed)
  }
}

test('community 列表分页可翻页并拉取第2页数据', async ({ page, request }) => {
  test.setTimeout(90_000)
  await ensureEnoughPublicNotes(request)
  await page.goto('/community')

  const pagination = page.locator('.pagination')
  await expect(pagination).toBeVisible()

  const pageLabel = pagination.locator('span')
  await expect(pageLabel).toContainText('第 1 /')

  const firstTitleOnPage1 = (await page.locator('.topic-row:not(.pinned) .topic-title').first().textContent())?.trim() || ''

  const page2ResponsePromise = page.waitForResponse((response) => {
    if (!response.url().includes('/api/notes')) {
      return false
    }
    const url = new URL(response.url())
    return url.searchParams.get('page') === '2' && response.request().method() === 'GET'
  })

  await pagination.getByRole('button', { name: '下一页' }).click()

  const page2Response = await page2ResponsePromise
  expect(page2Response.ok()).toBeTruthy()

  await expect(pageLabel).toContainText('第 2 /')

  const firstTitleOnPage2 = (await page.locator('.topic-row:not(.pinned) .topic-title').first().textContent())?.trim() || ''
  expect(firstTitleOnPage2.length).toBeGreaterThan(0)
  expect(firstTitleOnPage2).not.toBe(firstTitleOnPage1)

  await page.screenshot({ path: '../output/manual-note-smoke/community-page-2.png', fullPage: true })
})

test('community 分页与筛选联动：tab/category/nav 切换重置页码', async ({ page, request }) => {
  test.setTimeout(90_000)
  await ensureEnoughPublicNotes(request)
  await page.goto('/community')

  const pagination = page.locator('.pagination')
  const pageLabel = pagination.locator('span')
  await expect(pagination).toBeVisible()
  await expect(pageLabel).toContainText('第 1 /')

  const toPage2 = waitNotesResponseWithQuery(page, { page: '2' })
  await pagination.getByRole('button', { name: '下一页' }).click()
  expect((await toPage2).ok()).toBeTruthy()
  await expect(pageLabel).toContainText('第 2 /')

  const featuredBtn = page.getByRole('button', { name: '精选合集' })
  const featuredResp = waitNotesResponseWithQuery(page, { page: '1', status: 'PUBLISHED' })
  await featuredBtn.click()
  expect((await featuredResp).ok()).toBeTruthy()
  await expect(pageLabel).toContainText('第 1 /')

  const latestBtn = page.getByRole('button', { name: '最新 AI 分享' })
  const latestResp = waitNotesResponseWithQuery(page, { page: '1' })
  await latestBtn.click()
  expect((await latestResp).ok()).toBeTruthy()
  await expect(pageLabel).toContainText('第 1 /')

  const toPage2Again = waitNotesResponseWithQuery(page, { page: '2' })
  await pagination.getByRole('button', { name: '下一页' }).click()
  expect((await toPage2Again).ok()).toBeTruthy()
  await expect(pageLabel).toContainText('第 2 /')

  const categoryBtn = page.locator('.forum-sidebar .forum-nav-group').nth(1).locator('button.forum-nav-item').first()
  const categoryResetResp = waitNotesResponseWithQuery(page, { page: '1' })
  await categoryBtn.click()
  expect((await categoryResetResp).ok()).toBeTruthy()
  await expect(pagination).toBeHidden()

  await page.locator('.filter-dropdown').click()
  await page.getByRole('button', { name: '分享广场' }).click()
  await page.locator('.dropdown-item', { hasText: '全部领域' }).click()

  await expect(pagination).toBeVisible()
  await expect(pageLabel).toContainText('第 1 /')

  await page.screenshot({ path: '../output/manual-note-smoke/community-pagination-linked.png', fullPage: true })
})
