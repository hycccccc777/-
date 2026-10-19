/**
 * 独立 HTTP 层；不依赖页面或 localStorage，便于替换认证方案和测试。
 * success code 暂兼容文档的 200 与 100000，联调冻结后应统一。
 *
 * <p>P3 之后这里承担两件事：
 * 1) 每个请求带上 X-Trace-Id，后端沿用该值写入日志，前后端可对账同一次请求；
 * 2) access token 过期（401）时自动用 refreshToken 换新令牌并重放原请求，
 *    刷新失败才真正把用户踢回登录页。
 */

export class ApiError extends Error {
  constructor(message, { status = 0, code = null, traceId = null } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.traceId = traceId
  }
}

/** 生成 16 位十六进制 traceId，与后端 TraceContext.newId() 长度一致 */
function defaultTraceId() {
  const bytes = new Uint8Array(8)
  globalThis.crypto.getRandomValues(bytes)
  return Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('')
}

/** 判断是否值得尝试刷新：只有「已登录请求被 401」才刷新 */
function shouldAttemptRefresh(authenticated, error) {
  return Boolean(authenticated) && error instanceof ApiError && error.status === 401
}

export function createHttpClient({
  baseURL = '/api/v1',
  timeoutMs = 10000,
  getToken = () => null,
  getRefreshToken = () => null,
  onTokenRefreshed = () => {},
  onAuthExpired = () => {},
  fetchImpl = globalThis.fetch,
  traceIdFactory = defaultTraceId,
} = {}) {
  const timeout = Number(timeoutMs)
  if (!Number.isFinite(timeout) || timeout <= 0) throw new Error('请求超时必须大于 0')

  /**
   * 并发刷新串行化。
   * 页面同时发 5 个请求、令牌又刚好过期时，若各自去刷新，
   * 后端的一次性刷新机制会让后 4 个失败并把用户踢下线。
   * 这里用共享 Promise 保证只刷一次，其余请求等同一个结果。
   */
  let refreshing = null

  async function sendRaw(path, { method = 'GET', query, body }, token) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query || {})) {
      if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
    }
    const queryString = params.toString()
    const url = baseURL.replace(/\/$/, '') + path + (queryString ? '?' + queryString : '')

    const headers = {
      Accept: 'application/json',
      'X-Trace-Id': traceIdFactory(),
    }
    if (token) headers.Authorization = 'Bearer ' + token

    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
    if (body !== undefined && !isFormData) headers['Content-Type'] = 'application/json'

    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), timeout)
    try {
      const response = await fetchImpl(url, {
        method,
        headers,
        signal: controller.signal,
        body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
      })
      const traceId = response.headers.get('X-Trace-Id')
      if (response.status === 204) return null
      let result
      try {
        result = await response.json()
      } catch {
        if (controller.signal.aborted) throw new ApiError('请求超时，请稍后重试')
        throw new ApiError('服务返回了非 JSON 响应，请检查代理或后端配置', { status: response.status, traceId })
      }
      if (!response.ok || !result || ![200, 100000].includes(result.code)) {
        throw new ApiError(result?.message || '请求失败', { status: response.status, code: result?.code, traceId })
      }
      return result.data
    } catch (error) {
      if (error instanceof ApiError) throw error
      if (controller.signal.aborted) throw new ApiError('请求超时，请稍后重试')
      throw new ApiError('无法连接后端，请检查服务和代理配置')
    } finally {
      clearTimeout(timer)
    }
  }

  async function refreshAccessToken() {
    if (refreshing) return refreshing
    refreshing = (async () => {
      const refreshToken = getRefreshToken()
      if (!refreshToken) throw new ApiError('登录已过期，请重新登录', { status: 401, code: 110002 })
      // 刷新接口本身不能走 authenticated 分支，否则 401 会递归刷新
      const data = await sendRaw('/auth/refresh', { method: 'POST', body: { refreshToken } }, null)
      if (!data || !data.token) throw new ApiError('登录已过期，请重新登录', { status: 401, code: 110002 })
      onTokenRefreshed(data)
      return data.token
    })().finally(() => {
      refreshing = null
    })
    return refreshing
  }

  return async function request(path, { method = 'GET', query, body, authenticated = false } = {}) {
    if (!path.startsWith('/') || path.startsWith('//')) throw new Error('接口路径必须以单个 / 开头')

    const token = authenticated ? getToken() : null
    if (authenticated && !token) throw new ApiError('请先完成真实登录', { status: 401, code: 110001 })

    try {
      return await sendRaw(path, { method, query, body }, token)
    } catch (error) {
      if (!shouldAttemptRefresh(authenticated, error)) throw error
      // 只在第一次 401 时尝试刷新；刷新后重放仍失败则交给上层
      let newToken
      try {
        newToken = await refreshAccessToken()
      } catch (refreshError) {
        onAuthExpired()
        throw refreshError
      }
      return sendRaw(path, { method, query, body }, newToken)
    }
  }
}