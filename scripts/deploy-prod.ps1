# 生产上线一键脚本（Windows / Docker Desktop，或装了 Docker 的机器）
# 逻辑：读 .env -> 校验生产必填项 -> docker compose up -d --build -> 等健康 -> 打印访问信息
# 由仓库根目录的「部署上线.cmd」双击调用。

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Ok($m)   { Write-Host "[OK]   $m" -ForegroundColor Green }
function Warn($m) { Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "[FAIL] $m" -ForegroundColor Red; exit 1 }

Write-Host "==== 校易淘 · 生产上线 ====" -ForegroundColor Cyan

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Warn "已从 .env.example 生成 .env。请先编辑 .env 填好："
    Write-Host "       MYSQL_ROOT_PASSWORD / CAMPUS_JWT_SECRET / SPRING_PROFILES_ACTIVE=druid,prod"
    Write-Host "       保存后重新双击本脚本。"
    Read-Host "按回车退出" | Out-Null
    exit 0
}

$envMap = @{}
Get-Content ".env" -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $i = $line.IndexOf("=")
    if ($i -gt 0) { $envMap[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim() }
}
function Get-Env($k, $def = "") { if ($envMap.ContainsKey($k) -and $envMap[$k] -ne "") { return $envMap[$k] } else { return $def } }
function Test-Placeholder([string]$v) {
    if ([string]::IsNullOrWhiteSpace($v)) { return $true }
    if ($v.StartsWith("<") -and $v.EndsWith(">")) { return $true }
    return $v.Contains("change-me")
}

# ---- 必填校验 ----
$profiles = Get-Env "SPRING_PROFILES_ACTIVE"
if ($profiles -notmatch "prod") { Fail "SPRING_PROFILES_ACTIVE 必须包含 prod（建议 druid,prod），否则不会启用生产自检" }

$jwt = Get-Env "CAMPUS_JWT_SECRET"
$jwtBad = (Test-Placeholder $jwt) -or ($jwt.Length -lt 32)
if ($jwtBad) { Fail "CAMPUS_JWT_SECRET 必须是 >=32 位随机串（openssl rand -hex 48）" }

$dbPwd = Get-Env "MYSQL_ROOT_PASSWORD"
if (Test-Placeholder $dbPwd) { Fail "MYSQL_ROOT_PASSWORD 未设置" }

# 管理员初始密码：为空则自动生成并回填 .env（仅在 admin 仍为默认密码时由后端应用）
$adminPwd = Get-Env "CAMPUS_ADMIN_PASSWORD"
if ([string]::IsNullOrWhiteSpace($adminPwd)) {
    $chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
    $adminPwd = -join (1..16 | ForEach-Object { $chars[(Get-Random -Maximum $chars.Length)] })
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::AppendAllText((Join-Path (Get-Location) ".env"), "`r`nCAMPUS_ADMIN_PASSWORD=$adminPwd`r`n", $utf8NoBom)
    $envMap["CAMPUS_ADMIN_PASSWORD"] = $adminPwd
    Ok "已生成管理员初始密码并写入 .env（上线后会打印）"
} elseif ($adminPwd.Length -lt 8) {
    Fail "CAMPUS_ADMIN_PASSWORD 少于 8 位，请改长一点"
}

$payMethod = Get-Env "CAMPUS_PAY_METHOD" "4"
$paySecret = Get-Env "CAMPUS_PAY_SECRET"
$payMissing = Test-Placeholder $paySecret
if ($payMethod -ne "4" -and $payMissing) {
    Fail "已选择在线支付（CAMPUS_PAY_METHOD=$payMethod），必须配置 CAMPUS_PAY_SECRET 随机串"
}

$cors = Get-Env "CAMPUS_CORS_ORIGINS"
$corsBad = $cors.Contains("localhost") -or $cors.Contains("127.0.0.1") -or $cors.Contains("*")
if ($corsBad) { Fail "CAMPUS_CORS_ORIGINS 含 localhost/127.0.0.1/通配符，生产不允许" }
if ($cors -eq "") { Warn "CAMPUS_CORS_ORIGINS 为空：仅同源可访问（前端经 Nginx 反代 /api 时没问题）" }

$smsEnabled = (Get-Env "CAMPUS_SMS_ENABLED" "false") -eq "true"
if ($smsEnabled) {
    $smsId = Get-Env "TENCENT_SMS_SECRET_ID"
    $smsTpl = Get-Env "TENCENT_SMS_TEMPLATE_ID"
    $smsMissing = (Test-Placeholder $smsId) -or (Test-Placeholder $smsTpl)
    if ($smsMissing) {
        Fail "开启了短信验证（CAMPUS_SMS_ENABLED=true）但缺少腾讯云短信凭据/模板；要么补齐，要么先设为 false"
    }
    Ok "短信验证已启用"
} else {
    Warn "短信验证未启用：注册为账号密码模式（后续可在 .env 配置腾讯云短信后重开）"
}

$storage = Get-Env "CAMPUS_STORAGE_TYPE" "local"
if ($storage -eq "cos") {
    $cosId = Get-Env "TENCENT_COS_SECRET_ID"
    $cosBucket = Get-Env "TENCENT_COS_BUCKET"
    $cosMissing = (Test-Placeholder $cosId) -or (Test-Placeholder $cosBucket)
    if ($cosMissing) {
        Warn "CAMPUS_STORAGE_TYPE=cos 但缺少 COS 凭据，将回退本地磁盘（图片经 /profile 反代，可用）"
    } else {
        Ok "图片存储：腾讯云 COS"
    }
} else {
    Ok "图片存储：本地磁盘（图片经 Nginx /profile 反代）"
}
Ok "生产配置前置校验通过"

try { & docker compose version | Out-Null } catch { Fail "未检测到 docker compose，请先安装 Docker Desktop / Docker Engine" }

Write-Host "`n== 构建并启动全部服务（首次构建可能 5-15 分钟）==" -ForegroundColor Cyan
& docker compose up -d --build
if ($LASTEXITCODE -ne 0) { Fail "docker compose 启动失败，请查看上方日志" }

$port = Get-Env "BACKEND_PORT" "8080"
Write-Host "`n== 等待后端就绪（端口 $port）==" -ForegroundColor Cyan
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 3
    try {
        $r = Invoke-WebRequest "http://127.0.0.1:$port/api/v1/config" -UseBasicParsing -TimeoutSec 4
        if ($r.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
}
if (-not $ready) {
    Warn "后端 90 秒内未就绪，可能仍在启动或配置有误。可执行：docker compose logs backend"
    exit 1
}

# 清理演示卖家与演示商品（默认开；CAMPUS_CLEAN_DEMO=false 可关）
if ((Get-Env "CAMPUS_CLEAN_DEMO" "true") -ne "false") {
    Write-Host "`n== 清理演示数据 ==" -ForegroundColor Cyan
    $db = Get-Env "MYSQL_DATABASE" "shop"
    try {
        Get-Content "scripts\clean-demo.sql" -Raw | & docker compose exec -T mysql mysql -uroot ("-p" + $dbPwd) $db | Out-Null
        if ($LASTEXITCODE -eq 0) { Ok "演示卖家/商品已清理" } else { Warn "清理演示数据返回非 0，可稍后手动执行 scripts\clean-demo.sql" }
    } catch { Warn "清理演示数据失败：$($_.Exception.Message)" }
}

$fePort = Get-Env "FRONTEND_PORT" "5174"
Write-Host "`n==== 上线完成 ====" -ForegroundColor Green
Write-Host ("  本机访问：  http://127.0.0.1:{0}" -f $fePort)
Write-Host  "  公网访问：  配置域名 + HTTPS 反向代理到本机 $fePort（详见 docs/上线部署.md）"
Write-Host  "  支付方式：  线下面交（买家线下付款后确认）"
Write-Host ("  管理员账号： admin / {0}" -f $adminPwd)
Write-Host  "  健康检查：  已通过 /api/v1/config"
Write-Host ""
Write-Host  "  如需关闭：  docker compose down"
Read-Host "按回车关闭窗口" | Out-Null
