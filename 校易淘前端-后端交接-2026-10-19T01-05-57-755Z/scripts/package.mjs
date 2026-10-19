import { createHash } from 'node:crypto'
import { cp, mkdir, readFile, readdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
if (process.platform !== 'win32') {
  throw new Error('此打包命令使用 Windows PowerShell；其他系统请按 README 白名单手动压缩源码。')
}
// 使用白名单，不遍历 node_modules、dist、.env.local 或上次交付包。
const roots = ['src', 'docs', 'tests', 'scripts', 'deploy', 'index.html', 'package.json', 'package-lock.json', 'vite.config.js', '.gitignore', '.env.example', 'start.cmd', 'README.md', '验证记录.md']
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const name = '校易淘前端-后端交接-' + stamp
const output = path.join(root, '交付包')
const staging = path.join(output, name)
await mkdir(staging, { recursive: true })
for (const item of roots) await cp(path.join(root, item), path.join(staging, item), { recursive: true, errorOnExist: true, force: false })

async function walk(dir) {
  const paths = []
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) paths.push(...await walk(full))
    else if (entry.isFile()) paths.push(full)
    else throw new Error('交付目录不允许符号链接等非普通文件：' + full)
  }
  return paths.sort()
}
const files = []
for (const file of await walk(staging)) {
  const bytes = await readFile(file)
  files.push({ path: path.relative(staging, file).replaceAll('\\', '/'), bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex') })
}
await writeFile(path.join(staging, '交付清单.json'), JSON.stringify({ createdAt: new Date().toISOString(), description: '前端源码与接口准备；不含后端、依赖、密钥和真实用户数据', files }, null, 2), 'utf8')

const archive = path.join(output, name + '.zip')
// 路径只作为参数传给 PowerShell，避免把路径拼成可执行代码。
const script = "Add-Type -AssemblyName System.IO.Compression; Add-Type -AssemblyName System.IO.Compression.FileSystem; $source=$env:CT_HANDOFF_SOURCE; $stream=[IO.File]::Open($env:CT_HANDOFF_ZIP,[IO.FileMode]::CreateNew); $zip=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create,$false,[Text.Encoding]::UTF8); try { Get-ChildItem -LiteralPath $source -Recurse -File -Force | ForEach-Object { $relative=$_.FullName.Substring($source.Length).TrimStart([char]92); $entry=([IO.Path]::GetFileName($source)+'/'+$relative).Replace([char]92,[char]47); [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,$entry,[IO.Compression.CompressionLevel]::Optimal) | Out-Null } } finally { $zip.Dispose(); $stream.Dispose() }"
const result = spawnSync('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command', script], { encoding: 'utf8', windowsHide: true, env: { ...process.env, CT_HANDOFF_SOURCE: staging, CT_HANDOFF_ZIP: archive } })
if (result.status !== 0) throw new Error(result.stderr || result.error?.message || '打包失败')
const sha256 = createHash('sha256').update(await readFile(archive)).digest('hex')
await writeFile(archive + '.sha256', sha256 + '  ' + path.basename(archive) + '\n', 'utf8')
console.log('ZIP: ' + archive)
console.log('Files: ' + files.length)
console.log('SHA256: ' + sha256)