# 数据库恢复：从 backups 目录的 dump 还原 shop 库
# 用法：powershell -ExecutionPolicy Bypass -File scripts\restore.ps1 [-File 备份.sql] [-TargetDb shop]
param(
    [string]$File,
    [string]$TargetDb = 'shop'
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$backupDir = Join-Path $root 'backups'

if (-not $File) {
    $File = Get-ChildItem $backupDir -Filter 'shop-*.sql' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $File -or -not (Test-Path $File)) {
    Write-Host '[x] 找不到备份文件，请用 -File 指定' -ForegroundColor Red
    exit 1
}

$dbUser = if ($env:CAMPUS_DB_USERNAME) { $env:CAMPUS_DB_USERNAME } else { 'root' }
$dbPass = $env:CAMPUS_DB_PASSWORD
if (-not $dbPass) {
    Write-Host '[x] 未设置 CAMPUS_DB_PASSWORD 环境变量' -ForegroundColor Red
    exit 1
}

$mysql = (Get-Command mysql.exe -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    $svc = Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^MySQL' } | Select-Object -First 1
    if ($svc) {
        $exe = ($svc.PathName -replace '^"([^"]+)".*$', '$1')
        $cand = Join-Path (Split-Path $exe -Parent) 'mysql.exe'
        if (Test-Path $cand) { $mysql = $cand }
    }
}
if (-not $mysql) {
    Write-Host '[x] 找不到 mysql.exe' -ForegroundColor Red
    exit 1
}

Write-Host ("→ 从 " + (Split-Path $File -Leaf) + " 恢复到库 " + $TargetDb)

# 建库（若不存在）
$env:MYSQL_PWD = $dbPass   # 走环境变量，避免密码出现在命令行
& $mysql "-u$dbUser" -e "CREATE DATABASE IF NOT EXISTS ``$TargetDb`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
if ($LASTEXITCODE -ne 0) { Write-Host '[x] 建库失败' -ForegroundColor Red; exit 1 }

# 还原
Get-Content $File -Raw -Encoding UTF8 | & $mysql "-u$dbUser" --default-character-set=utf8mb4 $TargetDb
if ($LASTEXITCODE -ne 0) {
    Write-Host '[x] 恢复失败' -ForegroundColor Red
    exit 1
}

$count = (& $mysql "-u$dbUser" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$TargetDb';") -join ''
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
Write-Host ("[OK] 恢复完成，" + $count.Trim() + " 张表") -ForegroundColor Green
