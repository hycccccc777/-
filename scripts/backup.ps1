# 数据库备份：mysqldump 导出 shop 库，按日期归档并保留最近 N 份
# 用法：powershell -ExecutionPolicy Bypass -File scripts\backup.ps1 [-Keep 7]
param(
    [int]$Keep = 7
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$backupDir = Join-Path $root 'backups'
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$dbHost = if ($env:CAMPUS_DB_HOST) { $env:CAMPUS_DB_HOST } else { '127.0.0.1' }
$dbPort = if ($env:CAMPUS_DB_PORT) { $env:CAMPUS_DB_PORT } else { '3306' }
$dbUser = if ($env:CAMPUS_DB_USERNAME) { $env:CAMPUS_DB_USERNAME } else { 'root' }
$dbPass = $env:CAMPUS_DB_PASSWORD
$dbName = 'shop'

if (-not $dbPass) {
    Write-Host '[x] 未设置 CAMPUS_DB_PASSWORD 环境变量' -ForegroundColor Red
    exit 1
}

# 定位 mysqldump.exe：优先 PATH，其次 MySQL 服务安装目录
$dump = (Get-Command mysqldump.exe -ErrorAction SilentlyContinue).Source
if (-not $dump) {
    $svc = Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^MySQL' } | Select-Object -First 1
    if ($svc) {
        $exe = ($svc.PathName -replace '^"([^"]+)".*$', '$1')
        $cand = Join-Path (Split-Path $exe -Parent) 'mysqldump.exe'
        if (Test-Path $cand) { $dump = $cand }
    }
}
if (-not $dump) {
    Write-Host '[x] 找不到 mysqldump.exe，请安装 MySQL 客户端或加入 PATH' -ForegroundColor Red
    exit 1
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$file = Join-Path $backupDir ("shop-$stamp.sql")

Write-Host ("→ 备份 " + $dbName + " 到 " + $file)
$env:MYSQL_PWD = $dbPass   # 走环境变量，避免密码出现在命令行和进程列表里
& $dump "-h$dbHost" "-P$dbPort" "-u$dbUser" --single-transaction --routines --triggers --set-gtid-purged=OFF --default-character-set=utf8mb4 $dbName |
    Out-File -FilePath $file -Encoding utf8

if ($LASTEXITCODE -ne 0 -or -not (Test-Path $file)) {
    Write-Host '[x] 备份失败' -ForegroundColor Red
    exit 1
}

$size = [math]::Round((Get-Item $file).Length / 1KB, 1)
Write-Host ("[OK] 备份完成：" + $size + " KB") -ForegroundColor Green

# 轮转：只保留最近 $Keep 份
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
$old = Get-ChildItem $backupDir -Filter 'shop-*.sql' | Sort-Object LastWriteTime -Descending | Select-Object -Skip $Keep
foreach ($f in $old) {
    Remove-Item $f.FullName -Force
    Write-Host ("  - 清理旧备份 " + $f.Name)
}

Write-Host ("共保留 " + (Get-ChildItem $backupDir -Filter 'shop-*.sql').Count + " 份备份") -ForegroundColor Gray
