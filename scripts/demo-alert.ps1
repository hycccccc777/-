# 告警送达演示：本地起一个 webhook 接收器，推一条测试告警给 Alertmanager，
# 然后确认这条告警真的被路由并投递到了接收端。
#
# 为什么要有这个：规则写得再对，没人收到也没用。这个脚本把"最后一公里"跑通一次，
# 顺带验证 alertmanager.yml 的 receiver / route 配置是通的。
#
# 前提：
#   1) Alertmanager 已按 docs/ops/alertmanager.yml 启动（容器或本地二进制均可）
#   2) 把 receiver 的 url 指到本脚本监听的地址（默认 http://127.0.0.1:5001/alerts）
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\demo-alert.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\demo-alert.ps1 -Alertmanager http://localhost:9093 -Port 5001
param(
    [string]$Alertmanager = 'http://localhost:9093',
    [int]$Port = 5001,
    [int]$WaitSeconds = 40,
    [string]$AlertName = 'CampusDemoAlert'
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $repo 'logs'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$received = Join-Path $outDir 'alert-webhook.log'
Remove-Item $received -ErrorAction SilentlyContinue

# 1) 起接收器
$listener = New-Object System.Net.HttpListener
$prefix = "http://127.0.0.1:$Port/"
$listener.Prefixes.Add($prefix)
try { $listener.Start() } catch { Write-Host ('[x] 端口 ' + $Port + ' 无法监听：' + $_.Exception.Message) -ForegroundColor Red; exit 1 }
Write-Host ('[OK] webhook 接收器已监听 ' + $prefix + 'alerts') -ForegroundColor Green

# 2) 推一条测试告警
$alert = @{
    labels      = @{ alertname = $AlertName; severity = 'critical'; instance = 'local-demo'; job = 'campus-shop' }
    annotations = @{ summary = '这是一条测试告警'; description = '用于验证 Alertmanager 路由与 webhook 投递' }
    generatorURL = 'http://localhost:9090/graph'
} | ConvertTo-Json -Depth 5
try {
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes('[' + $alert + ']')
    $r = Invoke-WebRequest -Method Post -Uri ($Alertmanager + '/api/v2/alerts') -ContentType 'application/json; charset=utf-8' -Body $bodyBytes -UseBasicParsing -TimeoutSec 10
    Write-Host ('[OK] 已推送测试告警，Alertmanager 返回 ' + [int]$r.StatusCode) -ForegroundColor Green
} catch {
    Write-Host ('[x] 推送失败：' + $_.Exception.Message) -ForegroundColor Red
    $listener.Stop()
    Write-Host '    Alertmanager 没起来？先执行：docker compose -f docker-compose.monitoring.yml up -d' -ForegroundColor Yellow
    exit 1
}

# 3) 等投递（group_wait 默认 10 秒）
Write-Host ('[..] 等待投递（最多 ' + $WaitSeconds + ' 秒）')
$deadline = (Get-Date).AddSeconds($WaitSeconds)
$got = $false
# 只保留一个挂起的 GetContextAsync：每次循环都新建会让请求落到被丢弃的 task 上，
# 表现为 Alertmanager 侧 "context deadline exceeded"（踩过）
$pending = $listener.GetContextAsync()
while ((Get-Date) -lt $deadline -and -not $got) {
    if ($pending.Wait(500)) {
        $ctx = $pending.Result
        $pending = $listener.GetContextAsync()
        $reader = New-Object System.IO.StreamReader($ctx.Request.InputStream)
        $body = $reader.ReadToEnd(); $reader.Close()
        $ctx.Response.StatusCode = 200
        $ctx.Response.ContentLength64 = 0
        $ctx.Response.Close()
        Add-Content -Path $received -Value $body -Encoding UTF8
        $got = $true
        Write-Host '[OK] 收到告警投递！' -ForegroundColor Green
        $j = $body | ConvertFrom-Json
        foreach ($a in $j.alerts) {
            Write-Host ('    ' + $a.labels.alertname + '  [' + $a.labels.severity + ']  ' + $a.annotations.summary)
        }
        Write-Host ('    完整载荷已写入 ' + $received)
    }
}
$listener.Stop()
if (-not $got) {
    Write-Host '[x] 超时未收到投递：检查 alertmanager.yml 的 receiver url 是否指向本机端口' -ForegroundColor Red
    exit 1
}
exit 0