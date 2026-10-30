#!/usr/bin/env pwsh
# 覆盖率门禁：读取 JaCoCo 聚合报告，校验 ruoyi-campus 业务模块的行覆盖率不低于阈值。
#
# 为什么不用 jacoco:check：测试都跑在 ruoyi-admin，业务代码在 ruoyi-campus，
# 单模块的 check 要么统计不到业务类（0% 必失败），要么只统计 admin 的空壳类。
# 直接读 report-aggregate 产出的 CSV 更准确，也与 CI 上传的产物一致。
#
# 用法：pwsh scripts/check-coverage.ps1 [-Minimum 35] [-Report <csv路径>]
param(
    [double]$Minimum = 35,
    [string]$Report
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
if (-not $Report) {
    $Report = Join-Path $repo 'RuoYi-Vue-master/RuoYi-Vue-master/ruoyi-admin/target/site/jacoco-aggregate/jacoco.csv'
}

if (-not (Test-Path $Report)) {
    Write-Host "[x] 找不到覆盖率报告：$Report" -ForegroundColor Red
    Write-Host "    请先运行测试生成报告（scripts/coverage.ps1 或 mvn -pl ruoyi-admin -am test）" -ForegroundColor Yellow
    exit 1
}

$rows = Import-Csv $Report
$campus = $rows | Where-Object { $_.PACKAGE -match 'campus' }
if (-not $campus) {
    Write-Host '[x] 报告里没有 campus 包，覆盖率为空，门禁无法判定' -ForegroundColor Red
    exit 1
}

function Sum($set, $col) { ($set | Measure-Object $col -Sum).Sum }
function Pct($covered, $missed) {
    $total = [double]$covered + [double]$missed
    if ($total -eq 0) { return 0 }
    return [math]::Round(100 * [double]$covered / $total, 2)
}

$lc = Sum $campus LINE_COVERED
$lm = Sum $campus LINE_MISSED
$bc = Sum $campus BRANCH_COVERED
$bm = Sum $campus BRANCH_MISSED
$ic = Sum $campus INSTRUCTION_COVERED
$im = Sum $campus INSTRUCTION_MISSED

$linePct = Pct $lc $lm
$branchPct = Pct $bc $bm
$instrPct = Pct $ic $im

Write-Host ''
Write-Host '=== 覆盖率门禁（ruoyi-campus 业务模块）===' -ForegroundColor Cyan
Write-Host ("  类数     : " + $campus.Count)
Write-Host ("  行覆盖   : " + $lc + "/" + ($lc + $lm) + " = " + $linePct + "%   (阈值 " + $Minimum + "%)")
Write-Host ("  指令覆盖 : " + $ic + "/" + ($ic + $im) + " = " + $instrPct + "%")
Write-Host ("  分支覆盖 : " + $bc + "/" + ($bc + $bm) + " = " + $branchPct + "%")
Write-Host ''

if ($linePct -lt $Minimum) {
    Write-Host ("[x] 门禁未通过：行覆盖率 " + $linePct + "% < " + $Minimum + "%") -ForegroundColor Red
    # 列出拖后腿的类，方便定位该补哪些测试
    $worst = $campus | ForEach-Object {
        $t = [int]$_.LINE_COVERED + [int]$_.LINE_MISSED
        if ($t -ge 20) {
            [pscustomobject]@{ Class = $_.CLASS; Missed = [int]$_.LINE_MISSED; Total = $t; Pct = [math]::Round(100 * [int]$_.LINE_COVERED / $t, 1) }
        }
    } | Sort-Object Missed -Descending | Select-Object -First 10

    Write-Host '  未覆盖行数最多的类：' -ForegroundColor Yellow
    foreach ($w in $worst) {
        Write-Host ("    " + $w.Class.PadRight(34) + " 未覆盖 " + ([string]$w.Missed).PadLeft(4) + " 行 / 共 " + ([string]$w.Total).PadLeft(4) + " 行  (" + $w.Pct + "%)")
    }
    exit 1
}

Write-Host ("[OK] 门禁通过：行覆盖率 " + $linePct + "% >= " + $Minimum + "%") -ForegroundColor Green
exit 0
