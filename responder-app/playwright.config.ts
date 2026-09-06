import { defineConfig, devices } from '@playwright/test'
import { existsSync } from 'node:fs'

// Phase 10: 지금까지 매 Phase마다 수동으로 해온 브라우저 검증(로그인, 보호된 화면 접근)을
// 코드화한다. backend(8080)·notification-server(3001)가 이미 떠 있다는 전제 — 이 앱의
// dev server만 이 config가 직접 띄운다.

// 이 레포를 만드는 데 쓴 샌드박스는 Chromium을 이 경로에 미리 깔아두는데, @playwright/test가
// 기본으로 요구하는 리비전과 달라 그대로 두면 재다운로드를 시도하다 막힌다. 이 경로가 없는
// 환경(GitHub Actions 등)에서는 undefined로 둬 `npx playwright install`로 받은 표준 브라우저를
// 그대로 쓰게 한다.
const LOCAL_SANDBOX_CHROMIUM = '/opt/pw-browsers/chromium'
const executablePath = existsSync(LOCAL_SANDBOX_CHROMIUM) ? LOCAL_SANDBOX_CHROMIUM : undefined

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5175',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'], launchOptions: { executablePath } } },
  ],
  webServer: {
    command: 'npm run dev -- --port 5175',
    url: 'http://localhost:5175',
    reuseExistingServer: true,
    timeout: 30_000,
  },
})
