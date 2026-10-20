# 采集本机运行环境与版本，输出到控制台，并生成 docs/screenshots/07-runtime-versions.png
# 用法：双击 采集环境信息.cmd，或 powershell -ExecutionPolicy Bypass -File docs\采集环境信息.ps1

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$shotDir = Join-Path $root 'docs\screenshots'
New-Item -ItemType Directory -Force -Path $shotDir | Out-Null
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$list = [System.Collections.Generic.List[pscustomobject]]::new()
function Add-Item($label, $cmd, $out) {
    $script:list.Add([pscustomobject]@{ L = $label; C = $cmd; O = ($out | Out-String).TrimEnd() })
    Write-Host ""
    Write-Host ("--- " + $label) -ForegroundColor Cyan
    Write-Host ("> " + $cmd) -ForegroundColor Yellow
    Write-Host ($out | Out-String).TrimEnd()
}

# 1. Java
$javaHome = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
Add-Item "编程语言 · Java（pom 编译目标 17）" "java -version" (& cmd.exe /c "`"$javaHome`" -version 2>&1")

# 2. Maven
$mvn = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
    $mvn = @(
        "$env:TEMP\maven-dist\apache-maven-3.9.16\bin\mvn.cmd",
        "$env:LOCALAPPDATA\campus-maven\apache-maven-3.9.9\bin\mvn.cmd"
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if ($mvn) { Add-Item "构建工具 · Apache Maven" "mvn -v" ((& $mvn -v 2>&1) | Select-Object -First 1) }
else { Add-Item "构建工具 · Apache Maven" "mvn -v" "未安装（启动后端.cmd 会自动下载）" }

# 3. Node / npm
$nodeOut = "node " + (& node -v 2>&1) + "`nnpm  " + (& npm -v 2>&1)
Add-Item "前端运行时 · Node.js / npm" "node -v ; npm -v" $nodeOut

# 4. Git
Add-Item "版本控制 · Git" "git --version" (& git --version 2>&1)

# 5. Redis
$redisOut = ''
try {
    $c = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 6379)
    $s = $c.GetStream(); $w = New-Object IO.StreamWriter($s); $w.NewLine = "`r`n"; $w.AutoFlush = $true
    $w.WriteLine('INFO server'); Start-Sleep -Milliseconds 400
    $r = New-Object IO.StreamReader($s); $b = New-Object char[] 4000
    $n = $r.Read($b, 0, 4000)
    $redisOut = ((-join $b[0..($n - 1)]) -split "`r`n" | Where-Object { $_ -match '^(redis_version|redis_mode|os|tcp_port):' }) -join "`n"
    $c.Close()
} catch { $redisOut = '连接失败：' + $_.Exception.Message }
Add-Item "缓存 · Redis" "redis-cli INFO server" $redisOut

# 6. MySQL
$mysqlExe = $null
$svc = Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^MySQL' } | Select-Object -First 1
if ($svc) {
    $exe = ($svc.PathName -replace '^"([^"]+)".*$', '$1')
    $dir = Split-Path $exe -Parent
    if (Test-Path (Join-Path $dir 'mysql.exe')) { $mysqlExe = Join-Path $dir 'mysql.exe' }
}
if ($mysqlExe -and $env:CAMPUS_DB_PASSWORD) {
    $ver = (& $mysqlExe -uroot "-p$($env:CAMPUS_DB_PASSWORD)" -N -e 'SELECT VERSION();' 2>$null) -join ''
    $tbl = (& $mysqlExe -uroot "-p$($env:CAMPUS_DB_PASSWORD)" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='shop';" 2>$null) -join ''
    Add-Item "数据库 · MySQL" "mysql -e SELECT VERSION()" ("MySQL " + $ver.Trim() + " / shop 库表数 " + $tbl.Trim())
} else {
    $line = (Select-String -Path (Join-Path $root 'logs\sys-info.log') -Pattern 'Database: jdbc' -Encoding UTF8 -ErrorAction SilentlyContinue | Select-Object -First 1)
    $txt = if ($line) { $line.Line.Substring($line.Line.IndexOf('Database:')) } else { '未找到启动日志（需先启动后端）' }
    Add-Item "数据库 · MySQL" "logs\sys-info.log" $txt
}

# 7. Docker
$dockerOut = if (Get-Command docker -ErrorAction SilentlyContinue) { (& docker --version 2>&1) } else { "本机未安装 Docker`n容器化真实验证见 GitHub Actions 容器编排验证作业" }
Add-Item "容器 · Docker" "docker --version" $dockerOut

# ---- 渲染成图 ----
$rows = foreach ($i in $list) {
    $esc = [System.Net.WebUtility]::HtmlEncode($i.O)
    $cmdEsc = [System.Net.WebUtility]::HtmlEncode('> ' + $i.C)
    "<div class='blk'><div class='lbl'>$($i.L)</div><pre><span class='p'>$cmdEsc</span>`n$esc</pre></div>"
}
$html = @"
<!doctype html><html><head><meta charset='utf-8'><style>
body{margin:0;padding:26px 30px;background:#0d1117;font-family:'Cascadia Mono','Consolas',monospace;color:#c9d1d9}
h1{font:600 19px 'Segoe UI','Microsoft YaHei';color:#e6edf3;margin:0 0 3px}
.sub{font:12.5px 'Segoe UI','Microsoft YaHei';color:#8b949e;margin-bottom:20px}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:13px}
.blk{background:#161b22;border:1px solid #30363d;border-radius:8px;overflow:hidden}
.lbl{background:#1f2937;color:#7ee787;font:600 12px 'Microsoft YaHei';padding:7px 12px;border-bottom:1px solid #30363d}
pre{margin:0;padding:10px 13px;font-size:11.5px;line-height:1.55;white-space:pre-wrap;word-break:break-all}
.p{color:#58a6ff}
</style></head><body>
<h1>校易淘 · 运行环境与版本采集</h1>
<div class='sub'>本机实际执行输出</div>
<div class='grid'>$($rows -join "`n")</div></body></html>
"@
$tmpHtml = Join-Path $env:TEMP 'env-evidence.html'
[System.IO.File]::WriteAllText($tmpHtml, $html, $utf8NoBom)

$edge = @(
    "$env:ProgramFiles (x86)\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($edge) {
    $out = Join-Path $shotDir '07-runtime-versions.png'
    $prof = Join-Path $env:TEMP ('edge-env-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
    & $edge --headless=new --disable-gpu --no-first-run --no-default-browser-check --hide-scrollbars --user-data-dir="$prof" --screenshot="$out" --window-size="1480,760" --virtual-time-budget=3000 ("file:///" + ($tmpHtml -replace '\\', '/')) 2>&1 | Out-Null
    Start-Sleep -Milliseconds 800
    Remove-Item $prof -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host ""
    Write-Host ("截图已生成：" + $out) -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host ("未找到 Edge/Chrome，HTML 已生成：" + $tmpHtml) -ForegroundColor Yellow
}
