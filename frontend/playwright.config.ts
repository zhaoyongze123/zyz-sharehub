import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:14173'

export default defineConfig({
  testDir: './tests/e2e',
  testIgnore: ['../.git-main-merge/**'],
  timeout: 30_000,
  workers: 1,
  expect: {
    timeout: 5_000
  },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: process.env.PLAYWRIGHT_HTML_REPORT || '../output/overnight/playwright-report' }]
  ],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    headless: true
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome']
      }
    }
  ]
})
