# 发布与回滚（本地可跑的最小实现）
#
# 为什么需要：回滚方案不演练等于没有。这个脚本把「发布 - 记录版本 - 回滚」
# 做成可重复的两条命令，演练时只需要跑一遍并记录耗时。
#
# 版本目录结构（与容器里按镜像 tag 回退是同一套思路）：
#   releases/
#     v-20261025-231500-a1b2c3d/ruoyi-admin.jar
#     v-20261025-232000-e4f5g6h/ruoyi-admin.jar
#   releases/current.txt        <- 当前生效的版本号
#
# 用法：
#   powershell -File scripts\release.ps1 -Action deploy    构建并发布新版本
#   powershell -File scripts\release.ps1 -Action rollback  回退到上一个版本
#   powershell -File scripts\release.ps1 -Action list      查看历史版本
param(
    [ValidateSet('deploy', 'rollback', 'list')]
    [string]$Action = 'list',
    [int]$Port = 8080,
    [int]$HealthTimeout = 60
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$base = Join-Path $repo 'RuoYi-Vue-master\RuoYi-Vue-master'

# 工具定位：不依赖 PATH，与其它脚本保持一致的查找顺序
function Resolve-Mvn {
    $cmd = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
    if ($cmd) { return $cmd }
    $cand = Join-Path $env:LOCALAPPDATA 'campus-maven\apache-maven-3.9.9\bin\mvn.cmd'
    if (Test-Path $cand) { return $cand }
    foreach ($d in @((Join-Path $env:TEMP 'maven-dist\apache-maven-3.9.16'), (Join-Path $env:LOCALAPPDATA 'campus-maven\apache-maven-3.9.16'))) {
        $c = Join-Path $d 'bin\mvn.cmd'
        if (Test-Path $c) { return $c }
    }
    throw '找不到 mvn.cmd：请安装 Maven 或运行 启动后端.cmd 让脚本自动下载'
}

function Resolve-Java {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return (Join-Path $env:JAVA_HOME 'bin\java.exe')
    }
    $cmd = (Get-Command java.exe -ErrorAction SilentlyContinue).Source
    if ($cmd) { return $cmd }
    throw '找不到 java.exe：请设置 JAVA_HOME'
}
$relDir = Join-Path $repo 'releases'
$currentFile = Join-Path $relDir 'current.txt'
$jarName = 'ruoyi-admin.jar'
New-Item -ItemType Directory -Force -Path $relDir | Out-Null

function Log($m, $c = 'Gray') { Write-Host $m -ForegroundColor $c }

# 数据库口令：环境变量优先，其次读仓库根 .env（与 启动后端.cmd 一致）。
# 缺了就直接停在这里 —— 让它跑到 Druid 初始化再报 "Access denied" 只会浪费一轮排查。
function Ensure-DbPassword {
    if ($env:CAMPUS_DB_PASSWORD) { return }
    $envFile = Join-Path $repo '.env'
    if (Test-Path $envFile) {
        foreach ($line in Get-Content $envFile -Encoding UTF8) {
            if ($line -match '^\s*CAMPUS_DB_PASSWORD\s*=\s*(.+)$') {
                $env:CAMPUS_DB_PASSWORD = $Matches[1].Trim()
                return
            }
        }
    }
    Log '[x] 未找到数据库口令：请在仓库根 .env 写 CAMPUS_DB_PASSWORD=...（模板见 .env.example）' 'Red'
    exit 1
}
function Now { Get-Date }

function Get-Versions {
    Get-ChildItem $relDir -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName $jarName) } |
        Sort-Object Name
}

function Get-Current {
    if (Test-Path $currentFile) { return (Get-Content $currentFile -Raw).Trim() }
    return $null
}

function Stop-Backend($port) {
    # 1) 先停指定端口的进程
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($x in $conn) {
        Stop-Process -Id $x.OwningProcess -Force -ErrorAction SilentlyContinue
    }

    # 2) 再停所有「从本仓库 jar 启动」的 java 进程。
    #    只按端口停是不够的：灰度实例（如 8083）也读同一个 target\ruoyi-admin.jar，
    #    只要有一个还活着，repackage 就会 "Unable to rename ...jar.original"。
    $jarPath = Join-Path $base ("ruoyi-admin\target\" + $jarName)
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and ($_.CommandLine -like "*ruoyi-admin.jar*" -or $_.CommandLine -like "*$relDir*") } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }

    # 3) 等端口与控制权释放
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Milliseconds 500
        if (-not (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)) { return }
    }
}

function Start-Backend($jar, $port) {
    $env:LOG_PATH = (Join-Path $repo 'logs')
    New-Item -ItemType Directory -Force -Path $env:LOG_PATH | Out-Null
    $outLog = Join-Path $env:LOG_PATH ('release-' + $port + '.out.log')
    $errLog = Join-Path $env:LOG_PATH ('release-' + $port + '.err.log')
    $proc = Start-Process -FilePath (Resolve-Java) -ArgumentList @('-jar', $jar, "--server.port=$port") `
        -WorkingDirectory $base -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru -WindowStyle Hidden
    return $proc
}

function Wait-Healthy($port, $timeoutSec) {
    for ($i = 0; $i -lt $timeoutSec; $i++) {
        Start-Sleep -Seconds 1
        try {
            $r = Invoke-WebRequest -Uri ("http://127.0.0.1:$port/api/v1/system/health") -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) { return $i + 1 }
        } catch { }
    }
    return -1
}

Ensure-DbPassword

$versions = Get-Versions
Log ''
Log ('=== 版本列表（' + $versions.Count + ' 个）===')
foreach ($v in $versions) {
    $mark = if ($v.Name -eq (Get-Current)) { ' <- 当前' } else { '' }
    Log ('  ' + $v.Name + $mark)
}

switch ($Action) {
    'list' { exit 0 }

    'deploy' {
        $ver = 'v-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
        $target = Join-Path $relDir $ver
        New-Item -ItemType Directory -Force -Path $target | Out-Null

        Log ''
        # 先停进程再构建：Windows 下运行中的 jar 被锁，repackage 会 "Unable to rename ...jar.original"
        Log '[0/4] 停止当前实例（避免锁住 target 下的 jar）'
        Stop-Backend $Port

        Log '[1/4] 构建（mvn package -DskipTests）'
        Push-Location $base
        & (Resolve-Mvn) -B -q -pl ruoyi-admin -am -DskipTests package
        $buildOk = ($LASTEXITCODE -eq 0)
        Pop-Location
        if (-not $buildOk) { Log '[x] 构建失败' 'Red'; exit 1 }

        Log '[2/4] 归档到 releases/' 
        Copy-Item (Join-Path $base "ruoyi-admin\target\$jarName") (Join-Path $target $jarName) -Force

        Log '[3/4] 记录当前版本'
        $previous = Get-Current
        Set-Content -Path $currentFile -Value $ver -Encoding UTF8

        Log '[4/4] 切换到新版本并探活'
        Stop-Backend $Port
        $t0 = Now
        $null = Start-Backend (Join-Path $target $jarName) $Port
        $sec = Wait-Healthy $Port $HealthTimeout
        $elapsed = ((Now) - $t0).TotalSeconds
        if ($sec -lt 0) {
            Log ('[x] 新版本未就绪，自动回滚到 ' + $previous) 'Red'
            if ($previous) { & $PSCommandPath -Action rollback -Port $Port }
            exit 1
        }
        Log ('[OK] 发布完成：' + $ver + '  就绪耗时 ' + [math]::Round($elapsed,1) + 's') 'Green'
        if ($previous) { Log ('     上一版本：' + $previous + '（可用 -Action rollback 回退）') }
        exit 0
    }

    'rollback' {
        $current = Get-Current
        if (-not $current) { Log '[x] 没有 current.txt，无法判断回滚目标' 'Red'; exit 1 }
        $all = @(Get-Versions | ForEach-Object { $_.Name })
        $idx = [array]::IndexOf($all, $current)
        if ($idx -le 0) { Log ('[x] 当前是第一个版本（' + $current + '），没有可回退的历史版本') 'Red'; exit 1 }
        $targetVer = $all[$idx - 1]
        $targetJar = Join-Path (Join-Path $relDir $targetVer) $jarName

        Log ''
        Log ('=== 回滚开始：' + $current + ' -> ' + $targetVer + ' ===')
        $t0 = Now

        Log '[1/3] 停止当前进程'
        Stop-Backend $Port
        $tStop = ((Now) - $t0).TotalSeconds

        Log '[2/3] 切换版本'
        Set-Content -Path $currentFile -Value $targetVer -Encoding UTF8
        $tSwitch = ((Now) - $t0).TotalSeconds

        Log '[3/3] 启动目标版本并探活'
        $null = Start-Backend $targetJar $Port
        $sec = Wait-Healthy $Port $HealthTimeout
        $elapsed = ((Now) - $t0).TotalSeconds

        if ($sec -lt 0) { Log '[x] 回滚后仍不健康，需要人工介入' 'Red'; exit 1 }
        Log ''
        Log ('[OK] 回滚完成') 'Green'
        Log ('     停止   : ' + [math]::Round($tStop,1) + 's')
        Log ('     切版本 : ' + [math]::Round($tSwitch,1) + 's')
        Log ('     总耗时 : ' + [math]::Round($elapsed,1) + 's（目标 ≤600s，SLO 内）')
        exit 0
    }
}