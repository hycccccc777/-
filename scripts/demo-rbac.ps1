# 角色权限演示：同一批接口，匿名 / 买家 / 卖家三种身份结果不同
# 自带一笔订单（下单+支付），用来看「买家不能替卖家发货」这条越权边界
# 用法：powershell -ExecutionPolicy Bypass -File scripts\demo-rbac.ps1 [-BaseUrl http://localhost:8080]
param([string]$BaseUrl = 'http://localhost:8080')

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
function Short($s, $n = 88) { $str = [string]$s; if ($str.Length -gt $n) { return $str.Substring(0, [Math]::Min($n, $str.Length)) + '...' } else { return $str } }
function Show($label, $r, $expect) {
    $color = if ($r.code -eq $expect) { 'Green' } else { 'Red' }
    Write-Host ('  ' + $label.PadRight(40) + ' -> ' + ([string]$r.code).PadRight(4)) -ForegroundColor $color -NoNewline
    Write-Host ('  ' + (Short $r.body)) -ForegroundColor DarkGray
}
function Login($phone) {
    $r = Invoke-RestMethod -Method Post -Uri ($BaseUrl + '/api/v1/auth/login') -Headers @{ 'Content-Type' = 'application/json' } -Body (@{ phone = $phone; password = 'admin123' } | ConvertTo-Json)
    return $r.data
}
function J($method, $path, $body, $token) { return (Call $method $path $body $token) }

Write-Host ''
Write-Host '=== 角色权限演示：同一接口，三种身份 ===' -ForegroundColor Cyan
Write-Host ''

Write-Host '[A] 未登录访问受保护接口（HTTP 401）' -ForegroundColor Cyan
Show 'GET  /api/v1/orders（匿名）' (Call 'Get' '/api/v1/orders' $null $null) 401
Show 'POST /api/v1/orders（匿名）' (Call 'Post' '/api/v1/orders' @{} $null) 401
Show 'GET  /api/v1/orders/sold（匿名）' (Call 'Get' '/api/v1/orders/sold' $null $null) 401
Write-Host ''

$buyer = Login '13800001001'
$seller = Login '13800001003'
Write-Host ('  买家 = ' + $buyer.user.nickname + '（id ' + $buyer.user.id + '）   卖家 = ' + $seller.user.nickname + '（id ' + $seller.user.id + '）') -ForegroundColor Yellow
Write-Host ''

Write-Host '[B] 数据隔离：买家看到「我买的」，卖家看到「我卖的」' -ForegroundColor Cyan
Show 'GET /api/v1/orders（买家）' (Call 'Get' '/api/v1/orders?page=1&pageSize=5' $null $buyer.token) 200
Show 'GET /api/v1/orders/sold（买家，应为空）' (Call 'Get' '/api/v1/orders/sold' $null $buyer.token) 200
Show 'GET /api/v1/orders/sold（卖家，应有数据）' (Call 'Get' '/api/v1/orders/sold' $null $seller.token) 200
Write-Host ''

Write-Host '[C] 越权边界：买家不能替卖家发货' -ForegroundColor Cyan
# 先造一笔「待发货」订单：加购 -> 下单 -> 支付
$page = (Invoke-RestMethod -Uri ($BaseUrl + '/api/v1/products?page=1&pageSize=20') -Headers @{ Authorization = 'Bearer ' + $buyer.token }).data
$target = $page.list | Where-Object { [string]$_.sellerId -ne [string]$buyer.user.id -and $_.stock -gt 0 } | Select-Object -First 1
if ($target) {
    foreach ($ph in @('13800001002','13800001003','13800001004')) {
        try {
            $cand = Login $ph
            if ([string]$cand.user.id -eq [string]$target.sellerId) { $seller = $cand; break }
        } catch { }
    }
}
if ($target) {
    $null = Call 'Post' '/api/v1/cart/items' @{ productId = [int]$target.productId; quantity = 1 } $buyer.token
    $cart = @((Invoke-RestMethod -Uri ($BaseUrl + '/api/v1/cart') -Headers @{ Authorization = 'Bearer ' + $buyer.token }).data)
    $ci = $cart | Where-Object { $_.productId -eq $target.productId } | Select-Object -First 1
    $ob = @{ cartItemIds = @([int]$ci.cartItemId); receiver = '张小林'; phone = '13800001001'; addressDetail = '演示大学 1 号宿舍楼 302'; idempotencyKey = ('rbac-' + [guid]::NewGuid().ToString('N').Substring(0, 10)) }
    $od = (Invoke-RestMethod -Method Post -Uri ($BaseUrl + '/api/v1/orders') -Headers @{ 'Content-Type' = 'application/json'; Authorization = 'Bearer ' + $buyer.token } -Body ($ob | ConvertTo-Json -Depth 5)).data
    $orderNo = $od.orders[0].orderNo
    $pay = (Invoke-RestMethod -Method Post -Uri ($BaseUrl + '/api/v1/pay/create') -Headers @{ 'Content-Type' = 'application/json'; Authorization = 'Bearer ' + $buyer.token } -Body (@{ orderNo = $orderNo; payMethod = 'sandbox' } | ConvertTo-Json)).data
    $null = Call 'Post' ('/api/v1/pay/' + $pay.payNo + '/confirm') @{} $buyer.token
    Write-Host ('  演示订单 ' + $orderNo + '（已支付，待发货）') -ForegroundColor DarkGray
    Show 'PUT /orders/{no}/ship（买家身份，越权）' (Call 'Put' ('/api/v1/orders/' + $orderNo + '/ship') $null $buyer.token) 404
    Show 'PUT /orders/{no}/ship（卖家身份，合法）' (Call 'Put' ('/api/v1/orders/' + $orderNo + '/ship') $null $seller.token) 200
} else {
    Write-Host '  （没有卖家在售商品，跳过）' -ForegroundColor Yellow
}
Write-Host ''

Write-Host '[D] 探针端点保持匿名可访问（负载均衡 / K8s / Prometheus 用）' -ForegroundColor Cyan
Show 'GET /actuator/health（匿名）' (Call 'Get' '/actuator/health' $null $null) 200
Show 'GET /actuator/prometheus（匿名，仅暴露指标）' (Call 'Get' '/actuator/prometheus' $null $null) 200
Write-Host ''
Write-Host '=== 结论：认证 401、越权 404（不泄露资源存在性）、数据按角色隔离 ===' -ForegroundColor Yellow