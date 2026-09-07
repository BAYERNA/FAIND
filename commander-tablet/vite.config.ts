import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// backend(Java, /api/v1)와 notification-server(REST+WebSocket)를 각각 프록시한다 — 실서비스에서는
// Spring Cloud Gateway가 맡을 라우팅을, 데모 단계에서는 dev server가 대신한다 (admin-web과 동일 패턴).
// admin-web이 5173을 쓰므로 지휘관 태블릿은 5174를 쓴다.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    restoreMocks: true,
    // e2e/*.spec.ts는 Playwright 전용(별도 playwright.config.ts) — vitest 기본 include 패턴이
    // *.spec.ts도 잡아서 여기 명시적으로 src의 *.test.ts(x)만 대상으로 좁힌다.
    include: ['src/**/*.test.{ts,tsx}'],
  },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
      '/notify': {
        target: process.env.VITE_NOTIFICATION_URL ?? 'http://localhost:3001',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/notify/, ''),
      },
      '/socket.io': {
        target: process.env.VITE_NOTIFICATION_URL ?? 'http://localhost:3001',
        changeOrigin: true,
        ws: true,
      },
      // CMD-002 라이브 카메라 뷰(FR-24/26) — ai-server의 MJPEG 중계 엔드포인트로 프록시한다.
      '/ai-stream': {
        target: process.env.VITE_AI_SERVER_URL ?? 'http://localhost:8001',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai-stream/, '/api/v1/streams'),
      },
    },
  },
})
