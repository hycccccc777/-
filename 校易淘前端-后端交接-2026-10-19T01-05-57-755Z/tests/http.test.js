import test from 'node:test'
import assert from 'node:assert/strict'
import { ApiError, createHttpClient } from '../src/api/http.js'

const json = (value, status = 200, headers = {}) => new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json', ...headers } })

test('编码查询参数，保留 0 和 false，去除空值并解包响应', async () => {
  let sent
  const request = createHttpClient({ fetchImpl: async (url, options) => { sent = { url, options }; return json({ code: 200, data: { total: 0, list: [] } }) } })
  assert.deepEqual(await request('/products', { query: { keyword: '高数 & Java', minPrice: 0, available: false, empty: '', missing: undefined } }), { total: 0, list: [] })
  assert.equal(sent.url, '/api/v1/products?keyword=%E9%AB%98%E6%95%B0+%26+Java&minPrice=0&available=false')
  assert.equal(sent.options.headers.Authorization, undefined)
})

test('真实认证传入 Bearer Token，序列化 JSON 请求体', async () => {
  const request = createHttpClient({ getToken: () => 'test-token', fetchImpl: async (_, options) => {
    assert.equal(options.headers.Authorization, 'Bearer test-token')
    assert.equal(options.headers['Content-Type'], 'application/json')
    assert.equal(options.body, '{"productId":12,"quantity":1}')
    return json({ code: 100000, data: { id: 1 } })
  } })
  assert.deepEqual(await request('/cart/items', { method: 'POST', authenticated: true, body: { productId: 12, quantity: 1 } }), { id: 1 })
})

test('缺少真实 Token 时拒绝认证请求，不发送网络请求', async () => {
  let sent = false
  const request = createHttpClient({ fetchImpl: () => { sent = true } })
  await assert.rejects(request('/orders', { authenticated: true }), e => e instanceof ApiError && e.status === 401)
  assert.equal(sent, false)
})

test('保留业务错误码、HTTP 状态和 traceId', async () => {
  const request = createHttpClient({ fetchImpl: async () => json({ code: 120009, message: '库存不足' }, 409, { 'X-Trace-Id': 'trace-test' }) })
  await assert.rejects(request('/orders'), e => e.message === '库存不足' && e.status === 409 && e.code === 120009 && e.traceId === 'trace-test')
})

test('HTTP 200 中的失败业务码也抛出错误', async () => {
  const request = createHttpClient({ fetchImpl: async () => json({ code: 110002, message: 'Token 已过期' }) })
  await assert.rejects(request('/user/info'), e => e.code === 110002)
})

test('HTML 错误页提示代理配置问题', async () => {
  const request = createHttpClient({ fetchImpl: async () => new Response('<html>Bad gateway</html>', { status: 502 }) })
  await assert.rejects(request('/products'), e => e instanceof ApiError && e.status === 502 && e.message.includes('非 JSON'))
})

test('超时会中止请求并提供明确错误', async () => {
  const request = createHttpClient({ timeoutMs: 10, fetchImpl: (_, { signal }) => new Promise((_, reject) => signal.addEventListener('abort', () => reject(new Error('aborted')))) })
  await assert.rejects(request('/products'), e => e instanceof ApiError && e.message.includes('超时'))
})

test('网络失败不暴露内部异常信息', async () => {
  const request = createHttpClient({ fetchImpl: async () => { throw new Error('internal details') } })
  await assert.rejects(request('/products'), e => e.message === '无法连接后端，请检查服务和代理配置')
})

test('204 删除响应返回 null', async () => {
  const request = createHttpClient({ fetchImpl: async () => new Response(null, { status: 204 }) })
  assert.equal(await request('/cart/items/1', { method: 'DELETE' }), null)
})
// ===================== P3: traceId 透传与令牌自动刷新 =====================

test('每个请求都带上 X-Trace-Id，前后端可对账同一次请求', async () => {
  let seen
  const request = createHttpClient({
    traceIdFactory: () => 'fixed-trace-0001',
    fetchImpl: async (_, options) => { seen = options.headers['X-Trace-Id']; return json({ code: 200, data: null }) },
  })
  await request('/products')
  assert.equal(seen, 'fixed-trace-0001')
})

test('401 时自动用 refreshToken 换新令牌并重放原请求', async () => {
  const calls = []
  let currentToken = 'old-token'
  const request = createHttpClient({
    getToken: () => currentToken,
    getRefreshToken: () => 'refresh-1',
    onTokenRefreshed: data => { currentToken = data.token },
    fetchImpl: async (url, options) => {
      calls.push({ url, auth: options.headers.Authorization, body: options.body })
      if (url.endsWith('/auth/refresh')) {
        return json({ code: 200, data: { token: 'new-token', refreshToken: 'refresh-2' } })
      }
      // 第一次用旧令牌请求返回 401，第二次用新令牌成功
      if (options.headers.Authorization === 'Bearer old-token') {
        return json({ code: 401, message: '登录已过期' }, 401)
      }
      return json({ code: 200, data: { ok: true } })
    },
  })

  assert.deepEqual(await request('/orders', { authenticated: true }), { ok: true })
  assert.equal(calls.length, 3)
  assert.equal(calls[0].auth, 'Bearer old-token')
  assert.equal(calls[1].url, '/api/v1/auth/refresh')
  assert.equal(calls[1].body, '{"refreshToken":"refresh-1"}')
  assert.equal(calls[2].auth, 'Bearer new-token')
})

test('并发 401 只触发一次刷新，避免一次性刷新令牌被并发消耗', async () => {
  let refreshCount = 0
  let currentToken = 'old-token'
  const request = createHttpClient({
    getToken: () => currentToken,
    getRefreshToken: () => 'refresh-1',
    onTokenRefreshed: data => { currentToken = data.token },
    fetchImpl: async (url, options) => {
      if (url.endsWith('/auth/refresh')) {
        refreshCount += 1
        await new Promise(r => setTimeout(r, 10))
        return json({ code: 200, data: { token: 'new-token', refreshToken: 'refresh-2' } })
      }
      if (options.headers.Authorization === 'Bearer old-token') return json({ code: 401, message: '过期' }, 401)
      return json({ code: 200, data: { ok: true } })
    },
  })

  const results = await Promise.all([
    request('/a', { authenticated: true }),
    request('/b', { authenticated: true }),
    request('/c', { authenticated: true }),
    request('/d', { authenticated: true }),
  ])
  assert.equal(refreshCount, 1, '四个并发请求应只刷新一次')
  results.forEach(r => assert.deepEqual(r, { ok: true }))
})

test('刷新失败时回调 onAuthExpired 并把错误抛给上层', async () => {
  let expired = 0
  const request = createHttpClient({
    getToken: () => 'old-token',
    getRefreshToken: () => 'bad-refresh',
    onAuthExpired: () => { expired += 1 },
    fetchImpl: async url => url.endsWith('/auth/refresh')
      ? json({ code: 401, message: '刷新令牌已失效' }, 401)
      : json({ code: 401, message: '过期' }, 401),
  })

  await assert.rejects(
    request('/orders', { authenticated: true }),
    e => e instanceof ApiError && e.status === 401,
  )
  assert.equal(expired, 1)
})

test('没有 refreshToken 时不尝试刷新，直接判定登录过期', async () => {
  let networkCalls = 0
  const request = createHttpClient({
    getToken: () => 'old-token',
    getRefreshToken: () => null,
    fetchImpl: async () => { networkCalls += 1; return json({ code: 401, message: '过期' }, 401) },
  })

  await assert.rejects(request('/orders', { authenticated: true }), e => e instanceof ApiError && e.status === 401)
  assert.equal(networkCalls, 1, '只应有原始请求，不应发出刷新请求')
})

test('非 401 错误不触发刷新，避免无意义的重试', async () => {
  let networkCalls = 0
  const request = createHttpClient({
    getToken: () => 'token',
    getRefreshToken: () => 'refresh-1',
    fetchImpl: async () => { networkCalls += 1; return json({ code: 500, message: '服务器错误' }, 500) },
  })

  await assert.rejects(request('/orders', { authenticated: true }))
  assert.equal(networkCalls, 1)
})