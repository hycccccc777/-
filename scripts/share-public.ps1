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

function Test-LanUp($port, $path, $ip) {
    if (-not $ip) { return $false }
    try {
        $r = Invoke-WebRequest -Uri ("http://${ip}:$port$path") -UseBasicParsing -TimeoutSec 4
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
    $c = Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.IPv4Address } | Select-Object -First 1
    if ($c) { return $c.IPv4Address.IPAddress }
    $a = Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | Select-Object -First 1
    if ($a) { return $a.IPAddress }
    return $null
}

function Ensure-FirewallRule($port) {
    $name = "CampusTrade-Frontend-$port"
    $existing = Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue
    if ($existing) { return $true }
    try {
        New-NetFirewallRule -DisplayName $name -Direction Inbound -Protocol TCP -LocalPort $port -Action Allow -Profile Any -ErrorAction Stop | Out-Null
        Say "      已添加防火墙放行规则（TCP $port）" Green
        return $true
    } catch {
        Say "      [!] 无法自动添加防火墙规则（需要管理员权限）。请手动放行：" Yellow
        Say "          控制面板 -> Windows Defender 防火墙 -> 高级设置 -> 入站规则 -> 新建规则 -> 端口 -> TCP $port -> 允许连接" Yellow
        return $false
    }
}

function Stop-Listener($port) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        try { Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue } catch { }
    }
    Start-Sleep -Seconds 2
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
# 先强制写出 .env.local，避免残留 127.0.0.1 导致外部访问失败
$envLocal = Join-Path $feDir '.env.local'
$envLines = @()
$envLines += "DEV_PORT=$FrontendPort"
$envLines += "DEV_HOST=0.0.0.0"
$envLines += "DEV_API_TARGET=http://127.0.0.1:$BackendPort"
$envLines += "DEV_ALLOWED_HOSTS=.trycloudflare.com,localhost,127.0.0.1"
$envLines | Set-Content -Path $envLocal -Encoding UTF8

$lan = Get-LanIp

# 如果端口已起就直接复用：即使它只绑了 127.0.0.1，公网隧道连的也是 127.0.0.1，
# 仍能正常打开；贸然杀掉反而会把正在工作的 tunnel 变成 502。
if (Test-Up $FrontendPort '/') {
    Say ("[2/3] 前端已在运行  http://127.0.0.1:$FrontendPort") Green
    if (-not (Test-LanUp $FrontendPort '/' $lan)) {
        Say '      （局域网地址可能不可用，但公网隧道不受影响）' DarkGray
    }
}

if (-not (Test-Up $FrontendPort '/')) {
    if (-not (Test-Path (Join-Path $feDir 'node_modules'))) {
        Say '[2/3] 安装前端依赖（首次约 1-3 分钟）...'
        Push-Location $feDir
        try { & npm.cmd ci --registry=https://registry.npmmirror.com } finally { Pop-Location }
    } else { Say '[2/3] 启动前端...' }
    # 隐藏窗口 + 日志落盘：避免误关窗口把 Vite 一起带走，出问题时也能看日志
    $viteLog = Join-Path $env:TEMP 'campus-vite.log'
    Remove-Item $viteLog -ErrorAction SilentlyContinue
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "cd /d `"$feDir`" && npm run dev > `"$viteLog`" 2>&1") -WindowStyle Hidden | Out-Null
    if (-not (Wait-Up $FrontendPort '/' 90)) {
        Say '[x] 前端启动超时，日志如下（%TEMP%\campus-vite.log）:' Red
        if (Test-Path $viteLog) { Get-Content $viteLog -Encoding UTF8 -ErrorAction SilentlyContinue | Select-Object -Last 20 | ForEach-Object { Say ("      " + $_) DarkGray } }
        exit 1
    }
    Say '      前端就绪' Green
}

# 防火墙：不放行的话局域网/公网都进不来
Ensure-FirewallRule $FrontendPort | Out-Null

# 局域网可达性自检
$lanOk = Test-LanUp $FrontendPort '/' $lan
if (-not $lanOk) {
    Say '[!] 局域网自检失败：别人大概率打不开。常见原因：' Yellow
    Say '    1. 防火墙未放行（上面的自动添加失败时需要手动）' Yellow
    Say '    2. 电脑与手机不在同一 WiFi / 路由器开启了 AP 隔离' Yellow
    Say '    3. 企业网络禁止端口互通' Yellow
}

# ---------- 3) 公网隧道 ----------
$publicUrl = $null
if (-not $NoTunnel) {
    if (-not (Test-Path $cf)) {
        Say '[3/3] 首次需要下载 cloudflared（约 50MB，国内可能较慢）...'
        try {
            Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' `
                -OutFile $cf -UseBasicParsing -TimeoutSec 600
        } catch {
            Say ('[!] 下载失败（' + $_.Exception.Message + '），跳过公网隧道，仅局域网可用') Yellow
        }
    }
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
            Say '      隧道已建立' Green
        } else {
            Say '[!] 没能取到公网地址，请查看 %TEMP%\cloudflared-share.log' Yellow
            Say '    常见原因：国内网络无法访问 cloudflare；可改用 ngrok（需注册）或把电脑部署到云服务器' Yellow
        }
    }
}

# ---------- 汇总 ----------
# 端到端自检：隧道必须能真的打开前端（不只是进程在跑）
if ($publicUrl) {
    $tunnelOk = $false
    for ($i = 0; $i -lt 15; $i++) {
        try {
            $r = Invoke-WebRequest -Uri $publicUrl -UseBasicParsing -TimeoutSec 8
            if ($r.StatusCode -eq 200 -and $r.Content -match 'id="app"') { $tunnelOk = $true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    if ($tunnelOk) {
        Say '      公网地址自检通过' Green
    } else {
        Say '[!] 公网地址暂时打不开（隧道已建但源站不可达）' Yellow
        if (-not (Test-Up $FrontendPort '/')) {
            Say '    前端已停止，请重新运行本脚本；日志见 %TEMP%\campus-vite.log' Yellow
        } else {
            Say '    前端在跑但隧道访问失败，检查 %TEMP%\cloudflared-share.log' Yellow
        }
    }
}

Say ''
Say '============================================================' Green
Say '  给别人打开这些地址：' Green
Say '============================================================' Green
if ($publicUrl) { Say ("    公网（任何网络）  " + $publicUrl) White }
if ($lan -and $lanOk) { Say ("    同一 WiFi        http://${lan}:$FrontendPort") White }
Say ("    本机             http://localhost:$FrontendPort") White
Say ''
Say '  说明：' DarkGray
Say '    · 公网地址走 Cloudflare quick tunnel，每次重启都会变，用完即弃' DarkGray
Say '    · 别人只需要打开前端地址；/api 由前端开发服务器反代到后端，无需额外开放端口' DarkGray
Say '    · 要固定域名/长期使用：部署到服务器，或用 cloudflared named tunnel' DarkGray
Say '    · 演示账号：管理员 15888888888 / admin123；买家 13800001001 / admin123' DarkGray
Say ''

if (-not $NoBrowser) { Start-Process ("http://localhost:$FrontendPort") | Out-Null }