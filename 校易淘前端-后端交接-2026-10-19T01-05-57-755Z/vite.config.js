import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  // 后端代理地址只由 Vite 在开发服务器中读取，不放入客户端包。
  const env = loadEnv(mode, process.cwd(), '')
  const port = Number(env.DEV_PORT || 5174)
  const target = env.DEV_API_TARGET || 'http://127.0.0.1:8080'
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('DEV_PORT 必须是有效端口')
  if (!/^https?:\/\//.test(target)) throw new Error('DEV_API_TARGET 必须为 HTTP(S) 地址')
  // 允许访问的 Host：Vite 会校验 Host 头，拒绝不在白名单里的域名（防 DNS 重绑定）。
  // 用内网穿透或域名演示时，把域名加进来，否则会直接被 403。
  const allowedHosts = (env.DEV_ALLOWED_HOSTS || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return {
    plugins: [vue()],
    server: {
      // 默认只监听本机；需要让同网段/公网访问时用 DEV_HOST 覆盖
      host: env.DEV_HOST || '127.0.0.1',
      port,
      strictPort: true,
      allowedHosts: allowedHosts.length ? allowedHosts : undefined,
      proxy: { '/api': { target, changeOrigin: true }, '/profile': { target, changeOrigin: true } },
    },
    preview: { host: env.DEV_HOST || '127.0.0.1', port: 4174, strictPort: true },
  }
})