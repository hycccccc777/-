# =====================================================================
# 待付款订单超时自动取消 · 端到端演示
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\demo-order-timeout.ps1
#
# 做的事：
#   1) 以演示买家登录，挑一件有库存的商品下单（不付款）；
#   2) 把订单 create_time 回拨到 2 分钟前，制造"超时"；
#   3) 等定时任务下一轮扫描（cron 每分钟），断言订单变为已取消、库存已回滚。
#
# 前置条件：
#   * 后端已启动，且 campus.order.timeout-minutes 设为 1（演示用短阈值）：
#       set CAMPUS_ORDER_TIMEOUT_MINUTES=1 && 启动后端.cmd
#   * MySQL 口令与 reset-demo-data.ps1 一致（默认 qazwsx@134567，可用 -DbPassword 覆盖）
#
# 说明：脚本直接改数据库时间是为了不用真等 30 分钟，不是绕过业务逻辑——
#       定时任务仍然走完整的 SQL 条件更新与库存回滚。
# =====================================================================
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$DbPassword = 'qazwsx@134567',
    [string]$MysqlExe = 'C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe'
)
$ErrorActionPreference = 'Stop'
$env:MYSQL_PWD = $DbPassword
function Q($sql) { & $MysqlExe -uroot -N -e $sql 2>$null }
function J($obj) { $obj | ConvertTo-Json -Depth 8 -Compress }

Write-Host "== 待付款订单超时自动取消 ==" -ForegroundColor Cyan

$login = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body (J @{ phone = '13800001001'; password = 'admin123' })
$tok = $login.data.token
if (-not $tok) { throw '登录失败，请先运行 reset-demo-data.ps1' }
$hd = @{ Authorization = 'Bearer ' + $tok }

$plist = Invoke-RestMethod -Uri "$BaseUrl/api/v1/products?page=1&pageSize=20" -Headers $hd
$p = $plist.data.list | Where-Object { $_.stock -gt 0 -and $_.sellerId -ne '1001' } | Select-Object -First 1
if (-not $p) { throw '没有可下单的商品，请先运行 reset-demo-data.ps1' }
Write-Host ("  商品: #{0} {1}（库存 {2}）" -f $p.productId, $p.title, $p.stock)

$null = Invoke-RestMethod -Uri "$BaseUrl/api/v1/cart/items" -Method Post -Headers $hd -ContentType 'application/json' -Body (J @{ productId = [long]$p.productId; quantity = 1 })
$cart = Invoke-RestMethod -Uri "$BaseUrl/api/v1/cart" -Headers $hd
$cartItemId = $cart.data[0].cartItemId

$orderBody = J @{ cartItemIds = @([long]$cartItemId); receiver = '超时演示'; phone = '13800000000'; addressDetail = '演示地址' }
$order = Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders" -Method Post -Headers $hd -ContentType 'application/json' -Body $orderBody
$orderNo = $order.data.orders[0].orderNo
Write-Host ("  订单号: {0}" -f $orderNo)

$stockAfterOrder = (Q "select stock from shop.ct_product where product_id=$($p.productId);") -join ''
Write-Host ("  下单后: 状态=待付款, 库存={0}（已被占用）" -f $stockAfterOrder)

Q "update shop.ct_order set create_time=date_sub(now(), interval 2 minute) where order_no='$orderNo';" | Out-Null
Write-Host '  把 create_time 回拨 2 分钟，等待定时任务下一轮扫描（最多 90 秒）...'

$cancelled = $false
for ($i = 0; $i -lt 18; $i++) {
    Start-Sleep -Seconds 5
    $st = (Q "select status from shop.ct_order where order_no='$orderNo';" | Select-Object -First 1)
    if ($st -eq '4') { $cancelled = $true; Write-Host ("  {0} 秒后订单被自动取消" -f (($i + 1) * 5)) -ForegroundColor Green; break }
}

$final = Q "select status, ifnull(cancel_reason,'') from shop.ct_order where order_no='$orderNo';"
$stockFinal = (Q "select stock from shop.ct_product where product_id=$($p.productId);") -join ''
Write-Host ("  最终: {0}, 库存={1}" -f (($final -join ' ')), $stockFinal)

Remove-Item Env:\MYSQL_PWD
if ($cancelled -and $stockFinal -eq [string]$p.stock) {
    Write-Host '  结果: PASS（订单已取消且库存已回滚）' -ForegroundColor Green
    exit 0
}
Write-Host '  结果: FAIL（检查 CLI 阈值是否设为 1 分钟、任务是否在运行）' -ForegroundColor Red
exit 1
