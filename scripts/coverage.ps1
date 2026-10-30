# 生成后端测试覆盖率报告（JaCoCo）
#
# 为什么需要这个脚本：
#   JaCoCo 的 javaagent 通过命令行参数接收输出路径，如果仓库放在含中文的目录下
#   （例如 %USERPROFILE%\Desktop\校园二手），Maven 传给 agent 的路径会被错误解码，
#   agent 静默失败、不产出 jacoco.exec，覆盖率报告因此是 0%。
#   本脚本把后端源码复制到纯英文的临时目录再跑，规避这个问题。
#
# 用法：powershell -ExecutionPolicy Bypass -File scripts\coverage.ps1
param(
    [string]$Maven,
    [switch]$KeepTemp
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repo 'RuoYi-Vue-master\RuoYi-Vue-master'
$openapi = Join-Path $repo 'openapi'
$temp = Join-Path $env:TEMP 'campus-coverage'
$tempOpenapi = Join-Path (Split-Path $temp -Parent) 'openapi'

if (-not (Test-Path $backend)) {
    Write-Host ("[x] 找不到后端目录: " + $backend) -ForegroundColor Red
    exit 1
}

# 定位 Maven
if (-not $Maven) {
    $Maven = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
}
if (-not $Maven) {
    $Maven = @(
        "$env:TEMP\maven-dist\apache-maven-3.9.16\bin\mvn.cmd",
        "$env:LOCALAPPDATA\campus-maven\apache-maven-3.9.9\bin\mvn.cmd"
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $Maven) {
    Write-Host '[x] 找不到 mvn.cmd，请先运行 启动后端.cmd（会自动下载 Maven）或用 -Maven 指定' -ForegroundColor Red
    exit 1
}

Write-Host ("→ 复制源码到 " + $temp)
Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue
& robocopy $backend $temp /E /XD target .git /NFL /NDL /NJH /NJS /NP | Out-Null

# ApiContractTest 用相对路径 ("..","..","..","openapi") 定位契约文件，
# 基准是模块目录 ruoyi-admin，因此要把 openapi 放到它算出来的那个位置
$expectedOpenapi = [System.IO.Path]::GetFullPath((Join-Path $temp 'ruoyi-admin\..\..\..\openapi'))
if (Test-Path $openapi) {
    New-Item -ItemType Directory -Force -Path $expectedOpenapi | Out-Null
    Copy-Item (Join-Path $openapi '*') $expectedOpenapi -Force -ErrorAction SilentlyContinue
}

Write-Host '→ 运行测试并采集覆盖率（约 1 分钟）'
Push-Location $temp
& $Maven -B -pl ruoyi-admin -am test
$code = $LASTEXITCODE
Pop-Location

$csv = Join-Path $temp 'ruoyi-admin\target\site\jacoco-aggregate\jacoco.csv'
$html = Join-Path $temp 'ruoyi-admin\target\site\jacoco-aggregate\index.html'

if ($code -ne 0) {
    Write-Host '[x] 测试失败，覆盖率可能不完整' -ForegroundColor Red
}
if (-not (Test-Path $csv)) {
    Write-Host '[x] 没有生成 jacoco.csv' -ForegroundColor Red
    exit 1
}

# 汇总 campus 业务模块
$rows = Import-Csv $csv
$camp = $rows | Where-Object { $_.PACKAGE -match 'campus' }
function P($c, $m) { $t = [double]$c + [double]$m; if ($t -eq 0) { 'n/a' } else { ([math]::Round(100 * [double]$c / $t, 1)).ToString() + '%' } }
function S($set, $col) { ($set | Measure-Object $col -Sum).Sum }

$ic = S $camp INSTRUCTION_COVERED; $im = S $camp INSTRUCTION_MISSED
$bc = S $camp BRANCH_COVERED; $bm = S $camp BRANCH_MISSED
$lc = S $camp LINE_COVERED; $lm = S $camp LINE_MISSED

Write-Host ''
Write-Host '=== ruoyi-campus 业务模块覆盖率 ===' -ForegroundColor Cyan
Write-Host ('  指令 ' + $ic + '/' + ($ic + $im) + ' = ' + (P $ic $im))
Write-Host ('  分支 ' + $bc + '/' + ($bc + $bm) + ' = ' + (P $bc $bm))
Write-Host ('  行   ' + $lc + '/' + ($lc + $lm) + ' = ' + (P $lc $lm))
Write-Host ''
Write-Host ('HTML 报告：' + $html) -ForegroundColor Green
Write-Host '（用浏览器打开可查看每个类、每个方法的覆盖情况）'

if (-not $KeepTemp) {
    Write-Host ('（临时目录保留在 ' + $temp + '，便于查看报告；传 -KeepTemp 可保留，否则下次运行会覆盖）') -ForegroundColor Gray
}
