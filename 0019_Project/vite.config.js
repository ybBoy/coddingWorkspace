import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 构建配置：React + TypeScript，代理 WebSocket 到后端 8080 端口
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
});
