import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite 配置
// - build 产物输出到 backend/dist/，这样后端 Jetty 可以直接静态托管
// - 开发时通过 proxy 把 WebSocket 请求转给后端 8080
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../backend/dist',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
})
