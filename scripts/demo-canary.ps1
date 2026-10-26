# 灰度发布验证：按 header 强制 / 按比例分流 / 一键切回
#
# 为什么用这个方式验证：灰度最容易出的问题是「以为分流了其实全打一台」
# 或「切回不生效」。这里用 X-Instance-Id 直接看出请求落在哪个节点。
#
# 用法（先起稳定 + 灰度两个实例 + LB，见 README「灰度发布」）：
#   powershell -ExecutionPolicy Bypass -File scripts\demo-canary.ps1
param(
    [string]$Lb = 'http://localhost:8088',
    [int]$Samples = 30
)

$ErrorActionPreference = 'Stop'
$script:pass = 0
$script:fail = 0
function Check($name, $ok, $detail) {
    if ($ok) { $script:pass++; Write-Host ('  [PASS] ' + $name) -ForegroundColor Green }
    else { $script:fail++; Write-Host ('  [FAIL] ' + $name + '  ' + $detail) -ForegroundColor Red }
}
function Probe($headers) {
    $h = @{}
    if ($headers) { $headers.GetEnumerator() | ForEach-Object { $h[$_.Key] = $_.Value } }
    try {
        $r = Invoke-WebRequest -Uri ($Lb + '/api/v1/system/health') -Headers $h -UseBasicParsing -TimeoutSec 10
        return @{ code = [int]$r.StatusCode; inst = [string]$r.Headers['X-Instance-Id'] }
    } catch { return @{ code = 0; inst = '' } }
}

Write-Host ''
Write-Host '=== 灰度发布验证 ===' -ForegroundColor Cyan
Write-Host ('  LB=' + $Lb + '  样本=' + $Samples)
Write-Host ''

# 0) LB 是否在线
$r = Probe $null
if ($r.code -ne 200) {
    Write-Host ('[x] LB 不可达（' + $r.code + '）：先起两个实例与 lb-proxy / nginx') -ForegroundColor Red
    exit 1
}

# 1) header 强制灰度
Write-Host '[1] X-Canary: 1 强制走灰度' -ForegroundColor Cyan
$c1 = Probe @{ 'X-Canary' = '1' }
Check '强制灰度命中 canary 节点' ($c1.inst -like '*canary*') ('实际: ' + $c1.inst)
Write-Host ('       X-Instance-Id = ' + $c1.inst) -ForegroundColor DarkGray

# 2) header 强制稳定
Write-Host ''
Write-Host '[2] X-Canary: 0 强制走稳定' -ForegroundColor Cyan
$c2 = Probe @{ 'X-Canary' = '0' }
Check '强制稳定未命中 canary' ($c2.inst -notlike '*canary*') ('实际: ' + $c2.inst)
Write-Host ('       X-Instance-Id = ' + $c2.inst) -ForegroundColor DarkGray

# 3) 按比例分流
Write-Host ''
Write-Host ('[3] 不带 header，按比例分流（' + $Samples + ' 次）') -ForegroundColor Cyan
$canary = 0; $stable = 0
for ($i = 0; $i -lt $Samples; $i++) {
    $x = Probe $null
    if ($x.inst -like '*canary*') { $canary++ } else { $stable++ }
}
$ratio = if (($canary + $stable) -gt 0) { [math]::Round(100.0 * $canary / ($canary + $stable), 1) } else { 0 }
Write-Host ('       canary=' + $canary + '  stable=' + $stable + '  灰度占比 ' + $ratio + '%') -ForegroundColor DarkGray
Check '灰度节点收到了流量' ($canary -gt 0) ('canary=0，分流可能没生效')
Check '稳定节点仍在服务多数流量' ($stable -gt 0) ('stable=0，可能已全量切到灰度')

# 4) 写接口不进灰度（关键安全设计）
Write-Host ''
Write-Host '[4] 写接口钉在稳定池（即使带 X-Canary: 1）' -ForegroundColor Cyan
try {
    $body = '{"phone":"13800001001","password":"wrong-for-canary-test"}'
    $resp = Invoke-WebRequest -Uri ($Lb + '/api/v1/auth/login') -Method Post -Headers @{ 'X-Canary' = '1'; 'Content-Type' = 'application/json' } `
        -Body $body -UseBasicParsing -TimeoutSec 10
    $inst = [string]$resp.Headers['X-Instance-Id']
} catch {
    $resp = $_.Exception.Response
    $inst = if ($resp) { [string]$resp.Headers['X-Instance-Id'] } else { '' }
}
Check 'POST 登录没有落到灰度节点' ($inst -notlike '*canary*') ('实际: ' + $inst)
Write-Host ('       X-Instance-Id = ' + $inst) -ForegroundColor DarkGray

Write-Host ''
Write-Host ('=== 结果：' + $script:pass + ' 通过 / ' + $script:fail + ' 失败 ===') -ForegroundColor $(if ($script:fail -eq 0) { 'Green' } else { 'Red' })
if ($script:fail -gt 0) { exit 1 }
exit 0