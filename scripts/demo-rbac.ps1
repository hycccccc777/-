# 角色权限演示：同一批接口，匿名 / 买家 / 卖家三种身份结果不同
#
# 自带一笔订单（下单+支付），用来看「买家不能替卖家发货」这条越权边界。
# 关键：卖家身份必须取「这笔订单的实际卖家」，不能写死某个账号 ——
# 否则「我卖的」那一栏会返回空数组，与标签「应有数据」自相矛盾。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\demo-rbac.ps1 [-BaseUrl http://localhost:8080]
# 前置：数据库里至少有一件「非买家 1001 出售、且有库存」的商品；
#       没有就先跑 scripts\reset-demo-data.ps1
param([string]$BaseUrl = 'http://localhost:8080', [string]$BuyerPhone = '13800001001')

$ErrorActionPreference = 'Stop'

function Call($method, $path, $body, $token) {
    $h = @{ 'Content-Type' = 'application/json' }
    if ($token) { $h['Authorization'] = 'Bearer ' + $token }
    $a = @{ Method = $method; Uri = ($BaseUrl + $path); Headers = $h; TimeoutSec = 20; UseBasicParsing = $true }
    if ($null -ne $body) { $a['Body'] = ($body | ConvertTo-Json -Depth 6) }
    try {
        $r = Invoke-WebRequest @a
        $c = $r.Content; if ($c -is [byte[]]) { $c = [System.Text.Encoding]::UTF8.GetString($c) }
        return @{ code = [int]$r.StatusCode; body = ([string]$c -replace '\s+', ' ') }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $sr = New-Object System.IO.StreamReader($resp.GetResponseStream()); $txt = $sr.ReadToEnd(); $sr.Close()
            return @{ code = [int]$resp.StatusCode; body = ($txt -replace '\s+', ' ') }
        }
        return @{ code = 0; body = $_.Exception.Message }
    }
}
function Short($s, $n = 88) { $str = [string]$s; if ($str.Length -gt $n) { return $str.Substring(0, $n) + '...' } else { return $str } }
function Show($label, $r, $expect) {
    $ok = ($r.code -eq $expect)
    $color = if ($ok) { 'Green' } else { 'Red' }
    $script:fail = if (-not $ok) { $script:fail + 1 } else { $script:fail }
    Write-Host ('  ' + $label.PadRight(40) + ' -> ' + ([string]$r.code).PadRight(4)) -ForegroundColor $color -NoNewline
    Write-Host ('  ' + (Short $r.body)) -ForegroundColor DarkGray
}
# 登录走共用助手：缓存令牌 + 429 等待重试
# 原因：登录限流是 10 次/分钟/IP，反复演示两轮就会自己把自己限流，
# 脚本报 429 看起来像系统坏了，其实是演示脚本的问题。
. "$PSScriptRoot\_demo-auth.ps1"
function Login($phone) {
    return (Get-DemoToken -BaseUrl $BaseUrl -Phone $phone)
}

$script:fail = 0

Write-Host ''
Write-Host '=== 角色权限演示：同一接口，三种身份 ===' -ForegroundColor Cyan
Write-Host ''

# ---------- [A] 匿名 ----------
Write-Host '[A] 未登录访问受保护接口（HTTP 401）' -ForegroundColor Cyan
Show 'GET  /api/v1/orders（匿名）' (Call 'Get' '/api/v1/orders' $null $null) 401
Show 'POST /api/v1/orders（匿名）' (Call 'Post' '/api/v1/orders' @{} $null) 401
Show 'GET  /api/v1/orders/sold（匿名）' (Call 'Get' '/api/v1/orders/sold' $null $null) 401
Write-Host ''

$buyer = Login $BuyerPhone
$buyerId = [string]$buyer.user.id

# ---------- 先造一笔「待发货」订单，并确定它的真实卖家 ----------
$page = (Invoke-RestMethod -Uri ($BaseUrl + '/api/v1/products?page=1&pageSize=20') -Headers @{ Authorization = 'Bearer ' + $buyer.token }).data
$target = $page.list | Where-Object { [string]$_.sellerId -ne $buyerId -and [int]$_.stock -gt 0 } | Select-Object -First 1
if (-not $target) {
    Write-Host '  [x] 没有可下单的商品：请先跑 scripts\reset-demo-data.ps1 复位演示数据' -ForegroundColor Red
    exit 1
}

# 演示账号规律：phone = '1380000' + user_id（如 1002 -> 13800001002）
$sellerPhone = '1380000' + ([string]$target.sellerId)
$seller = Login $sellerPhone
Write-Host ('  买家 = ' + $buyer.user.nickname + '（id ' + $buyerId + '）   卖家 = ' + $seller.user.nickname + '（id ' + $seller.user.id + '，商品 #' + $target.productId + ' 的卖家）') -ForegroundColor Yellow
Write-Host ''

$null = Call 'Post' '/api/v1/cart/items' @{ productId = [int]$target.productId; quantity = 1 } $buyer.token
$cart = @((Invoke-RestMethod -Uri ($BaseUrl + '/api/v1/cart') -Headers @{ Authorization = 'Bearer ' + $buyer.token }).data)
$ci = $cart | Where-Object { $_.productId -eq $target.productId } | Select-Object -First 1
$ob = @{
    cartItemIds    = @([int]$ci.cartItemId)
    receiver       = '张小林'
    phone          = $BuyerPhone
    addressDetail  = '演示大学 1 号宿舍楼 302'
    idempotencyKey = ('rbac-' + [guid]::NewGuid().ToString('N').Substring(0, 10))
}
$od = (Invoke-RestMethod -Method Post -Uri ($BaseUrl + '/api/v1/orders') -Headers @{ 'Content-Type' = 'application/json'; Authorization = 'Bearer ' + $buyer.token } -Body ($ob | ConvertTo-Json -Depth 5)).data
$orderNo = $od.orders[0].orderNo
$pay = (Invoke-RestMethod -Method Post -Uri ($BaseUrl + '/api/v1/pay/create') -Headers @{ 'Content-Type' = 'application/json'; Authorization = 'Bearer ' + $buyer.token } -Body (@{ orderNo = $orderNo; payMethod = 'sandbox' } | ConvertTo-Json)).data
$null = Call 'Post' ('/api/v1/pay/' + $pay.payNo + '/confirm') @{} $buyer.token
Write-Host ('  演示订单 ' + $orderNo + '（已支付，待发货）') -ForegroundColor DarkGray
Write-Host ''

# ---------- [B] 数据隔离 ----------
Write-Host '[B] 数据隔离：买家看到「我买的」，卖家看到「我卖的」' -ForegroundColor Cyan
Show 'GET /api/v1/orders（买家）' (Call 'Get' '/api/v1/orders?page=1&pageSize=5' $null $buyer.token) 200
Show 'GET /api/v1/orders/sold（买家，应为空）' (Call 'Get' '/api/v1/orders/sold' $null $buyer.token) 200
Show 'GET /api/v1/orders/sold（卖家，应有数据）' (Call 'Get' '/api/v1/orders/sold' $null $seller.token) 200
Write-Host ''

# ---------- [C] 越权边界 ----------
Write-Host '[C] 越权边界：买家不能替卖家发货' -ForegroundColor Cyan
Show 'PUT /orders/{no}/ship（买家身份，越权）' (Call 'Put' ('/api/v1/orders/' + $orderNo + '/ship') $null $buyer.token) 404
Show 'PUT /orders/{no}/ship（卖家身份，合法）' (Call 'Put' ('/api/v1/orders/' + $orderNo + '/ship') $null $seller.token) 200
Write-Host ''

# ---------- [D] 探针 ----------
Write-Host '[D] 探针端点保持匿名可访问（负载均衡 / K8s / Prometheus 用）' -ForegroundColor Cyan
Show 'GET /actuator/health（匿名）' (Call 'Get' '/actuator/health' $null $null) 200
Show 'GET /actuator/prometheus（匿名，仅暴露指标）' (Call 'Get' '/actuator/prometheus' $null $null) 200
Write-Host ''

if ($script:fail -eq 0) {
    Write-Host '=== 结论：认证 401、越权 404（不泄露资源存在性）、数据按角色隔离 ===' -ForegroundColor Yellow
    exit 0
} else {
    Write-Host ('=== 有 ' + $script:fail + ' 项未达预期，见上方红字 ===') -ForegroundColor Red
    exit 1
}
