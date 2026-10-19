/**
 * 独立 HTTP 层；不依赖页面或 localStorage，便于替换认证方案和测试。
 * success code 暂兼容文档的 200 与 100000，联调冻结后应统一。
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

export function createHttpClient({
  baseURL = '/api/v1',
  timeoutMs = 10000,
  getToken = () => null,
  fetchImpl = globalThis.fetch,
} = {}) {
  const timeout = Number(timeoutMs)
  if (!Number.isFinite(timeout) || timeout <= 0) throw new Error('请求超时必须大于 0')

  return async function request(path, { method = 'GET', query, body, authenticated = false } = {}) {
    if (!path.startsWith('/') || path.startsWith('//')) throw new Error('接口路径必须以单个 / 开头')
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query || {})) {
      if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
    }
    const queryString = params.toString()
    const url = baseURL.replace(/\/$/, '') + path + (queryString ? '?' + queryString : '')
    const headers = { Accept: 'application/json' }
    if (authenticated) {
      const token = getToken()
      if (!token) throw new ApiError('请先完成真实登录', { status: 401, code: 110001 })
      headers.Authorization = 'Bearer ' + token
    }
    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
    if (body !== undefined && !isFormData) headers['Content-Type'] = 'application/json'
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), timeout)
    try {
      const response = await fetchImpl(url, {
        method, headers, signal: controller.signal,
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
}