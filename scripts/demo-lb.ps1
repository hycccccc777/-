# 多实例 + 负载均衡演示：四条断言，一次跑完
#
# 为什么要这四条：单实例能跑通不代表多实例能跑通。多实例真正会暴露的问题是
#   1) 流量根本没分开（LB 配置错，或者 least_conn 串行时总落第一个节点）
#   2) 令牌只活在某个节点的内存里 -> 换个节点就 401
#   3) 杀掉一个节点后整个站不可用
#   4) 限流是进程内的 -> 配额被放大 N 倍
#
# 用法（先起两个后端 + LB，见 README「多实例」）：
#   powershell -ExecutionPolicy Bypass -File scripts\demo-lb.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\demo-lb.ps1 -Lb http://localhost:8088 -NodeA 8080 -NodeB 8082
param(
    [string]$Lb = 'http://localhost:8088',
    [int]$NodeA = 8080,
    [int]$NodeB = 8082,
    [int]$KillNode = 0
)

$ErrorActionPreference = 'Stop'
$script:pass = 0
$script:fail = 0

function Check($name, $ok, $detail) {
    if ($ok) { $script:pass++; Write-Host ('  [PASS] ' + $name) -ForegroundColor Green }
    else { $script:fail++; Write-Host ('  [FAIL] ' + $name + '  ' + $detail) -ForegroundColor Red }
}

function Probe($url, $method = 'Get', $body = $null, $token = $null) {
    $h = @{}
    if ($token) { $h['Authorization'] = 'Bearer ' + $token }
    $a = @{ Method = $method; Uri = $url; Headers = $h; UseBasicParsing = $true; TimeoutSec = 20 }
    if ($body) { $a['ContentType'] = 'application/json'; $a['Body'] = $body }
    try {
        $r = Invoke-WebRequest @a
        return @{ code = [int]$r.StatusCode; inst = [string]$r.Headers['X-Instance-Id']; backend = [string]$r.Headers['X-LB-Backend']; body = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            return @{ code = [int]$resp.StatusCode
                      inst = [string]$resp.Headers['X-Instance-Id']
                      backend = [string]$resp.Headers['X-LB-Backend']; body = '' }
        }
        return @{ code = 0; inst = ''; backend = ''; body = $_.Exception.Message }
    }
}

Write-Host ''
Write-Host '=== 多实例 + 负载均衡验证 ===' -ForegroundColor Cyan
Write-Host ('  LB=' + $Lb + '  nodes=' + $NodeA + ',' + $NodeB)
Write-Host ''

# --- 前置：LB 与两个节点都活着
$h = Probe ($Lb + '/api/v1/system/health')
if ($h.code -ne 200) {
    Write-Host ('[x] LB 不可达（' + $h.code + '）：先启动后端与 LB，见 README「多实例」') -ForegroundColor Red
    exit 1
}

Write-Host '[1] 流量是否真的分到两个节点' -ForegroundColor Cyan
$seen = @{}
for ($i = 0; $i -lt 8; $i++) {
    $r = Probe ($Lb + '/api/v1/system/health')
    if ($r.inst) { $prev = 0; if ($seen.ContainsKey($r.inst)) { $prev = $seen[$r.inst] }; $seen[$r.inst] = $prev + 1 }
}
$dist = ($seen.Keys | Sort-Object | ForEach-Object { $_ + '=' + $seen[$_] }) -join ', '
Check '两个节点都收到请求' ($seen.Count -eq 2) ('实际分布: ' + $dist)
Write-Host ('       分布: ' + $dist) -ForegroundColor DarkGray

Write-Host ''
Write-Host '[2] 令牌跨节点可用（无状态）' -ForegroundColor Cyan
$login = Probe ($Lb + '/api/v1/auth/login') 'Post' '{"phone":"13800001001","password":"admin123"}'
if ($login.code -eq 200) {
    $token = ($login.body | ConvertFrom-Json).data.token
    $okCount = 0; $nodes = @{}
    for ($i = 0; $i -lt 6; $i++) {
        $r = Probe ($Lb + '/api/v1/orders?page=1&pageSize=1') 'Get' $null $token
        if ($r.code -eq 200) { $okCount++ }
        if ($r.inst) { $nodes[$r.inst] = 1 }
    }
    Check '同一令牌 6 次请求全部 200' ($okCount -eq 6) ('成功 ' + $okCount + '/6')
    Check '这 6 次确实落在两个不同节点上' ($nodes.Count -eq 2) ('只落到了: ' + ($nodes.Keys -join ','))
} else {
    Check '登录成功' $false ('HTTP ' + $login.code)
}

Write-Host ''
Write-Host '[3] 限流配额全局共享（10 次/60 秒/IP）' -ForegroundColor Cyan
$tmp = Join-Path $env:TEMP ('lb-demo-' + [guid]::NewGuid().ToString('N').Substring(0, 6) + '.json')
[System.IO.File]::WriteAllText($tmp, '{"refreshToken":"invalid-for-limit-demo"}', (New-Object System.Text.UTF8Encoding($false)))
$codes = @()
for ($i = 0; $i -lt 12; $i++) {
    $r = Probe ($Lb + '/api/v1/auth/refresh') 'Post' '{"refreshToken":"invalid-for-limit-demo"}'
    $codes += $r.code
}
Remove-Item $tmp -Force -ErrorAction SilentlyContinue
$allowed = @($codes | Where-Object { $_ -ne 429 }).Count
$blocked = @($codes | Where-Object { $_ -eq 429 }).Count
Check '12 次里前 10 次放行、之后限流' (($blocked -ge 1) -and ($allowed -le 10)) ('放行 ' + $allowed + ' 次，拦截 ' + $blocked + ' 次')
if ($allowed -gt 10) {
    Write-Host '       放行超过 10 次说明限流计数没有共享（进程内计数被放大 N 倍）' -ForegroundColor Yellow
}

Write-Host ''
Write-Host '[4] 单节点故障，服务不中断' -ForegroundColor Cyan
if (-not $KillNode) {
    Write-Host '       跳过（要验证容错，加 -KillNode 8082，脚本会杀掉该端口的进程）' -ForegroundColor DarkGray
} else {
    $victim = (Get-NetTCPConnection -LocalPort $KillNode -State Listen -ErrorAction SilentlyContinue).OwningProcess
    if (-not $victim) {
        Check ('杀掉 ' + $KillNode + ' 端口进程') $false '该端口没有监听进程'
    } else {
        Stop-Process -Id $victim -Force -ErrorAction SilentlyContinue
        Write-Host ('       已杀掉 ' + $KillNode + '（pid=' + $victim + '），等待 LB 摘除') -ForegroundColor DarkGray
        Start-Sleep -Seconds 2
        $okAfter = 0; $usedNodes = @{}
        for ($i = 0; $i -lt 6; $i++) {
            $r = Probe ($Lb + '/api/v1/system/health')
            if ($r.code -eq 200) { $okAfter++ }
            if ($r.inst) { $usedNodes[$r.inst] = 1 }
        }
        # 允许第一次落到刚死的节点上返回 502（等价于 nginx proxy_next_upstream 生效前的抖动）
        Check '少一个节点后仍能正常服务' ($okAfter -ge 5) ('成功 ' + $okAfter + '/6')
        Write-Host ('       仍在服务的节点: ' + ($usedNodes.Keys -join ',')) -ForegroundColor DarkGray
    }
}

Write-Host ''
Write-Host ('=== 结果：' + $script:pass + ' 通过 / ' + $script:fail + ' 失败 ===') -ForegroundColor $(if ($script:fail -eq 0) { 'Green' } else { 'Red' })
if ($script:fail -gt 0) { exit 1 }
exit 0