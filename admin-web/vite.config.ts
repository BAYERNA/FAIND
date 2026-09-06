import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// 백엔드가 /api/v1로 노출하는 API를 프록시한다 — 실서비스에서는 Spring Cloud Gateway가
// 맡을 라우팅을, 데모 단계에서는 dev server가 대신한다 (기술스택 §4 "API 진입점" 참조).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
