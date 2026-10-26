# 接口层测试：不是"点一遍"，而是把每类风险各打一枪，记录实际状态码与响应
# 输出格式固定为 [PASS]/[FAIL] 行，便于汇总进测试报告
param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
$script:pass = 0
$script:fail = 0

function Req($method, $path, $body, $token, $extraHeaders) {
    $h = @{ 'Content-Type' = 'application/json' }
    if ($token) { $h['Authorization'] = 'Bearer ' + $token }
    if ($extraHeaders) { foreach ($k in $extraHeaders.Keys) { $h[$k] = $extraHeaders[$k] } }
    $a = @{ Method = $method; Uri = ($BaseUrl + $path); Headers = $h; TimeoutSec = 20; UseBasicParsing = $true }
    if ($null -ne $body) { $a['Body'] = ($body | ConvertTo-Json -Depth 6) }
    try {
        $r = Invoke-WebRequest @a
        $c = $r.Content; if ($c -is [byte[]]) { $c = [System.Text.Encoding]::UTF8.GetString($c) }
        return @{ code = [int]$r.StatusCode; body = [string]$c; headers = $r.Headers }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $sr = New-Object System.IO.StreamReader($resp.GetResponseStream()); $txt = $sr.ReadToEnd(); $sr.Close()
            return @{ code = [int]$resp.StatusCode; body = $txt; headers = $resp.Headers }
        }
        return @{ code = 0; body = $_.Exception.Message; headers = @{} }
    }
}
function Check($name, $result, $expect, $note) {
    $ok = ($result.code -eq $expect)
    $tag = if ($ok) { 'PASS' } else { 'FAIL' }
    if ($ok) { $script:pass++ } else { $script:fail++ }
    $snippet = ($result.body -replace '\s+', ' ')
    if ($snippet.Length -gt 70) { $snippet = $snippet.Substring(0, 70) + '...' }
    Write-Output ("[$tag] " + $name.PadRight(38) + " HTTP " + ([string]$result.code).PadRight(4) + " expect " + $expect + "  " + $snippet)
}
function Token($phone) {
    $r = Req 'Post' '/api/v1/auth/login' @{ phone = $phone; password = 'admin123' } $null
    if ($r.code -ne 200) { throw ("登录失败 " + $phone + " HTTP " + $r.code) }
    return ($r.body | ConvertFrom-Json).data
}

Write-Output '=== A. 健康与探针 ==='
Check '健康检查 GET /api/v1/system/health' (Req 'Get' '/api/v1/system/health' $null $null) 200
Check 'Actuator 健康 GET /actuator/health' (Req 'Get' '/actuator/health' $null $null) 200
Check 'Prometheus 指标 GET /actuator/prometheus' (Req 'Get' '/actuator/prometheus' $null $null) 200
Check 'Druid 控制台默认关闭 GET /druid/index.html' (Req 'Get' '/druid/index.html' $null $null) 404

Write-Output ''
Write-Output '=== B. 认证与令牌 ==='
$bad = Req 'Post' '/api/v1/auth/login' @{ phone = '13800001001'; password = 'wrong-password' } $null
Check '错误密码登录被拒' $bad 401
$noUser = Req 'Post' '/api/v1/auth/login' @{ phone = '19900000000'; password = 'admin123' } $null
Check '不存在账号登录被拒' $noUser 401
$empty = Req 'Post' '/api/v1/auth/login' @{ phone = ''; password = '' } $null
Check '空参数登录被拒（参数校验）' $empty 400

$buyer = Token '13800001001'
Write-Output ("       买家 token 前 16 位 = " + $buyer.token.Substring(0,16) + "...")
Check '无令牌访问订单 GET /orders' (Req 'Get' '/api/v1/orders' $null $null) 401
Check '伪造令牌访问 GET /orders' (Req 'Get' '/api/v1/orders' $null 'eyJhbGciOiJIUzUxMiJ9.forged.signature') 401
$refresh = Req 'Post' '/api/v1/auth/refresh' @{ refreshToken = ($buyer.token + 'x') } $null
Check '非法刷新令牌被拒（401 语义）' $refresh 401
Check '当前用户 GET /user/info' (Req 'Get' '/api/v1/user/info' $null $buyer.token) 200

Write-Output ''
Write-Output '=== C. 商品与分页边界 ==='
Check '商品列表默认分页' (Req 'Get' '/api/v1/products' $null $buyer.token) 200
Check '商品列表 pageSize=1' (Req 'Get' '/api/v1/products?page=1&pageSize=1' $null $buyer.token) 200
Check '商品列表超大 pageSize（应被钳制）' (Req 'Get' '/api/v1/products?page=1&pageSize=99999' $null $buyer.token) 200
Check '商品列表非法 page=0' (Req 'Get' '/api/v1/products?page=0' $null $buyer.token) 200
Check '商品详情 不存在 id=999999' (Req 'Get' '/api/v1/products/999999' $null $buyer.token) 404
Check '商品搜索关键字' (Req 'Get' '/api/v1/products?keyword=耳机' $null $buyer.token) 200
Check '分类列表' (Req 'Get' '/api/v1/categories' $null $buyer.token) 200

Write-Output ''
Write-Output '=== D. 数据隔离与越权 ==='
Check '买家我的订单 GET /orders' (Req 'Get' '/api/v1/orders' $null $buyer.token) 200
Check '买家「我卖的」为空 GET /orders/sold' (Req 'Get' '/api/v1/orders/sold' $null $buyer.token) 200
Check '不存在的订单详情' (Req 'Get' '/api/v1/orders/CT000000000000000000' $null $buyer.token) 404
Check '买家调用发货（越权）' (Req 'Put' '/api/v1/orders/CT000000000000000000/ship' $null $buyer.token) 404

Write-Output ''
Write-Output '=== E. 404 / 405 / 参数校验 ==='
Check '未知接口 GET /api/v1/not-exists' (Req 'Get' '/api/v1/not-exists' $null $buyer.token) 404
Check '已知接口用错方法 DELETE /api/v1/products' (Req 'Delete' '/api/v1/products' $null $buyer.token) 405
Check '下单缺必填字段' (Req 'Post' '/api/v1/orders' @{ cartItemIds = @() } $buyer.token) 400
Check '购物车加购非法数量' (Req 'Post' '/api/v1/cart/items' @{ productId = 1; quantity = 0 } $buyer.token) 400
Check '购物车加购不存在商品（409 业务冲突）' (Req 'Post' '/api/v1/cart/items' @{ productId = 999999; quantity = 1 } $buyer.token) 409

Write-Output ''
Write-Output '=== F. 安全响应头与 CORS ==='
$h = Req 'Get' '/api/v1/system/health' $null $null
$hdr = $h.headers
foreach ($k in @('X-Content-Type-Options','X-Frame-Options','X-XSS-Protection')) {
  $v = $null
  try { $v = $hdr[$k] } catch { }
  if ($v) { Write-Output ("[PASS] 响应头 " + $k.PadRight(30) + " = " + $v); $script:pass++ }
  else { Write-Output ("[FAIL] 响应头 " + $k.PadRight(30) + " 缺失"); $script:fail++ }
}
$cors = Req 'Get' '/api/v1/system/health' $null $null @{ 'Origin' = 'http://evil.example.com' }
$acao = $null
try { $acao = $cors.headers['Access-Control-Allow-Origin'] } catch { }
if ($acao -and $acao -eq 'http://evil.example.com') {
  Write-Output ("[FAIL] CORS 反射任意 Origin：" + $acao); $script:fail++
} else {
  Write-Output ("[PASS] CORS 不反射未授权 Origin（返回 " + $(if($acao){$acao}else{"无该头"}) + "）"); $script:pass++
}

Write-Output ''
Write-Output '=== G. XSS / 注入探针 ==='
$xss = Req 'Get' ('/api/v1/products?keyword=' + [uri]::EscapeDataString('<script>alert(1)</script>')) $null $buyer.token
Check 'XSS 关键字不导致 500' $xss 200
$inject = Req 'Get' ("/api/v1/products?keyword=" + [uri]::EscapeDataString("' OR 1=1 --")) $null $buyer.token
Check 'SQL 注入关键字不导致 500' $inject 200
$bigId = Req 'Get' '/api/v1/products/999999999999999999999' $null $buyer.token
if ($bigId.code -in @(400,404)) { Write-Output ("[PASS] 超大数字 ID 被拒 HTTP " + $bigId.code); $script:pass++ }
else { Write-Output ("[FAIL] 超大数字 ID 未按预期拒绝 HTTP " + $bigId.code); $script:fail++ }

Write-Output ''
Write-Output ("=== 汇总: PASS " + $script:pass + " / FAIL " + $script:fail + " ===")
if ($script:fail -gt 0) { exit 1 }
