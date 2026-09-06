import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// backend(Java, /api/v1)와 notification-server(REST+WebSocket)를 각각 프록시한다 — admin-web(5173)·
// commander-tablet(5174)과 동시에 띄울 수 있도록 대원 앱은 5175를 쓴다.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    restoreMocks: true,
  },
  server: {
    port: 5175,
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
    },
  },
})
