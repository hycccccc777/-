# 校易淘 · 本地打包镜像（在你自己的电脑上运行，服务器无需编译）
# 用法：双击「打包镜像.cmd」，或 powershell -File scripts\package-images.ps1 -Only all
param([ValidateSet("all", "frontend", "backend")][string]$Only = "all")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Ok($m)   { Write-Host "[OK]   $m" -ForegroundColor Green }
function Step($m) { Write-Host "== $m ==" -ForegroundColor Cyan }
function Fail($m) { Write-Host "[FAIL] $m" -ForegroundColor Red; exit 1 }

try { & docker compose version | Out-Null } catch { Fail "未检测到 docker compose，请先启动 Docker Desktop" }

# 统一构建成 linux/amd64，保证能跑在常见的 x86_64 云服务器上
$env:DOCKER_DEFAULT_PLATFORM = "linux/amd64"

$out = Join-Path $root "发布包"
New-Item -ItemType Directory -Force -Path $out | Out-Null

$services = switch ($Only) {
    "all"      { @("frontend", "backend") }
    "frontend" { @("frontend") }
    "backend"  { @("backend") }
}

Step ("构建镜像：" + ($services -join ", ") + "（首次可能要几分钟）")
& docker compose build @services
if ($LASTEXITCODE -ne 0) { Fail "docker compose build 失败" }

Step "导出镜像为 tar"
$images = @()
foreach ($svc in $services) { $images += "campus-$svc:latest" }
$tar = Join-Path $out "campus-images.tar"
if (Test-Path $tar) { Remove-Item $tar -Force }
& docker save -o $tar @images
if ($LASTEXITCODE -ne 0) { Fail "docker save 失败" }
Ok ("镜像包：" + $tar + "（" + [math]::Round((Get-Item $tar).Length / 1MB, 1) + " MB）")

Step "同步部署所需的配置文件到「发布包」"
Copy-Item "docker-compose.yml" $out -Force
Copy-Item ".env.example" $out -Force
if (Test-Path "docker-compose.override.yml") { Copy-Item "docker-compose.override.yml" $out -Force }
New-Item -ItemType Directory -Force -Path (Join-Path $out "scripts") | Out-Null
if (Test-Path "scripts\clean-demo.sql") { Copy-Item "scripts\clean-demo.sql" (Join-Path $out "scripts") -Force }

$readme = @"
# 校易淘 发布包（本地构建，服务器只加载）

## 1. 传到服务器
把整个「发布包」目录上传到服务器，例如 /root/campus-release/

## 2. 服务器上加载镜像（不编译，内存占用很小）
cd /root/campus-release
docker load -i campus-images.tar

## 3. 准备 .env（若服务器已有可跳过）
cp .env.example .env
vi .env   # 填 MYSQL_ROOT_PASSWORD / CAMPUS_JWT_SECRET，SPRING_PROFILES_ACTIVE=druid,prod

## 4. 启动（不重新构建）
docker compose up -d --no-build

## 提示
- `docker load` 主要吃磁盘、不吃内存；如失败先看 `df -h` 和 `docker system df`。
- 磁盘紧张可先 `docker image prune -f` / `docker builder prune -f`。
"@
Set-Content -Path (Join-Path $out "部署说明.txt") -Value $readme -Encoding UTF8

Ok ("完成，发布包目录：" + $out)
Write-Host "接下来：把「发布包」整个传到服务器 → docker load -i campus-images.tar → docker compose up -d --no-build" -ForegroundColor Yellow
