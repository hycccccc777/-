# 校易淘 · 运维维护菜单（由「维护菜单.cmd」双击调用）
# 所有操作都基于 docker compose + 仓库根目录的 .env。
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Ok($m)   { Write-Host "[OK]   $m" -ForegroundColor Green }
function Warn($m) { Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "[FAIL] $m" -ForegroundColor Red }

function Read-EnvMap {
    $map = @{}
    if (Test-Path ".env") {
        Get-Content ".env" -Encoding UTF8 | ForEach-Object {
            $line = $_.Trim()
            if ($line -eq "" -or $line.StartsWith("#")) { return }
            $i = $line.IndexOf("=")
            if ($i -gt 0) { $map[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim() }
        }
    }
    return $map
}
function GetV($map, $k, $def = "") { if ($map.ContainsKey($k) -and $map[$k] -ne "") { return $map[$k] } else { return $def } }
function Set-Env($k, $v) {
    $path = Join-Path $root ".env"
    $lines = @(Get-Content $path -Encoding UTF8)
    $found = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match ("^" + [regex]::Escape($k) + "=")) { $lines[$i] = "$k=$v"; $found = $true }
    }
    if (-not $found) { $lines += "$k=$v" }
    [System.IO.File]::WriteAllText($path, ($lines -join "`r`n") + "`r`n", (New-Object System.Text.UTF8Encoding($false)))
}

function Ensure-Docker {
    try { & docker compose version | Out-Null } catch { Fail "未检测到 docker compose"; return $false }
    return $true
}
function Wait-Backend($port) {
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 3
        try { $r = Invoke-WebRequest "http://127.0.0.1:$port/api/v1/config" -UseBasicParsing -TimeoutSec 4; if ($r.StatusCode -eq 200) { return $true } } catch { }
    }
    return $false
}

function Do-CleanDemo {
    if (-not (Ensure-Docker)) { return }
    $map = Read-EnvMap
    $db = GetV $map "MYSQL_DATABASE" "shop"
    $pwd = GetV $map "MYSQL_ROOT_PASSWORD"
    if ([string]::IsNullOrWhiteSpace($pwd)) { Fail "MYSQL_ROOT_PASSWORD 为空"; return }
    Write-Host "清理演示卖家/商品..."
    Get-Content "scripts\clean-demo.sql" -Raw | & docker compose exec -T mysql mysql -uroot ("-p" + $pwd) $db
    if ($LASTEXITCODE -eq 0) { Ok "演示数据已清理" } else { Fail "清理失败" }
}

function Do-ResetAdmin {
    if (-not (Ensure-Docker)) { return }
    $map = Read-EnvMap
    $pwd1 = Read-Host "输入新的管理员密码（>=8 位，直接回车则自动生成）"
    if ([string]::IsNullOrWhiteSpace($pwd1)) {
        $chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        $pwd1 = -join (1..16 | ForEach-Object { $chars[(Get-Random -Maximum $chars.Length)] })
        Write-Host "已生成密码：$pwd1" -ForegroundColor Yellow
    }
    if ($pwd1.Length -lt 8) { Fail "密码太短（<8）"; return }
    Set-Env "CAMPUS_ADMIN_PASSWORD" $pwd1
    Set-Env "CAMPUS_ADMIN_FORCE_RESET" "true"
    Write-Host "重启后端以应用新密码..."
    & docker compose up -d backend | Out-Null
    $port = GetV $map "BACKEND_PORT" "8080"
    if (Wait-Backend $port) {
        Set-Env "CAMPUS_ADMIN_FORCE_RESET" "false"   # 关掉强制重置，下次重启不再改
        Ok ("管理员已更新：admin / " + $pwd1)
    } else {
        Warn "后端未就绪，请稍后看 docker compose logs backend"
    }
}

function Do-Backup {
    if (-not (Ensure-Docker)) { return }
    $map = Read-EnvMap
    $db = GetV $map "MYSQL_DATABASE" "shop"
    $pwd = GetV $map "MYSQL_ROOT_PASSWORD"
    $dir = Join-Path $root "backups"
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
    $file = Join-Path $dir ("campus-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".sql")
    Write-Host "导出到 $file ..."
    cmd /c "docker compose exec -T mysql mysqldump -uroot -p$pwd --single-transaction --routines --triggers $db > `"$file`""
    if ($LASTEXITCODE -eq 0 -and (Test-Path $file)) { Ok ("备份完成：" + $file + "（" + [math]::Round((Get-Item $file).Length/1KB,1) + " KB）") } else { Fail "备份失败" }
}

function Do-Logs {
    if (-not (Ensure-Docker)) { return }
    & docker compose logs --tail 150 backend
}

function Do-Status {
    if (-not (Ensure-Docker)) { return }
    & docker compose ps
}

function Do-Restart {
    if (-not (Ensure-Docker)) { return }
    & docker compose restart
    Ok "已重启全部服务"
}

while ($true) {
    Write-Host ""
    Write-Host "==== 校易淘 · 运维维护菜单 ====" -ForegroundColor Cyan
    Write-Host "  1) 清理演示卖家/商品"
    Write-Host "  2) 重置管理员密码"
    Write-Host "  3) 备份数据库"
    Write-Host "  4) 查看后端日志（最近 150 行）"
    Write-Host "  5) 查看服务状态"
    Write-Host "  6) 重启全部服务"
    Write-Host "  0) 退出"
    $choice = Read-Host "请选择"
    try {
        switch ($choice) {
            "1" { Do-CleanDemo }
            "2" { Do-ResetAdmin }
            "3" { Do-Backup }
            "4" { Do-Logs }
            "5" { Do-Status }
            "6" { Do-Restart }
            "0" { break }
            default { Warn "无效选项" }
        }
    } catch {
        Fail ("执行出错：" + $_.Exception.Message)
    }
    if ($choice -eq "0") { break }
}
