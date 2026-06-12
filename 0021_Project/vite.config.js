import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 配置文件
// 配置 React 插件、开发服务器端口、以及 WebSocket 代理
export default defineConfig({
  plugins: [react()],
  root: '.',
  server: {
    port: 3000,
    open: true,
    proxy: {
      // WebSocket 代理：前端 /ws 路径转发到后端 Java 服务器
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
