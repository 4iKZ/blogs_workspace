import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
// 静态资源 CDN：生产环境用 .env.production 中的 VITE_CDN_BASE，未设置则用相对路径
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const cdnBase = env.VITE_CDN_BASE || '/'
  return {
  base: cdnBase.endsWith('/') ? cdnBase : cdnBase + '/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
})