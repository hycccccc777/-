# 让别人打开：起后端 + 前端（绑 0.0.0.0）+ 公网隧道，最后打印可分享的地址
#
# 为什么要脚本：手动要起三个进程、改两处配置、还要处理防火墙与 Vite 的 Host 校验，
# 任何一步漏了都表现为「别人打不开」。这里把三条路径都做出来并实测。
#
# 用法：powershell -ExecutionPolicy Bypass -File scripts\share-public.ps1
param(
    [int]$FrontendPort = 5174,
    [int]$BackendPort = 8080,
    [switch]$NoTunnel,
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$feDir = Join-Path $repo '校易淘前端-后端交接-2026-10-19T01-05-57-755Z'
$beDir = Join-Path $repo 'RuoYi-Vue-master\RuoYi-Vue-master'
$cf = Join-Path $env:TEMP 'cloudflared.exe'

function Say($m, $c = 'Gray') { Write-Host $m -ForegroundColor $c }

function Test-Up($port, $path) {
    try {
        $r = Invoke-WebRequest -Uri ("http://127.0.0.1:$port$path") -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -eq 200)
    } catch { return $false }
}

function Wait-Up($port, $path, $seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        if (Test-Up $port $path) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Get-LanIp {
    # 取有默认网关的那块网卡地址，避免拿到 169.254.x 这类无意义地址
    $c = Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.IPv4Address } | Select-Object -First 1
    if ($c) { return $c.IPv4Address.IPAddress }
    $a = Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | Select-Object -First 1
    if ($a) { return $a.IPAddress }
    return $null
}

Say ''
Say '============================================================' Cyan
Say '  校易淘 · 让别人打开（后端 + 前端 + 公网隧道）' Cyan
Say '============================================================' Cyan
Say ''

# ---------- 0) 数据库口令 ----------
if (-not $env:CAMPUS_DB_PASSWORD) {
    $envFile = Join-Path $repo '.env'
    if (Test-Path $envFile) {
        foreach ($line in Get-Content $envFile -Encoding UTF8) {
            if ($line -match '^\s*CAMPUS_DB_PASSWORD\s*=\s*(.+)$') { $env:CAMPUS_DB_PASSWORD = $Matches[1].Trim(); break }
        }
    }
}
if (-not $env:CAMPUS_DB_PASSWORD) {
    Say '[x] 缺少数据库口令：请在仓库根 .env 写 CAMPUS_DB_PASSWORD=你的密码（模板见 .env.example）' Red
    exit 1
}

# ---------- 1) 后端 ----------
if (Test-Up $BackendPort '/api/v1/system/health') {
    Say ("[1/3] 后端已在运行  http://127.0.0.1:$BackendPort") Green
} else {
    $jar = Join-Path $beDir 'ruoyi-admin\target\ruoyi-admin.jar'
    if (-not (Test-Path $jar)) {
        Say '[1/3] 后端未构建，正在打包（首次约 1-2 分钟）...'
        $mvn = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
        if (-not $mvn) { $mvn = Join-Path $env:LOCALAPPDATA 'campus-maven\apache-maven-3.9.9\bin\mvn.cmd' }
        if (-not (Test-Path $mvn)) { Say '[x] 找不到 Maven，请先运行 启动后端.cmd 让它自动准备环境' Red; exit 1 }
        Push-Location $beDir
        try { & $mvn -q -pl ruoyi-admin -am -DskipTests package } finally { Pop-Location }
    }
    Say '[1/3] 启动后端...'
    Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar, "--server.port=$BackendPort") `
        -WorkingDirectory $beDir -WindowStyle Minimized | Out-Null
    if (-not (Wait-Up $BackendPort '/api/v1/system/health' 120)) {
        Say '[x] 后端启动超时，看 logs 目录下的日志' Red
        exit 1
    }
    Say '      后端就绪' Green
}

# ---------- 2) 前端（必须绑 0.0.0.0 才能被外部访问）----------
if (Test-Up $FrontendPort '/') {
    Say ("[2/3] 前端已在运行  http://127.0.0.1:$FrontendPort") Green
} else {
    if (-not (Test-Path (Join-Path $feDir 'node_modules'))) {
        Say '[2/3] 安装前端依赖（首次约 1-3 分钟）...'
        Push-Location $feDir
        try { & npm.cmd ci --registry=https://registry.npmmirror.com } finally { Pop-Location }
    } else { Say '[2/3] 启动前端...' }
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "cd /d `"$feDir`" && npm run dev") -WindowStyle Minimized | Out-Null
    if (-not (Wait-Up $FrontendPort '/' 90)) {
        Say '[x] 前端启动超时' Red
        exit 1
    }
    Say '      前端就绪（已监听 0.0.0.0）' Green
}

# ---------- 3) 公网隧道 ----------
$publicUrl = $null
if (-not $NoTunnel) {
    if (-not (Test-Path $cf)) {
        Say '[3/3] 首次需要下载 cloudflared（约 50MB）...'
        try {
            Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' `
                -OutFile $cf -UseBasicParsing -TimeoutSec 600
        } catch {
            Say ('[!] 下载失败（' + $_.Exception.Message + '），跳过公网隧道，仅局域网可用') Yellow
        }
    }
    # 已有 cloudflared 在跑就复用它的地址：重复起隧道会留下多个进程，
    # 而且只有最新的那个地址有效，容易把人搞混
    $existing = Get-Process cloudflared -ErrorAction SilentlyContinue
    if ($existing) {
        $prevLog = Join-Path $env:TEMP 'cloudflared-share.log'
        if (Test-Path $prevLog) {
            $m0 = [regex]::Match((Get-Content $prevLog -Raw -ErrorAction SilentlyContinue), 'https://[a-z0-9-]+\.trycloudflare\.com')
            if ($m0.Success) {
                $publicUrl = $m0.Value
                Say ('[3/3] 复用已运行的公网隧道：' + $publicUrl) Green
            }
        }
    }
    if (-not $publicUrl -and (Test-Path $cf)) {
        $cfLog = Join-Path $env:TEMP 'cloudflared-share.log'
        Remove-Item $cfLog -ErrorAction SilentlyContinue
        Say '[3/3] 建立公网隧道...'
        Start-Process -FilePath $cf -ArgumentList @('tunnel', '--url', "http://127.0.0.1:$FrontendPort", '--no-autoupdate') `
            -RedirectStandardError $cfLog -WindowStyle Hidden | Out-Null
        for ($i = 0; $i -lt 60; $i++) {
            Start-Sleep -Seconds 1
            if (Test-Path $cfLog) {
                $m = [regex]::Match((Get-Content $cfLog -Raw -ErrorAction SilentlyContinue), 'https://[a-z0-9-]+\.trycloudflare\.com')
                if ($m.Success) { $publicUrl = $m.Value; break }
            }
        }
        if ($publicUrl) {
            # 隧道域名要进 Vite 的 allowedHosts，否则会被 Host 校验 403
            $envLocal = Join-Path $feDir '.env.local'
            $lines = if (Test-Path $envLocal) { Get-Content $envLocal -Encoding UTF8 } else { @() }
            if (-not ($lines -match '^DEV_ALLOWED_HOSTS=')) {
                Add-Content -Path $envLocal -Value 'DEV_ALLOWED_HOSTS=.trycloudflare.com,localhost,127.0.0.1' -Encoding UTF8
            }
            if (-not ($lines -match '^DEV_HOST=')) {
                Add-Content -Path $envLocal -Value 'DEV_HOST=0.0.0.0' -Encoding UTF8
            }
            Say '      隧道已建立' Green
        } else {
            Say '[!] 没能取到公网地址，请查看 %TEMP%\cloudflared-share.log' Yellow
        }
    }
}

# ---------- 汇总 ----------
$lan = Get-LanIp
Say ''
Say '============================================================' Green
Say '  给别人打开这些地址：' Green
Say '============================================================' Green
if ($publicUrl) { Say ("    公网（任何网络）  " + $publicUrl) White }
if ($lan) { Say ("    同一 WiFi        http://${lan}:$FrontendPort") White }
Say ("    本机             http://localhost:$FrontendPort") White
Say ''
Say '  说明：' DarkGray
Say '    · 公网地址走 Cloudflare quick tunnel，每次重启都会变，用完即弃' DarkGray
Say '    · 别人只需要打开前端地址；/api 由前端开发服务器反代到后端，无需额外开放端口' DarkGray
Say '    · 要固定域名/长期使用：部署到服务器，或用 cloudflared named tunnel' DarkGray
Say '    · 演示账号：买家 13800001001 / admin123，卖家 13800001002 / admin123' DarkGray
Say ''

if (-not $NoBrowser) { Start-Process ("http://localhost:$FrontendPort") | Out-Null }