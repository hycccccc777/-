import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(here, '..')
const contractPath = path.resolve(root, '..', 'openapi', 'campus.openapi.json')

const contract = JSON.parse(fs.readFileSync(contractPath, 'utf8'))
const apiSource = fs.readFileSync(path.join(root, 'src', 'api', 'index.js'), 'utf8')

/**
 * 从源码里抽出前端实际发起的请求。
 *
 * <p>用扫描而不是正则硬抠：路径是拼接出来的（'/products/' + segment(id)），
 * 正则很容易在嵌套括号上出错。这里按括号配平截出参数，
 * 再把 segment(x) 还原成 OpenAPI 里的 {x} 模板。
 */
function extractCalls(source) {
  const calls = []
  const marker = 'request('
  let i = 0
  while (true) {
    const at = source.indexOf(marker, i)
    if (at < 0) break
    // 跳过对象属性名上的 request（例如 { request: ... }），只认函数调用
    i = at + marker.length

    // 配平括号，跳过字符串字面量
    let depth = 1
    let quote = null
    let end = -1
    for (let j = i; j < source.length; j++) {
      const ch = source[j]
      if (quote) {
        if (ch === '\\') { j++; continue }
        if (ch === quote) quote = null
        continue
      }
      if (ch === "'" || ch === '"' || ch === '`') { quote = ch; continue }
      if (ch === '(') depth++
      else if (ch === ')') { depth--; if (depth === 0) { end = j; break } }
    }
    if (end < 0) break
    const args = source.slice(i, end)
    i = end

    // 顶层逗号切分参数
    const parts = []
    let buf = ''
    depth = 0
    quote = null
    for (const ch of args) {
      if (quote) { buf += ch; if (ch === quote) quote = null; continue }
      if (ch === "'" || ch === '"' || ch === '`') { quote = ch; buf += ch; continue }
      if ('([{'.includes(ch)) depth++
      if (')]}'.includes(ch)) depth--
      if (ch === ',' && depth === 0) { parts.push(buf); buf = ''; continue }
      buf += ch
    }
    parts.push(buf)

    const rawPath = parts[0]
    if (!rawPath || !rawPath.includes("'")) continue
    // segment(x) → {x}；去掉加号与引号；去掉查询串
    let p = rawPath
      .replace(/segment\(\s*(\w+)\s*\)/g, '{$1}')
      .replace(/'/g, '')
      .replace(/\+/g, '')
      .replace(/\s+/g, '')          // 拼接处会留下空格，必须去掉
      .trim()
    p = p.split('?')[0]              // 去掉查询串
    if (!p.startsWith('/')) continue
    // 契约里的路径是全量路径（含 baseURL），前端写的是相对路径
    if (!p.startsWith('/api/v1')) p = '/api/v1' + p

    const methodMatch = parts.slice(1).join(',').match(/method:\s*'([A-Z]+)'/)
    calls.push({ path: p, method: methodMatch ? methodMatch[1].toLowerCase() : 'get' })
  }
  return calls
}

const calls = extractCalls(apiSource)

test('前端确实抽取到了请求（解析器本身没坏）', () => {
  assert.ok(calls.length >= 30, `应至少解析出 30 个请求，实际 ${calls.length}`)
})

/**
 * 把路径里的参数名抹掉，只保留结构。
 *
 * <p>路径参数是按位置替换的：前端写 '/cart/items/{id}'、后端契约写
 * '/cart/items/{cartItemId}'，运行时填进去的是同一个值，行为完全一致。
 * 变量名不同属于风格差异，不该判成契约不一致。
 */
function shape(p) {
  return p.split('/').map(seg => (seg.startsWith('{') && seg.endsWith('}') ? '{}' : seg)).join('/')
}

test('前端调用的每个接口都存在于 OpenAPI 契约中', () => {
  // 先按结构建索引，保留真实路径用于报错
  const index = new Map()
  for (const [p, item] of Object.entries(contract.paths)) {
    for (const method of Object.keys(item)) {
      if (method === 'parameters') continue
      const key = method + ' ' + shape(p)
      if (!index.has(key)) index.set(key, [])
      index.get(key).push(p)
    }
  }

  const missing = []
  for (const call of calls) {
    const key = call.method + ' ' + shape(call.path)
    if (!index.has(key)) {
      missing.push(`${call.method.toUpperCase()} ${call.path} —— 契约中无此接口`)
    }
  }
  assert.deepEqual(missing, [], '前端调用了契约里不存在的接口：\n' + missing.join('\n'))
})

test('契约里每个操作都有中文摘要（不允许无文档接口）', () => {
  const undocumented = []
  for (const [p, item] of Object.entries(contract.paths)) {
    for (const [method, op] of Object.entries(item)) {
      if (method === 'parameters') continue
      if (!op.summary || !op.summary.trim()) undocumented.push(`${method.toUpperCase()} ${p}`)
    }
  }
  assert.deepEqual(undocumented, [], '以下操作缺少 summary：\n' + undocumented.join('\n'))
})

test('契约里的每个操作都标注了安全方案', () => {
  // /api/v1 下除支付回调外都需要登录；契约应统一声明 apikey
  assert.ok(contract.components.securitySchemes.apikey, '缺少 apikey 安全方案')
  assert.ok(contract.security && contract.security.length > 0, '契约未声明全局安全要求')
})