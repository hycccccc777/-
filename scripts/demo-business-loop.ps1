# 业务闭环演示：真实调用接口，跑一遍 浏览 -> 加购 -> 下单(幂等) -> 支付 -> 发货 -> 收货
# 用法：powershell -ExecutionPolicy Bypass -File scripts\demo-business-loop.ps1 [-BaseUrl http://localhost:8080]
param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
$script:n = 0
function Step($t) { $script:n++; Write-Host ('[' + $script:n + '] ' + $t) -ForegroundColor Cyan }
function Ok($t)   { Write-Host ('    OK  ' + $t) -ForegroundColor Green }
function Info($t) { Write-Host ('        ' + $t) -ForegroundColor Gray }

function Api($method, $path, $body, $token) {
    $h = @{ 'Content-Type' = 'application/json' }
    if ($token) { $h['Authorization'] = 'Bearer ' + $token }
    $args = @{ Method = $method; Uri = ($BaseUrl + $path); Headers = $h; TimeoutSec = 20 }
    if ($null -ne $body) { $args['Body'] = ($body | ConvertTo-Json -Depth 6) }
    return Invoke-RestMethod @args
}
# 登录走共用助手：缓存令牌 + 429 等待重试
# 原因：登录限流是 10 次/分钟/IP，反复演示两轮就会自己把自己限流，
# 脚本报 429 看起来像系统坏了，其实是演示脚本的问题。
. "$PSScriptRoot\_demo-auth.ps1"
function Login($phone, $pwd) {
    return (Get-DemoToken -BaseUrl $BaseUrl -Phone $phone -Password $pwd)
}

Step '买家登录（13800001001）'
$buyer = Login '13800001001' 'admin123'
$bt = $buyer.token
Ok ('token 前 16 位 = ' + $bt.Substring(0, 16) + '...   昵称 = ' + $buyer.user.nickname)

Step '浏览商品列表（分页 20）'
$page = (Api 'Get' '/api/v1/products?page=1&pageSize=20' $null $bt).data
Ok ('返回 ' + $page.list.Count + ' 条，共 ' + $page.total + ' 件在售')

Step '挑一件非本人出售、有库存的商品'
$target = $page.list | Where-Object { [string]$_.sellerId -ne [string]$buyer.user.id -and $_.stock -gt 0 } | Select-Object -First 1
if (-not $target) { Write-Host '    !! 没有可下单的商品（可能都被买光了）' -ForegroundColor Red; exit 1 }
Ok ('商品 #' + $target.productId + '《' + $target.title + '》 单价 ' + $target.price + ' 库存 ' + $target.stock)
Info ('卖家 userId = ' + $target.sellerId + '（' + $target.sellerName + '）')

Step '加入购物车'
$null = Api 'Post' '/api/v1/cart/items' @{ productId = [int]$target.productId; quantity = 1 } $bt
$cart = @((Api 'Get' '/api/v1/cart' $null $bt).data)
$ci = $cart | Where-Object { $_.productId -eq $target.productId } | Select-Object -First 1
Ok ('购物车共 ' + $cart.Count + ' 项，本次 cartItemId = ' + $ci.cartItemId)

Step '提交订单（带幂等键）'
$idem = 'demo-' + [guid]::NewGuid().ToString('N').Substring(0, 10)
$ob = @{ cartItemIds = @([int]$ci.cartItemId); receiver = '张小林'; phone = '13800001001'; addressDetail = '演示大学 1 号宿舍楼 302'; idempotencyKey = $idem }
$o1 = (Api 'Post' '/api/v1/orders' $ob $bt).data
$orderNo = $o1.orders[0].orderNo
Ok ('生成订单 ' + $orderNo + '  应付 ' + $o1.orders[0].payAmount + '  状态 ' + $o1.orders[0].statusText)

Step '同一幂等键再提交一次（模拟用户连点两下）'
$o2 = (Api 'Post' '/api/v1/orders' $ob $bt).data
if ($o2.groupNo -eq $o1.groupNo) { Ok ('返回同一分组 ' + $o2.groupNo + ' —— 幂等生效，没有重复下单') }
else { Write-Host ('    !! 生成了新分组 ' + $o2.groupNo) -ForegroundColor Red }

Step '发起支付（沙箱网关）'
$pay = (Api 'Post' '/api/v1/pay/create' @{ orderNo = $orderNo; payMethod = 'sandbox' } $bt).data
Ok ('支付单 ' + $pay.payNo + '  金额 ' + $pay.amount + '  网关 ' + $pay.gateway)
Info ('预下单数据: ' + $pay.prepayData)

Step '确认支付（沙箱人工确认）'
$null = Api 'Post' ('/api/v1/pay/' + $pay.payNo + '/confirm') @{} $bt
$d1 = (Api 'Get' ('/api/v1/orders/' + $orderNo) $null $bt).data
Ok ('订单状态 -> ' + $d1.status + ' ' + $d1.statusText)

Step '卖家登录并发货'
$seller = $null
foreach ($ph in @('13800001003', '13800001004', '13800001002')) {
    try {
        $cand = Login $ph 'admin123'
        if ([string]$cand.user.id -eq [string]$target.sellerId) { $seller = $cand; break }
    } catch { }
}
if (-not $seller) { Write-Host '    !! 找不到卖家账号' -ForegroundColor Red; exit 1 }
Info ('卖家 ' + $seller.user.nickname + ' 登录成功')
$null = Api 'Put' ('/api/v1/orders/' + $orderNo + '/ship') $null $seller.token
$d2 = (Api 'Get' ('/api/v1/orders/' + $orderNo) $null $bt).data
Ok ('订单状态 -> ' + $d2.status + ' ' + $d2.statusText)

Step '买家确认收货'
$null = Api 'Put' ('/api/v1/orders/' + $orderNo + '/confirm') $null $bt
$d3 = (Api 'Get' ('/api/v1/orders/' + $orderNo) $null $bt).data
Ok ('订单状态 -> ' + $d3.status + ' ' + $d3.statusText)

Step '回看商品库存（应扣减 1）'
$after = (Api 'Get' ('/api/v1/products/' + $target.productId) $null $bt).data
Ok ('库存 ' + $target.stock + ' -> ' + $after.stock)

Write-Host ''
Write-Host ('=== 闭环完成：' + $orderNo + ' 走完 待付款 -> 待发货 -> 待收货 -> 已完成 ===') -ForegroundColor Yellow