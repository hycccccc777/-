# 重置演示数据：让「业务闭环演示」可以反复跑
#
# 为什么必须有这个脚本：
#   演示脚本会真的下单、扣库存、生成订单。跑过一次之后，
#   商品变成「已售出（status=4）、库存 0」，第二次演示就直接卡在
#   「没有可下单的商品」。一次性脚本不算演示，能反复跑才算。
#
# 做法：只重置「交易产生」的数据，不动用户与分类（那些是基线）：
#   - ct_order / ct_order_item / ct_payment / ct_idempotency / ct_refund
#   - ct_cart_item / ct_address / ct_favorite（购物车与收藏会被演示污染）
#   - 把 ct_product 的库存与状态恢复成 seed 里的初始值
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\reset-demo-data.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\reset-demo-data.ps1 -DryRun
param(
    [string] $Database = 'shop',
    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'

# ---------- 定位 mysql 客户端 ----------
function Resolve-Mysql {
    $c = (Get-Command mysql.exe -ErrorAction SilentlyContinue).Source
    if ($c) { return $c }
    $roots = @("$env:ProgramFiles\MySQL", "${env:ProgramFiles(x86)}\MySQL")
    foreach ($r in $roots) {
        if (-not (Test-Path $r)) { continue }
        $f = Get-ChildItem $r -Recurse -Filter 'mysql.exe' -ErrorAction SilentlyContinue |
             Sort-Object FullName -Descending | Select-Object -First 1
        if ($f) { return $f.FullName }
    }
    throw '找不到 mysql.exe：请把 MySQL 的 bin 目录加入 PATH，或用 -Mysql 指定'
}

# ---------- 口令：环境变量优先，其次仓库根 .env ----------
$repo = Split-Path -Parent $PSScriptRoot
if (-not $env:CAMPUS_DB_PASSWORD) {
    $envFile = Join-Path $repo '.env'
    if (Test-Path $envFile) {
        foreach ($line in Get-Content $envFile -Encoding UTF8) {
            if ($line -match '^\s*CAMPUS_DB_PASSWORD\s*=\s*(.+)$') { $env:CAMPUS_DB_PASSWORD = $Matches[1].Trim(); break }
        }
    }
}
if (-not $env:CAMPUS_DB_PASSWORD) {
    Write-Host '[x] 缺少数据库口令：请在仓库根 .env 写 CAMPUS_DB_PASSWORD=...' -ForegroundColor Red
    exit 1
}

$mysql = Resolve-Mysql
$env:MYSQL_PWD = $env:CAMPUS_DB_PASSWORD

function Invoke-Sql($sql) {
    $out = & $mysql -uroot --default-character-set=utf8mb4 -D $Database -N -B -e $sql 2>&1
    if ($LASTEXITCODE -ne 0) { throw ("SQL 执行失败：" + ($out -join "`n")) }
    return $out
}

Write-Host ''
Write-Host '=== 重置演示数据 ===' -ForegroundColor Cyan
Write-Host ("    库：$Database")

# ---------- 重置前快照 ----------
$orders = [int](Invoke-Sql 'SELECT COUNT(*) FROM ct_order')
$idem   = [int](Invoke-Sql 'SELECT COUNT(*) FROM ct_idempotency')
$sold   = [int](Invoke-Sql 'SELECT COUNT(*) FROM ct_product WHERE status = 4')
Write-Host ("    重置前：订单 $orders 条，幂等键 $idem 条，已售出商品 $sold 件")

if ($DryRun) {
    Write-Host ''
    Write-Host '    -DryRun：只报告，不改数据' -ForegroundColor Yellow
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    exit 0
}

# ---------- 1) 清交易数据（按外键依赖顺序）----------
# 先退款/支付，再订单项，再订单；幂等与购物车独立
$tables = @(
    'ct_refund',
    'ct_payment',
    'ct_order_item',
    'ct_order',
    'ct_idempotency',
    'ct_cart_item',
    'ct_address',
    'ct_favorite'
)
Write-Host ''
Write-Host '[1/2] 清理交易数据'
foreach ($t in $tables) {
    try {
        $n = [int](Invoke-Sql ("SELECT COUNT(*) FROM $t"))
        Invoke-Sql ("DELETE FROM $t") | Out-Null
        Write-Host ("      $t".PadRight(22) + " 清掉 $n 行")
    } catch {
        Write-Host ("      $t 跳过（" + $_.Exception.Message.Split("`n")[0] + "）") -ForegroundColor DarkYellow
    }
}

# ---------- 2) 恢复商品库存与状态 ----------
# seed 里 8 件商品都是 stock=1、status=1（已上架）。
# 这里按 product_id 精确恢复，不整表删重建 —— 避免误删用户后来新增的数据。
Write-Host ''
Write-Host '[2/2] 恢复商品库存与上架状态'
$rows = Invoke-Sql 'SELECT product_id FROM ct_product ORDER BY product_id'
$restored = 0
# 注意：变量不能叫 $pid —— 那是 PowerShell 的自动变量（当前进程 ID），只读
foreach ($productId in $rows) {
    if ([string]::IsNullOrWhiteSpace($productId)) { continue }
    Invoke-Sql ("UPDATE ct_product SET stock = 1, status = 1 WHERE product_id = " + [int]$productId) | Out-Null
    $restored++
}
Write-Host ("      已恢复 $restored 件商品为 stock=1 / status=1（已上架）")

# ---------- 校验 ----------
$err = 0
$soldAfter = [int](Invoke-Sql 'SELECT COUNT(*) FROM ct_product WHERE status = 4')
$zeroStock = [int](Invoke-Sql 'SELECT COUNT(*) FROM ct_product WHERE stock <= 0')
$orderAfter = [int](Invoke-Sql 'SELECT COUNT(*) FROM ct_order')
if ($soldAfter -ne 0) { Write-Host ("      [!] 仍有 $soldAfter 件已售出") -ForegroundColor Red; $err++ }
if ($zeroStock -ne 0) { Write-Host ("      [!] 仍有 $zeroStock 件库存为 0") -ForegroundColor Red; $err++ }
if ($orderAfter -ne 0) { Write-Host ("      [!] 仍有 $orderAfter 条订单") -ForegroundColor Red; $err++ }

# 至少要有「非本人出售且有库存」的商品，否则闭环还是跑不起来
$buyer = 1001
$candidates = [int](Invoke-Sql "SELECT COUNT(*) FROM ct_product WHERE seller_id <> $buyer AND stock > 0 AND status = 1")
if ($candidates -lt 1) {
    # 演示买家 1001 自己是卖家，若其余卖家都没货，就挑一件别的卖家的货补上
    Invoke-Sql "UPDATE ct_product SET stock = 1, status = 1 WHERE seller_id <> $buyer ORDER BY product_id LIMIT 1" | Out-Null
    $candidates = [int](Invoke-Sql "SELECT COUNT(*) FROM ct_product WHERE seller_id <> $buyer AND stock > 0 AND status = 1")
}

Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue

Write-Host ''
if ($err -eq 0 -and $candidates -ge 1) {
    Write-Host ('[OK] 复位完成：订单 0 条，8 件商品全部上架，可供买家 1001 下单的商品 ' + $candidates + ' 件') -ForegroundColor Green
    Write-Host '     下一步：powershell -File scripts\demo-business-loop.ps1' -ForegroundColor DarkGray
    exit 0
} else {
    Write-Host '[x] 复位不完整，请检查上面的提示' -ForegroundColor Red
    exit 1
}