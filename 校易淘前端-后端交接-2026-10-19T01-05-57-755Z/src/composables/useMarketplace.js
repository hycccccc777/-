import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { schools } from '../data/products'
import { readStorage, writeStorage } from '../utils/storage'
import { authApi, productApi, cartApi, orderApi, paymentApi, refundApi, sellerApi, uploadApi, setAccessToken, clearAccessToken } from '../api'

// 当前为本地演示状态。接入真实 API 前，按 docs/接口对接说明.md 替换数据来源。
// 此处的昵称、金额和 localStorage 均不能作为服务端认证或定价依据。
export function useMarketplace() {
const categories = ref([{ id: 'all', label: '全部好物', icon: 'grid' }])
const storedProducts = readStorage('ct.products', [])
const customProducts = ref(Array.isArray(storedProducts) ? storedProducts.filter(p => p && typeof p.id === 'string' && typeof p.title === 'string' && Number.isFinite(p.price) && p.price > 0) : [])
const serverProducts = ref([])
const serverTotal = ref(0)
const listLoading = ref(false)
const listError = ref('')
const pageNo = ref(1)
const pageSize = ref(12)
const allProducts = computed(() => [...customProducts.value, ...serverProducts.value])
// 购物车由服务端维护：{ id: cartItemId, productId, quantity, selected, product }
const cart = ref([])
const storedFavorites = readStorage('ct.favorites', [])
const favorites = ref(Array.isArray(storedFavorites) ? storedFavorites.filter(x => typeof x === 'string') : [])
// 真实登录态：用户信息来自后端，令牌仅内存保存，浏览器刷新后需重新登录。
const user = ref(null)
// 订单来自服务端：已映射为页面展示结构
const orders = ref([])
const checkoutKey = ref('')
const sellerTab = ref('products')
const sellerProducts = ref([])
const sellerOrders = ref([])
const sellerRefunds = ref([])
const sellerLoading = ref(false)
const page = ref(['home', 'favorites', 'cart', 'profile', 'seller'].includes(location.hash.slice(1)) ? location.hash.slice(1) : 'home')
const keyword = ref('')
const category = ref('all')
const school = ref('全部学校')
const condition = ref('全部成色')
const sort = ref('recommended')
const detail = ref(null)
const detailOpen = computed({ get: () => Boolean(detail.value), set: value => { if (!value) detail.value = null } })
const loginOpen = ref(false)
const publishOpen = ref(false)
const checkoutOpen = ref(false)
const loginPhone = ref('')
const loginPassword = ref('')
const loginLoading = ref(false)
const loginError = ref('')
const address = ref('')
const receiver = ref('')
const contact = ref('')
const remark = ref('')
const submittingOrder = ref(false)
const payingOrder = ref('')
const publish = ref({ title: '', category: '', price: '', condition: '九成新', location: '', description: '' })
const publishImage = ref('')
const publishUploading = ref(false)
let storageWarned = false

function persist(key, value) {
  if (!writeStorage(key, value) && !storageWarned) {
    storageWarned = true
    ElMessage.warning('浏览器存储不可用，本次操作仅保留在当前页面')
  }
}
watch(favorites, v => persist('ct.favorites', v), { deep: true })
watch(customProducts, v => persist('ct.products', v), { deep: true })
watch(page, value => {
  location.hash = value
  window.scrollTo({ top: 0, behavior: 'smooth' })
  if (value === 'cart') loadCart()
  if (value === 'profile') loadOrders()
  if (value === 'seller') loadSellerCenter()
})
function syncHash() {
  const target = location.hash.slice(1)
  page.value = ['home', 'favorites', 'cart', 'profile', 'seller'].includes(target) ? target : 'home'
}
onMounted(() => { window.addEventListener('hashchange', syncHash); fetchCategories(); fetchProducts(false) })
onUnmounted(() => window.removeEventListener('hashchange', syncHash))

// 收藏页仍为本地收藏（需求暂未提供收藏接口）
const favoriteProducts = computed(() => allProducts.value.filter(p => favorites.value.includes(p.id)))
// 首页商品列表来自服务端；收藏页使用本地收藏
const displayProducts = computed(() => page.value === 'favorites' ? favoriteProducts.value : serverProducts.value)
const displayTotal = computed(() => page.value === 'favorites' ? favoriteProducts.value.length : serverTotal.value)
const hasMore = computed(() => page.value !== 'favorites' && serverProducts.value.length < serverTotal.value)

const CONDITION_CODES = { '几乎全新': 'almost_new', '九成新': 'nine', '八成新': 'eight' }
const ART_BY_CATEGORY = { '100': 'math', '101': 'headphones', '102': 'lamp', '103': 'bike', '104': 'bag' }
const COLOR_BY_CATEGORY = { '100': '#eeede3', '101': '#e8e7e4', '102': '#f0eadc', '103': '#e4ebe4', '104': '#ede8df' }
function minutesAgo(value) {
  const t = new Date(String(value || '').replace(/-/g, '/')).getTime()
  return Number.isFinite(t) ? Math.max(0, Math.round((Date.now() - t) / 60000)) : 0
}
function mapProduct(item) {
  const key = String(item.categoryId)
  return {
    id: String(item.productId),
    title: item.title,
    price: Number(item.price),
    originalPrice: item.originalPrice != null ? Number(item.originalPrice) : null,
    category: key,
    condition: item.conditionLabel || '',
    school: item.schoolName || '',
    location: item.location || '',
    seller: item.sellerName || '校园卖家',
    avatar: (item.sellerName || '校').slice(0, 1),
    description: item.description || '',
    tag: item.tag || '',
    cover: item.coverUrl || '',
    art: ART_BY_CATEGORY[key] || 'custom',
    color: COLOR_BY_CATEGORY[key] || '#ede8df',
    time: minutesAgo(item.createTime),
  }
}
async function fetchCategories() {
  try {
    const data = await productApi.categories()
    const list = Array.isArray(data) ? data : []
    categories.value = [{ id: 'all', label: '全部好物', icon: 'grid' }, ...list.map(c => ({ id: String(c.categoryId), label: c.categoryName, icon: c.icon || 'grid' }))]
  } catch (e) {
    // 类目接口失败时保留“全部好物”，商品列表仍可独立使用
  }
}
async function fetchProducts(append = false) {
  listLoading.value = true
  listError.value = ''
  try {
    const query = { page: pageNo.value, size: pageSize.value }
    if (keyword.value.trim()) query.keyword = keyword.value.trim()
    if (category.value !== 'all') query.categoryId = category.value
    if (school.value !== '全部学校') query.school = school.value
    const code = CONDITION_CODES[condition.value]
    if (code) query.condition = code
    query.sort = sort.value
    const data = await productApi.list(query)
    const mapped = (data && Array.isArray(data.list) ? data.list : []).map(mapProduct)
    serverProducts.value = append ? [...serverProducts.value, ...mapped] : mapped
    serverTotal.value = Number((data && data.total) || 0)
  } catch (error) {
    listError.value = error?.message || '商品加载失败'
    if (!append) { serverProducts.value = []; serverTotal.value = 0 }
  } finally {
    listLoading.value = false
  }
}
function refreshList() { pageNo.value = 1; fetchProducts(false) }
function loadMore() { if (page.value !== 'favorites' && serverProducts.value.length < serverTotal.value) { pageNo.value += 1; fetchProducts(true) } }
watch([keyword, category, school, condition, sort], () => { if (page.value !== 'favorites') refreshList() })
function mapCartProduct(p) {
  if (!p) return null
  const key = String(p.categoryId)
  return {
    id: String(p.id),
    title: p.title || '',
    price: Number(p.price || 0),
    originalPrice: p.originalPrice != null ? Number(p.originalPrice) : null,
    cover: p.coverUrl || '',
    condition: p.conditionLabel || '',
    school: p.schoolName || '',
    location: p.location || '',
    seller: p.sellerName || '',
    avatar: (p.sellerName || '校').slice(0, 1),
    sellerId: String(p.sellerId),
    stock: p.stock,
    art: ART_BY_CATEGORY[key] || 'custom',
    color: COLOR_BY_CATEGORY[key] || '#ede8df',
    isMine: String(p.sellerId) === String(user.value?.id),
  }
}
async function loadCart() {
  if (!user.value) { cart.value = []; return }
  try {
    const data = await cartApi.list()
    cart.value = (Array.isArray(data) ? data : []).map(item => ({
      id: String(item.cartItemId),
      productId: String(item.productId),
      quantity: item.quantity,
      selected: item.selected !== false,
      product: mapCartProduct(item.product),
    }))
  } catch (e) {
    cart.value = []
  }
}
function mapOrder(o) {
  return {
    no: o.orderNo,
    status: o.statusText || o.status,
    statusCode: o.status,
    total: Number(o.payAmount || 0),
    date: o.createTime || '',
    user: user.value?.name || '',
    seller: o.sellerName || '',
    cancellable: o.status === '0',
    payable: o.status === '0',
    receivable: o.status === '2',
    refundable: ['0', '1', '2'].includes(o.status),
    items: (o.items || []).map(i => ({
      id: String(i.productId),
      title: i.title || '',
      cover: i.coverUrl || '',
      price: Number(i.price || 0),
      quantity: i.quantity || 1,
      art: 'custom',
      color: '#ede8df',
    })),
  }
}
async function loadOrders() {
  if (!user.value) { orders.value = []; return }
  try {
    const data = await orderApi.list()
    orders.value = (Array.isArray(data) ? data : []).map(mapOrder)
  } catch (e) {
    orders.value = []
  }
}
async function cancelOrder(orderNo) {
  try {
    await orderApi.cancel(orderNo)
    ElMessage.success('订单已取消')
    await loadOrders()
    await loadCart()
    refreshList()
  } catch (error) {
    ElMessage.error(error?.message || '取消订单失败')
  }
}
async function payOrder(orderNo) {
  if (payingOrder.value) return
  payingOrder.value = orderNo
  try {
    const pay = await paymentApi.create({ orderNo, payMethod: '0' })
    await paymentApi.confirm(pay && pay.payNo)
    ElMessage.success('支付成功，等待卖家发货')
    await loadOrders()
    refreshList()
  } catch (error) {
    ElMessage.error(error?.message || '支付失败，请稍后重试')
  } finally {
    payingOrder.value = ''
  }
}
async function confirmReceive(orderNo) {
  try {
    await orderApi.confirm(orderNo)
    ElMessage.success('已确认收货，交易完成')
    await loadOrders()
    refreshList()
  } catch (error) {
    ElMessage.error(error?.message || '确认收货失败')
  }
}
async function applyRefund(orderNo) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt('请简单说明退款原因，卖家会尽快处理', '申请退款', {
      confirmButtonText: '提交申请',
      cancelButtonText: '再想想',
      inputPlaceholder: '例如：商品与描述不符 / 买错了',
      inputValidator: v => (v && String(v).trim().length >= 2) || '请填写至少 2 个字的退款原因',
    })
    reason = res.value || ''
  } catch (e) {
    return
  }
  try {
    await refundApi.apply(orderNo, { reason })
    ElMessage.success('退款申请已提交，等待卖家处理')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error?.message || '申请退款失败')
  }
}
const PRODUCT_STATUS = { '0': '待审核', '1': '已上架', '2': '已下架', '3': '审核驳回', '4': '已售出' }
function mapSellerProduct(p) {
  const key = String(p.categoryId)
  return {
    id: String(p.productId),
    title: p.title,
    price: Number(p.price),
    status: p.status,
    statusText: PRODUCT_STATUS[p.status] || p.status,
    stock: p.stock,
    viewCount: p.viewCount || 0,
    cover: p.coverUrl || '',
    categoryName: p.categoryName || '',
    createTime: p.createTime || '',
    art: ART_BY_CATEGORY[key] || 'custom',
    color: COLOR_BY_CATEGORY[key] || '#ede8df',
  }
}
async function loadSellerCenter() {
  if (!user.value) {
    sellerProducts.value = []; sellerOrders.value = []; sellerRefunds.value = []
    return
  }
  sellerLoading.value = true
  try {
    const results = await Promise.all([sellerApi.products(), sellerApi.soldOrders(), sellerApi.incomingRefunds()])
    sellerProducts.value = (Array.isArray(results[0]) ? results[0] : []).map(mapSellerProduct)
    sellerOrders.value = (Array.isArray(results[1]) ? results[1] : []).map(mapOrder)
    sellerRefunds.value = Array.isArray(results[2]) ? results[2] : []
  } catch (e) {
    ElMessage.error(e?.message || '卖家中心数据加载失败')
  } finally {
    sellerLoading.value = false
  }
}
async function toggleProductStatus(product) {
  const next = product.status === '1' ? '2' : '1'
  try {
    await sellerApi.setProductStatus(product.id, next)
    ElMessage.success(next === '1' ? '已重新上架' : '商品已下架')
    await loadSellerCenter()
    refreshList()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}
async function removeMyProduct(product) {
  try {
    await ElMessageBox.confirm('确定删除「' + product.title + '」吗？删除后不可恢复。', '删除商品', { confirmButtonText: '删除', cancelButtonText: '再想想', type: 'warning' })
  } catch { return }
  try {
    await sellerApi.removeProduct(product.id)
    ElMessage.success('商品已删除')
    await loadSellerCenter()
    refreshList()
  } catch (error) {
    ElMessage.error(error?.message || '删除失败')
  }
}
async function shipOrder(orderNo) {
  try {
    await sellerApi.shipOrder(orderNo)
    ElMessage.success('已发货，等待买家确认收货')
    await loadSellerCenter()
  } catch (error) {
    ElMessage.error(error?.message || '发货失败')
  }
}
async function agreeRefund(refundNo) {
  try { await sellerApi.agreeRefund(refundNo); ElMessage.success('已同意退款'); await loadSellerCenter() }
  catch (error) { ElMessage.error(error?.message || '操作失败') }
}
async function completeRefund(refundNo) {
  try { await sellerApi.completeRefund(refundNo); ElMessage.success('退款已完成，商品已重新上架'); await loadSellerCenter(); refreshList() }
  catch (error) { ElMessage.error(error?.message || '操作失败') }
}
async function rejectRefund(refundNo) {
  let remark = ''
  try {
    const res = await ElMessageBox.prompt('请填写拒绝原因，买家会看到', '拒绝退款', {
      confirmButtonText: '确认拒绝', cancelButtonText: '取消',
      inputValidator: v => (v && String(v).trim().length >= 2) || '请填写至少 2 个字',
    })
    remark = res.value || ''
  } catch { return }
  try { await sellerApi.rejectRefund(refundNo, { remark }); ElMessage.success('已拒绝退款'); await loadSellerCenter() }
  catch (error) { ElMessage.error(error?.message || '操作失败') }
}
const cartProducts = computed(() => cart.value.map(item => ({ id: item.id, quantity: item.quantity, selected: item.selected, product: item.product })).filter(x => x.product))
const selectedItems = computed(() => cartProducts.value.filter(x => x.selected))
const total = computed(() => selectedItems.value.reduce((sum, item) => sum + Math.round(item.product.price * 100), 0) / 100)
const allSelected = computed(() => cartProducts.value.length > 0 && cartProducts.value.every(x => x.selected))
const myProducts = computed(() => customProducts.value.filter(p => p.seller === user.value?.name))

function money(amount) { return Number(amount).toFixed(2) }
function notify(message) { ElMessage.success(message) }
function requireUser() {
  if (user.value) return true
  loginOpen.value = true
  ElMessage.info('请先登录')
  return false
}
function toggleFavorite(id) {
  favorites.value = favorites.value.includes(id) ? favorites.value.filter(x => x !== id) : [...favorites.value, id]
}
async function addCart(product) {
  if (!requireUser()) return
  if (product.isMine) return ElMessage.info('不能购买自己发布的商品')
  try {
    await cartApi.add({ productId: Number(product.id), quantity: 1 })
    await loadCart()
    notify('已加入购物车')
  } catch (error) {
    ElMessage.error(error?.message || '加入购物车失败')
  }
}
async function removeCart(cartItemId) {
  try {
    await cartApi.remove(cartItemId)
    await loadCart()
  } catch (error) {
    ElMessage.error(error?.message || '移除失败')
  }
}
async function updateQty(cartItemId, quantity) {
  if (!Number.isFinite(quantity) || quantity < 1) return ElMessage.warning('数量至少为 1')
  try {
    await cartApi.update(cartItemId, { quantity })
    await loadCart()
  } catch (error) {
    ElMessage.error(error?.message || '修改数量失败')
  }
}
function selectItem(id, checked) { const item = cart.value.find(x => x.id === id); if (item) item.selected = checked }
function toggleAll(checked) { cart.value.forEach(x => { x.selected = checked }) }
function resetFilters() { keyword.value = ''; category.value = 'all'; school.value = '全部学校'; condition.value = '全部成色'; sort.value = 'recommended' }
function navigate(target) { page.value = target; if (target === 'home' || target === 'favorites') resetFilters() }
function search(term) {
  keyword.value = term ?? keyword.value
  page.value = 'home'
  requestAnimationFrame(() => document.getElementById('market')?.scrollIntoView({ behavior: 'smooth' }))
}
function normalizeUser(profile, fallbackPhone) {
  const src = profile || {}
  const name = src.name || src.nickname || (fallbackPhone ? '同学' + String(fallbackPhone).slice(-4) : '校园用户')
  return {
    id: src.id != null ? String(src.id) : '',
    name,
    nickname: src.nickname || name,
    avatar: src.avatar || '',
    phone: src.phone || fallbackPhone || '',
    roles: Array.isArray(src.roles) ? src.roles : [],
    role: src.role || 'user',
  }
}
async function submitLogin() {
  loginError.value = ''
  const phone = loginPhone.value.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) { loginError.value = '请输入有效的 11 位手机号'; return ElMessage.warning(loginError.value) }
  if (!loginPassword.value) { loginError.value = '请输入密码'; return ElMessage.warning(loginError.value) }
  loginLoading.value = true
  try {
    const data = await authApi.login({ phone, password: loginPassword.value })
    setAccessToken(data?.token || '')
    let profile = data?.user
    try {
      const info = await authApi.userInfo()
      if (info) profile = { ...profile, ...info }
    } catch (e) {
      // 登录已成功，用户信息拉取失败时沿用登录响应中的用户资料
    }
    user.value = normalizeUser(profile, phone)
    loadCart()
    loadOrders()
    loadSellerCenter()
    loginOpen.value = false
    loginPassword.value = ''
    notify('登录成功，欢迎回来，' + user.value.name)
  } catch (error) {
    loginError.value = error?.message || '登录失败，请稍后重试'
    ElMessage.error(loginError.value)
  } finally {
    loginLoading.value = false
  }
}
async function logout() {
  try { await authApi.logout() } catch (e) { /* 忽略：本地退出即可 */ }
  clearAccessToken()
  user.value = null
  cart.value = []
  orders.value = []
  sellerProducts.value = []
  sellerOrders.value = []
  sellerRefunds.value = []
  notify('已退出登录')
}
function openPublish() {
  if (!requireUser()) return
  const first = categories.value.find(c => c.id !== 'all')
  publish.value = { title: '', category: first ? first.id : '', price: '', condition: '九成新', location: '', description: '' }
  publishImage.value = ''
  publishUploading.value = false
  publishOpen.value = true
}
async function uploadPublishImage(file) {
  if (!file) return
  if (!file.type || !file.type.startsWith('image/')) return ElMessage.warning('请选择图片文件')
  if (file.size > 5 * 1024 * 1024) return ElMessage.warning('图片大小不能超过 5MB')
  publishUploading.value = true
  try {
    const data = await uploadApi.image(file)
    publishImage.value = data?.url || ''
    notify('图片上传成功')
  } catch (error) {
    ElMessage.error(error?.message || '图片上传失败，请稍后重试')
  } finally {
    publishUploading.value = false
  }
}
async function submitPublish() {
  const p = publish.value
  const price = Number(p.price)
  if (p.title.trim().length < 4) return ElMessage.warning('商品标题至少填写 4 个字')
  if (!p.category) return ElMessage.warning('请选择商品类目')
  if (!Number.isFinite(price) || price < 0.01 || price > 99999) return ElMessage.warning('请输入 0.01～99999 元之间的价格')
  if (!p.location.trim() || p.description.trim().length < 10) return ElMessage.warning('请填写交接地点与至少 10 个字的描述')
  if (!requireUser()) return
  try {
    await productApi.create({
      title: p.title.trim(),
      categoryId: Number(p.category),
      price: Math.round(price * 100) / 100,
      condition: CONDITION_CODES[p.condition] || '',
      description: p.description.trim(),
      location: p.location.trim(),
      images: publishImage.value ? [publishImage.value] : [],
    })
    publishOpen.value = false
    publish.value = { title: '', category: '', price: '', condition: '九成新', location: '', description: '' }
    publishImage.value = ''
    navigate('home')
    refreshList()
    notify('发布成功，已上架')
  } catch (error) {
    ElMessage.error(error?.message || '发布失败，请稍后重试')
  }
}
function startCheckout() {
  if (!selectedItems.value.length) return ElMessage.warning('请先选择要结算的商品')
  if (!requireUser()) return
  // 同一次结算使用同一个幂等键，网络重试不会重复下单
  checkoutKey.value = 'ck-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
  checkoutOpen.value = true
}
async function submitOrder() {
  if (!requireUser()) return
  if (!receiver.value.trim() || !address.value.trim()) return ElMessage.warning('请填写收货人和收货地址')
  if (!/^1[3-9]\d{9}$/.test(contact.value)) return ElMessage.warning('请输入有效的 11 位手机号')
  if (!selectedItems.value.length) return ElMessage.warning('请先选择商品')
  submittingOrder.value = true
  try {
    const data = await orderApi.create({
      cartItemIds: selectedItems.value.map(x => Number(x.id)),
      receiver: receiver.value.trim(),
      phone: contact.value.trim(),
      addressDetail: address.value.trim(),
      deliveryType: '0',
      remark: remark.value.trim(),
      idempotencyKey: checkoutKey.value,
    })
    checkoutOpen.value = false
    detail.value = null
    receiver.value = ''; contact.value = ''; address.value = ''; remark.value = ''
    await loadCart()
    await loadOrders()
    refreshList()
    page.value = 'profile'
    const count = (data && data.orders) ? data.orders.length : 1
    notify(count > 1 ? '下单成功，已按卖家拆为 ' + count + ' 笔订单' : '下单成功，请在「我的订单」查看')
  } catch (error) {
    ElMessage.error(error?.message || '下单失败，请稍后重试')
  } finally {
    submittingOrder.value = false
  }
}

return { categories, schools, page, keyword, category, school, condition, sort, detail, detailOpen, loginOpen, publishOpen, checkoutOpen, loginPhone, loginPassword, loginLoading, loginError, address, receiver, contact, remark, submittingOrder, publish, publishImage, publishUploading, uploadPublishImage, user, favorites, orders, displayProducts, displayTotal, listLoading, listError, hasMore, loadMore, refreshList, cartProducts, selectedItems, total, allSelected, myProducts, money, toggleFavorite, addCart, removeCart, updateQty, selectItem, toggleAll, loadCart, loadOrders, cancelOrder, payOrder, confirmReceive, applyRefund, payingOrder, loadSellerCenter, sellerTab, sellerProducts, sellerOrders, sellerRefunds, sellerLoading, toggleProductStatus, removeMyProduct, shipOrder, agreeRefund, completeRefund, rejectRefund, resetFilters, navigate, search, submitLogin, logout, openPublish, submitPublish, startCheckout, submitOrder }
}
