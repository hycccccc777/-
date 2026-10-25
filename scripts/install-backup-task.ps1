# 安装「每日数据库备份」Windows 计划任务
#
# 为什么需要这个脚本：backup.ps1 早就写好了，但从来没有人定时跑它 ——
# 没有调度的备份等于没有备份。这个脚本把「谁、什么时候、用什么凭据、日志写到哪」固化下来，
# 并在装完后立刻跑一次，确认真的能产出文件（而不是「配好了但跑不起来」）。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\install-backup-task.ps1 -DbPassword "密码"
#   powershell -ExecutionPolicy Bypass -File scripts\install-backup-task.ps1 -DbPassword "密码" -Time 03:30 -Keep 14 -Offsite "D:\db-backup"
#
# 实现说明：
#   * 口令写入 %LOCALAPPDATA%\campus-shop\db-password（ACL 仅当前用户可读）
#   * 任务动作是 powershell.exe 调一个包装 .ps1，避免 cmd 引号与 %%a 转义的坑（踩过两次）
param(
    [Parameter(Mandatory = $true)][string]$DbPassword,
    [string]$Time = '02:00',
    [int]$Keep = 7,
    [string]$Offsite,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
Import-Module ScheduledTasks -ErrorAction Stop

$repo = Split-Path -Parent $PSScriptRoot
$taskName = 'CampusShop-DailyBackup'
$credDir = Join-Path $env:LOCALAPPDATA 'campus-shop'
$passFile = Join-Path $credDir 'db-password'
$runner = Join-Path $credDir 'run-backup.ps1'
New-Item -ItemType Directory -Force -Path $credDir, (Join-Path $repo 'logs') | Out-Null

function Log($m, $c = 'Gray') { Write-Host $m -ForegroundColor $c }

$existing = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($existing -and -not $Force) {
    Log "[!] 已存在计划任务 $taskName，如需重建请加 -Force" 'Yellow'
    exit 1
}
if ($existing) { Unregister-ScheduledTask -TaskName $taskName -Confirm:$false }

# 1) 口令文件（仅当前用户可读）
[System.IO.File]::WriteAllText($passFile, $DbPassword, (New-Object System.Text.UTF8Encoding($false)))
& icacls $passFile /inheritance:r /grant:r "$($env:USERNAME):(R,W)" 2>&1 | Out-Null
Log ("[OK] 口令文件 " + $passFile + "（ACL 限制为当前用户）")

# 2) 包装脚本：注入环境变量后调用 backup.ps1
$offsiteArg = if ($Offsite) { " -Offsite `"$Offsite`"" } else { '' }
$runnerText = @"
# 由 install-backup-task.ps1 生成，勿手工修改
`$ErrorActionPreference = 'Stop'
`$env:CAMPUS_DB_PASSWORD = (Get-Content -Raw `"$passFile`").Trim()
& `"$repo\scripts\backup.ps1`" -Keep $Keep$offsiteArg
exit `$LASTEXITCODE
"@
[System.IO.File]::WriteAllText($runner, $runnerText, (New-Object System.Text.UTF8Encoding($true)))
Log ("[OK] 包装脚本 " + $runner)

# 3) 注册计划任务（用 ScheduledTasks 模块，避免 schtasks 的引号拼接问题）
$action = New-ScheduledTaskAction -Execute 'powershell.exe' `
    -Argument ('-NoProfile -ExecutionPolicy Bypass -File "{0}"' -f $runner)
$trigger = New-ScheduledTaskTrigger -Daily -At $Time
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -DontStopIfGoingOnBatteries -AllowStartIfOnBatteries
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings `
    -Description '校易淘：每日备份 shop 库（含异地副本与保留策略）' -Force | Out-Null
Log ("[OK] 计划任务已创建（每天 " + $Time + "）") 'Green'

# 4) 立刻跑一次：验证不是「配好了但跑不起来」
Log '[..] 立即触发一次，验证能产出文件'
$before = @(Get-ChildItem (Join-Path $repo 'backups') -Filter 'shop-*.sql' -ErrorAction SilentlyContinue).Count
Start-ScheduledTask -TaskName $taskName
$ok = $false
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 1
    $after = @(Get-ChildItem (Join-Path $repo 'backups') -Filter 'shop-*.sql' -ErrorAction SilentlyContinue).Count
    if ($after -gt $before) { $ok = $true; break }
}
if ($ok) {
    $latest = Get-ChildItem (Join-Path $repo 'backups') -Filter 'shop-*.sql' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    Log ("[OK] 验证通过：新备份 " + $latest.Name + "  " + [math]::Round($latest.Length/1KB,1) + " KB") 'Green'
} else {
    $info = Get-ScheduledTaskInfo -TaskName $taskName
    Log ('[x] 任务已注册，但触发后没有产出文件（LastTaskResult=' + $info.LastTaskResult + '）') 'Red'
    Log ('    日志：' + (Join-Path $repo 'logs\backup.log'))
    exit 1
}

Log ''
$info = Get-ScheduledTaskInfo -TaskName $taskName
Log ('  下次运行：' + $info.NextRunTime)
Log ('  上次结果：' + $info.LastTaskResult + '（0 表示成功）')