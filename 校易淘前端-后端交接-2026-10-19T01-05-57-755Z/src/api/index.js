import { createHttpClient } from './http'

let accessToken = null
let refreshToken = null
let authExpiredHandler = () => {}

// 只供正式登录成功后设置，演示昵称不会生成或设置 Token。
export function setAccessToken(token) { accessToken = token || null }
export function clearAccessToken() { accessToken = null }

/** 登录/刷新成功后可一次性写入两个令牌 */
export function setTokens({ token, refreshToken: nextRefreshToken } = {}) {
  accessToken = token || null
  refreshToken = nextRefreshToken || null
}

export function clearTokens() {
  accessToken = null
  refreshToken = null
}

/** 由应用层注册：刷新失败时清状态并跳登录 */
export function onAuthExpired(handler) {
  authExpiredHandler = typeof handler === 'function' ? handler : () => {}
}

const request = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeoutMs: Number(import.meta.env.VITE_API_TIMEOUT_MS || 10000),
  getToken: () => accessToken,
  getRefreshToken: () => refreshToken,
  onTokenRefreshed: data => {
    // 后端每次刷新都会下发新的 refreshToken（一次性语义），必须一并更新，
    // 否则第二次刷新会因旧令牌已作废而失败
    setTokens(data)
  },
  onAuthExpired: () => authExpiredHandler(),
})

const segment = value => encodeURIComponent(String(value))
const auth = { authenticated: true }

// 预留真实后端调用入口，当前演示页面尚未调用。以已有需求文档路径为准。
// 参数需要使用服务端 ID，不能直接传本地 p1、books 等演示标识。
export const systemApi = {
  health: () => request('/system/health'),
}
export const authApi = {
  login: body => request('/auth/login', { method: 'POST', body }),
  logout: () => request('/auth/logout', { method: 'POST', ...auth }),
  userInfo: () => request('/user/info', auth),
}
export const productApi = {
  list: query => request('/products', { query }),
  detail: id => request('/products/' + segment(id)),
  categories: () => request('/categories'),
  create: body => request('/products', { method: 'POST', body, ...auth }),
}
export const cartApi = {
  list: () => request('/cart', auth),
  add: body => request('/cart/items', { method: 'POST', body, ...auth }),
  update: (id, body) => request('/cart/items/' + segment(id), { method: 'PUT', body, ...auth }),
  remove: id => request('/cart/items/' + segment(id), { method: 'DELETE', ...auth }),
}
export const orderApi = {
  create: body => request('/orders', { method: 'POST', body, ...auth }),
  list: query => request('/orders', { query, ...auth }),
  detail: orderNo => request('/orders/' + segment(orderNo), auth),
  cancel: orderNo => request('/orders/' + segment(orderNo) + '/cancel', { method: 'PUT', ...auth }),
  confirm: orderNo => request('/orders/' + segment(orderNo) + '/confirm', { method: 'PUT', ...auth }),
}
export const paymentApi = {
  create: body => request('/pay/create', { method: 'POST', body, ...auth }),
  status: payNo => request('/pay/' + segment(payNo) + '/status', auth),
  // 沙箱支付确认：演示环境直接完成支付
  confirm: payNo => request('/pay/' + segment(payNo) + '/confirm', { method: 'POST', ...auth }),
}
export const uploadApi = {
  // 上传图片，返回 { url, fileName }；url 形如 /profile/upload/...
  image: file => {
    const form = new FormData()
    form.append('file', file)
    return request('/upload', { method: 'POST', body: form, ...auth })
  },
}
export const refundApi = {
  apply: (orderNo, body) => request('/orders/' + segment(orderNo) + '/refund', { method: 'POST', body, ...auth }),
  mine: () => request('/refunds', auth),
  incoming: () => request('/refunds/incoming', auth),
  agree: refundNo => request('/refunds/' + segment(refundNo) + '/agree', { method: 'POST', ...auth }),
  reject: (refundNo, body) => request('/refunds/' + segment(refundNo) + '/reject', { method: 'POST', body, ...auth }),
  complete: refundNo => request('/refunds/' + segment(refundNo) + '/complete', { method: 'POST', ...auth }),
}
export const sellerApi = {
  products: query => request('/my/products', { query, ...auth }),
  updateProduct: (id, body) => request('/my/products/' + segment(id), { method: 'PUT', body, ...auth }),
  setProductStatus: (id, status) => request('/my/products/' + segment(id) + '/status?status=' + encodeURIComponent(status), { method: 'PUT', ...auth }),
  removeProduct: id => request('/my/products/' + segment(id), { method: 'DELETE', ...auth }),
  soldOrders: () => request('/orders/sold', auth),
  shipOrder: orderNo => request('/orders/' + segment(orderNo) + '/ship', { method: 'PUT', ...auth }),
  incomingRefunds: () => request('/refunds/incoming', auth),
  agreeRefund: refundNo => request('/refunds/' + segment(refundNo) + '/agree', { method: 'POST', ...auth }),
  rejectRefund: (refundNo, body) => request('/refunds/' + segment(refundNo) + '/reject', { method: 'POST', body, ...auth }),
  completeRefund: refundNo => request('/refunds/' + segment(refundNo) + '/complete', { method: 'POST', ...auth }),
}