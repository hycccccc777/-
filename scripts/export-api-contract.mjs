#!/usr/bin/env node
/**
 * 导出 /api/v1 的 OpenAPI 合约。
 *
 * <p>用法：
 *   node scripts/export-api-contract.mjs                      # 启动后端、导出、关停
 *   node scripts/export-api-contract.mjs --base http://...   # 对已在运行的服务导出
 *   node scripts/export-api-contract.mjs --check              # 只比对不写盘，有漂移则退出码 1
 *
 * <p>为什么要有 --check：CI 里用它确认「代码里的接口」与
 * 「仓库里提交的契约快照」一致，防止有人加了接口却忘了更新快照，
 * 或者改了路径却没同步前端。
 */
import { spawn } from 'node:child_process'
import { writeFileSync, readFileSync, existsSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const repoRoot = resolve(here, '..')
const contractPath = resolve(repoRoot, 'openapi', 'campus.openapi.json')
const backendDir = resolve(repoRoot, 'RuoYi-Vue-master', 'RuoYi-Vue-master')

const args = process.argv.slice(2)
const checkOnly = args.includes('--check')
const baseArg = args.indexOf('--base')
const base = baseArg >= 0 ? args[baseArg + 1] : 'http://localhost:8080'

/** 规范化为可比较的稳定形式：递归排序键，避免仅顺序不同就报漂移 */
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical)
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k])]))
  }
  return value
}

function sameJson(a, b) {
  return JSON.stringify(canonical(a)) === JSON.stringify(canonical(b))
}

async function fetchContract() {
  const res = await fetch(`${base}/v3/api-docs/campus`)
  if (!res.ok) throw new Error(`拉取契约失败：HTTP ${res.status}`)
  return await res.json()
}

/** 列出两个契约的差异（按 路径+方法 对齐） */
function diff(oldDoc, newDoc) {
  const lines = []
  const ops = doc => {
    const map = new Map()
    for (const [p, item] of Object.entries(doc.paths || {})) {
      for (const [m, op] of Object.entries(item)) {
        if (m === 'parameters') continue
        map.set(`${m.toUpperCase()} ${p}`, op.summary || '')
      }
    }
    return map
  }
  const a = ops(oldDoc)
  const b = ops(newDoc)
  for (const [k, v] of b) {
    if (!a.has(k)) lines.push(`+ 新增 ${k}  ${v}`)
    else if (a.get(k) !== v) lines.push(`~ 摘要变化 ${k}  "${a.get(k)}" → "${v}"`)
  }
  for (const [k, v] of a) {
    if (!b.has(k)) lines.push(`- 删除 ${k}  ${v}`)
  }
  return lines
}

let child = null
if (baseArg < 0) {
  // 没指定外部服务时，自己把后端拉起来
  const jar = resolve(backendDir, 'ruoyi-admin', 'target', 'ruoyi-admin.jar')
  if (!existsSync(jar)) {
    console.error(`找不到 ${jar}，请先执行 mvn -pl ruoyi-admin -am -DskipTests package`)
    process.exit(1)
  }
  console.log('启动后端…')
  child = spawn('java', ['-jar', jar], { cwd: backendDir, stdio: 'ignore', env: process.env })
  // 等待服务就绪
  const deadline = Date.now() + 120000
  for (;;) {
    if (Date.now() > deadline) {
      console.error('后端启动超时')
      child.kill()
      process.exit(1)
    }
    try {
      const r = await fetch(`${base}/v3/api-docs/campus`)
      if (r.ok) break
    } catch { /* 还没起来，继续等 */ }
    await new Promise(r => setTimeout(r, 2000))
  }
}

try {
  const live = await fetchContract()
  if (!existsSync(contractPath)) {
    if (checkOnly) {
      console.error(`契约快照不存在：${contractPath}`)
      process.exit(1)
    }
    writeFileSync(contractPath, JSON.stringify(live, null, 2) + '\n', 'utf8')
    console.log(`已生成契约：${contractPath}`)
    process.exit(0)
  }
  const committed = JSON.parse(readFileSync(contractPath, 'utf8'))
  if (sameJson(committed, live)) {
    console.log('✓ 契约与代码一致，无漂移')
    process.exit(0)
  }
  const lines = diff(committed, live)
  console.error('✗ 接口契约发生漂移：')
  lines.forEach(l => console.error('  ' + l))
  if (!checkOnly) {
    writeFileSync(contractPath, JSON.stringify(live, null, 2) + '\n', 'utf8')
    console.error(`已更新契约快照：${contractPath}（请一并提交）`)
  }
  process.exit(1)
} finally {
  if (child) child.kill()
}