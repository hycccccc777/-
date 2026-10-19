import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  // 后端代理地址只由 Vite 在开发服务器中读取，不放入客户端包。
  const env = loadEnv(mode, process.cwd(), '')
  const port = Number(env.DEV_PORT || 5174)
  const target = env.DEV_API_TARGET || 'http://127.0.0.1:8080'
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('DEV_PORT 必须是有效端口')
  if (!/^https?:\/\//.test(target)) throw new Error('DEV_API_TARGET 必须为 HTTP(S) 地址')
  return {
    plugins: [vue()],
    server: {
      host: '127.0.0.1',
      port,
      strictPort: true,
      proxy: { '/api': { target, changeOrigin: true }, '/profile': { target, changeOrigin: true } },
    },
    preview: { host: '127.0.0.1', port: 4174, strictPort: true },
  }
})