# 数据库备份：mysqldump 导出 shop 库，按日期归档、保留最近 N 份、可选异地副本
#
# 用法：powershell -ExecutionPolicy Bypass -File scripts\backup.ps1 [-Keep 7] [-Offsite D:\db-backup]
#
# 由 scripts\install-backup-task.ps1 注册为每日计划任务；
# 手动执行时从环境变量 CAMPUS_DB_PASSWORD 取密码（计划任务靠包装脚本注入）。
param(
    [int]$Keep = 7,
    [string]$Offsite
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$backupDir = Join-Path $root 'backups'
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $backupDir, $logDir | Out-Null
$logFile = Join-Path $logDir 'backup.log'

function Write-Log($msg, $color = 'Gray') {
    $line = '[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ' + $msg
    Write-Host $line -ForegroundColor $color
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}

Write-Log '=== 开始备份 ==='

$dbHost = if ($env:CAMPUS_DB_HOST) { $env:CAMPUS_DB_HOST } else { '127.0.0.1' }
$dbPort = if ($env:CAMPUS_DB_PORT) { $env:CAMPUS_DB_PORT } else { '3306' }
$dbUser = if ($env:CAMPUS_DB_USERNAME) { $env:CAMPUS_DB_USERNAME } else { 'root' }
$dbPass = $env:CAMPUS_DB_PASSWORD
$dbName = 'shop'

if (-not $dbPass) {
    Write-Log '[x] 未设置 CAMPUS_DB_PASSWORD：计划任务请用 install-backup-task.ps1 安装（会注入凭据）' 'Red'
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
    Write-Log '[x] 找不到 mysqldump.exe，请安装 MySQL 客户端或加入 PATH' 'Red'
    exit 1
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$file = Join-Path $backupDir ("shop-$stamp.sql")

Write-Log ("→ 备份 " + $dbName + " 到 " + $file)
$env:MYSQL_PWD = $dbPass   # 走环境变量，避免密码出现在命令行与进程列表
& $dump "-h$dbHost" "-P$dbPort" "-u$dbUser" --single-transaction --routines --triggers --set-gtid-purged=OFF --default-character-set=utf8mb4 $dbName |
    Out-File -FilePath $file -Encoding utf8
$dumpExit = $LASTEXITCODE
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue

# 只判「文件存在」不够：dump 失败时也会留下一个空壳文件
if ($dumpExit -ne 0 -or -not (Test-Path $file) -or (Get-Item $file).Length -lt 1024) {
    Write-Log ('[x] 备份失败（dump 退出码 ' + $dumpExit + '，文件大小 ' + $(if (Test-Path $file) { (Get-Item $file).Length } else { 0 }) + ' 字节）') 'Red'
    if (Test-Path $file) { Remove-Item $file -Force -ErrorAction SilentlyContinue }
    exit 1
}

$size = [math]::Round((Get-Item $file).Length / 1KB, 1)
$sha = (Get-FileHash $file -Algorithm SHA256).Hash
Write-Log ("[OK] 备份完成：" + $size + " KB  sha256=" + $sha.Substring(0, 16) + "...") 'Green'
Add-Content -Path (Join-Path $backupDir 'manifest.txt') `
    -Value ("$stamp`t$size KB`t$sha") -Encoding UTF8

# 异地副本：本机磁盘坏了，同盘备份一起没
if ($Offsite) {
    try {
        New-Item -ItemType Directory -Force -Path $Offsite | Out-Null
        Copy-Item $file (Join-Path $Offsite (Split-Path $file -Leaf)) -Force
        $oldOff = Get-ChildItem $Offsite -Filter 'shop-*.sql' | Sort-Object LastWriteTime -Descending | Select-Object -Skip $Keep
        foreach ($f in $oldOff) { Remove-Item $f.FullName -Force; Write-Log ("  - 清理异地旧备份 " + $f.Name) }
        Write-Log ("[OK] 异地副本：" + $Offsite + "（保留 " + (Get-ChildItem $Offsite -Filter 'shop-*.sql').Count + " 份）") 'Green'
    } catch {
        Write-Log ('[x] 异地复制失败：' + $_.Exception.Message) 'Red'
        exit 1
    }
}

# 轮转：只保留最近 $Keep 份
$old = Get-ChildItem $backupDir -Filter 'shop-*.sql' | Sort-Object LastWriteTime -Descending | Select-Object -Skip $Keep
foreach ($f in $old) {
    Remove-Item $f.FullName -Force
    Write-Log ("  - 清理旧备份 " + $f.Name)
}

Write-Log ("共保留 " + (Get-ChildItem $backupDir -Filter 'shop-*.sql').Count + " 份，日志见 logs\backup.log")
exit 0