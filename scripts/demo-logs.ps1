# 日志集中化验证：两个实例的日志进同一个 Loki，按 traceId 一次查出全链路
#
# 为什么需要验证：日志分散在各节点磁盘上时，"用户报错但不知道是哪台机器处理的"
# 只能一台台 grep。集中化之后一条 LogQL 就能跨实例查。
#
# 前置：
#   1) Loki 已启动（docker compose -f docker-compose.monitoring.yml up -d loki，
#      或本地 loki.exe -config.file=docs/ops/loki-config.yml）
#   2) 两个后端启动时都注入了 LOKI_URL（见 README「日志集中化」）
#
# 用法：powershell -ExecutionPolicy Bypass -File scripts\demo-logs.ps1
param(
    [string]$Loki = 'http://localhost:3100',
    [string]$Lb = 'http://localhost:8088',
    [string]$Application = 'campus-shop'
)

$ErrorActionPreference = 'Stop'
$script:pass = 0
$script:fail = 0
function Check($name, $ok, $detail) {
    if ($ok) { $script:pass++; Write-Host ('  [PASS] ' + $name) -ForegroundColor Green }
    else { $script:fail++; Write-Host ('  [FAIL] ' + $name + '  ' + $detail) -ForegroundColor Red }
}

function QueryLoki($logql, $minutes = 15, $limit = 100) {
    $end = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() * 1000000
    $start = ([DateTimeOffset]::UtcNow.AddMinutes(-$minutes).ToUnixTimeMilliseconds()) * 1000000
    $url = $Loki + '/loki/api/v1/query_range?query=' + [uri]::EscapeDataString($logql) + '&start=' + $start + '&end=' + $end + '&limit=' + $limit
    return (Invoke-RestMethod -Uri $url -TimeoutSec 20)
}

Write-Host ''
Write-Host '=== 日志集中化验证 ===' -ForegroundColor Cyan
Write-Host ('  Loki=' + $Loki + '  LB=' + $Lb)
Write-Host ''

# 0) Loki 是否就绪
try { $null = Invoke-RestMethod -Uri ($Loki + '/ready') -TimeoutSec 5 } catch {
    Write-Host ('[x] Loki 不可达：' + $_.Exception.Message) -ForegroundColor Red
    Write-Host '    docker compose -f docker-compose.monitoring.yml up -d loki' -ForegroundColor Yellow
    exit 1
}

# 1) 两个实例是否都在往 Loki 送
Write-Host '[1] 两个实例都在上报' -ForegroundColor Cyan
$labels = (Invoke-RestMethod -Uri ($Loki + '/loki/api/v1/label/instance/values') -TimeoutSec 10).data
$insts = @($labels)
Write-Host ('       已入库的 instance: ' + ($insts -join ', ')) -ForegroundColor DarkGray
Check '至少两个实例上报过' ($insts.Count -ge 2) ('只有 ' + $insts.Count + ' 个: ' + ($insts -join ','))

# 2) 造一条带自定义 traceId 的请求，再按 traceId 查回来
Write-Host ''
Write-Host '[2] 按 traceId 精确检索（跨实例同一条链路）' -ForegroundColor Cyan
$trace = 'demo' + (Get-Date -Format 'HHmmssfff')
try {
    $resp = Invoke-WebRequest -Uri ($Lb + '/api/v1/products?page=1&pageSize=5') -Headers @{ 'X-Trace-Id' = $trace } -UseBasicParsing -TimeoutSec 15
    $servedBy = [string]$resp.Headers['X-Instance-Id']
} catch {
    Write-Host ('       请求失败：' + $_.Exception.Message) -ForegroundColor Red
    $servedBy = ''
}
Write-Host ('       traceId=' + $trace + '  由 ' + $servedBy + ' 处理') -ForegroundColor DarkGray
Start-Sleep -Seconds 5

$r = QueryLoki ('{application="' + $Application + '"} | json | traceId="' + $trace + '"')
$found = @()
foreach ($s in $r.data.result) {
    foreach ($v in $s.values) { $found += ($v[1] | ConvertFrom-Json) }
}
Check 'traceId 能查到对应日志' ($found.Count -ge 1) ('查到 ' + $found.Count + ' 条')
foreach ($f in $found) {
    Write-Host ('       [' + $f.instance + '][' + $f.level + '] ' + $f.msg) -ForegroundColor DarkGray
}

# 3) 失败请求也能按 traceId 定位（排障最常用的场景）
Write-Host ''
Write-Host '[3] 失败请求可按 traceId 定位' -ForegroundColor Cyan
$trace2 = 'fail' + (Get-Date -Format 'HHmmssfff')
try {
    Invoke-WebRequest -Uri ($Lb + '/api/v1/auth/login') -Method Post -Headers @{ 'X-Trace-Id' = $trace2 } `
        -ContentType 'application/json' -Body '{"phone":"13800001001","password":"wrong"}' -UseBasicParsing -TimeoutSec 15 | Out-Null
} catch { }
Start-Sleep -Seconds 5
$r2 = QueryLoki ('{application="' + $Application + '"} | json | traceId="' + $trace2 + '"')
$found2 = @()
foreach ($s in $r2.data.result) { foreach ($v in $s.values) { $found2 += ($v[1] | ConvertFrom-Json) } }
Check '失败请求的日志已入库' ($found2.Count -ge 1) ('查到 ' + $found2.Count + ' 条')
$has401 = @($found2 | Where-Object { $_.msg -match 'status=401' }).Count -gt 0
Check '日志里能看到 401' $has401 '没找到 status=401 的记录'

# 4) 按实例维度查（对比"日志分散在磁盘上"的旧方式）
Write-Host ''
Write-Host '[4] 按实例维度检索' -ForegroundColor Cyan
foreach ($i in $insts) {
    $ri = QueryLoki ('{application="' + $Application + '", instance="' + $i + '"}') -minutes 15 -limit 5
    $cnt = 0
    foreach ($s in $ri.data.result) { $cnt += @($s.values).Count }
    Check ('实例 ' + $i + ' 有日志') ($cnt -gt 0) ('0 条')
}

Write-Host ''
Write-Host ('=== 结果：' + $script:pass + ' 通过 / ' + $script:fail + ' 失败 ===') -ForegroundColor $(if ($script:fail -eq 0) { 'Green' } else { 'Red' })
if ($script:fail -gt 0) { exit 1 }
exit 0