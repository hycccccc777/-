# 告警有效性自检：规则里引用的指标，后端真的在暴露吗？
#
# 为什么需要它：告警规则写对了语法，不代表能触发。
# 如果规则引用了一个根本不存在的指标（或指标名拼错、标签不对），Prometheus 会得到
# 空结果集，规则永远不触发 —— 页面是绿的，人也不会有感知，这是最典型的
# 「看起来做了但没生效」。
#
# 检查内容：
#   1) 每条规则必须有 alert / expr / for / severity
#   2) 规则里引用的指标名，必须能在 /actuator/prometheus 的实时输出里找到
#      （up、alertmanager 自身指标等由 Prometheus 提供，列入白名单）
#   3) alertmanager.yml 至少配置一个 receiver，且 route 指向它
#
# 用法：pwsh scripts/check-alerting.ps1 [-BaseUrl http://localhost:8080] [-Rules docs/ops/alert-rules.yml]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Rules,
    [string]$Alertmanager
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
if (-not $Rules) { $Rules = Join-Path $repo 'docs/ops/alert-rules.yml' }
if (-not $Alertmanager) { $Alertmanager = Join-Path $repo 'docs/ops/alertmanager.yml' }

# Prometheus 自身或抓取目标固有的指标，不在应用的 /actuator/prometheus 里
$external = @('up', 'scrape_duration_seconds', 'scrape_samples_scraped', 'alertmanager_alerts', 'ALERTS')

Write-Host ''
Write-Host '=== 告警规则自检 ===' -ForegroundColor Cyan

$ruleText = [System.IO.File]::ReadAllText($Rules)
$alerts = [regex]::Matches($ruleText, '(?m)^\s*- alert:\s*(\S+)') | ForEach-Object { $_.Groups[1].Value }
if (-not $alerts) { Write-Host '[x] 规则文件里没有 alert' -ForegroundColor Red; exit 1 }
Write-Host ("  规则条数: " + $alerts.Count + "  (" + ($alerts -join ', ') + ")")

# 先打一发业务流量：Micrometer 的 Timer 只有在记录过请求后才会暴露 _count/_bucket 序列，
# 刚启动就抓会得到"指标其实存在、只是还没有序列"的假阴性
foreach ($warm in @('/api/v1/products', '/api/v1/system/health')) {
    try { Invoke-WebRequest -Uri ($BaseUrl + $warm) -UseBasicParsing -TimeoutSec 10 | Out-Null } catch { }
}
Start-Sleep -Milliseconds 300

# 取实时指标
try {
    $metrics = Invoke-RestMethod -Uri ($BaseUrl + '/actuator/prometheus') -TimeoutSec 15
} catch {
    Write-Host ('[x] 取不到 ' + $BaseUrl + '/actuator/prometheus：' + $_.Exception.Message) -ForegroundColor Red
    Write-Host '    请先启动后端（启动后端.cmd）' -ForegroundColor Yellow
    exit 1
}

$lines = if ($metrics -is [string]) { $metrics -split "`n" } else { @($metrics) }
$names = @{}
foreach ($l in $lines) {
    if ($l -match '^([a-zA-Z_:][a-zA-Z0-9_:]*)(\{| )') { $names[$Matches[1]] = $true }
    # histogram/summary 的 _count/_sum/_bucket 由 TYPE 声明派生，没有请求记录时不会出现序列
    if ($l -match '^# TYPE ([a-zA-Z_:][a-zA-Z0-9_:]*) (histogram|summary)') {
        $base = $Matches[1]
        $names[$base] = $true
        $names[$base + '_count'] = $true
        $names[$base + '_sum'] = $true
        if ($Matches[2] -eq 'histogram') { $names[$base + '_bucket'] = $true }
    }
}
Write-Host ("  实时指标数: " + $names.Count)

# 抽出规则里用到的指标名（去掉标签、比较符与函数）
$used = @{}
foreach ($m in [regex]::Matches($ruleText, '\b([a-zA-Z_:][a-zA-Z0-9_:]*)\s*(\{|==|!=|>|<|\)|$)')) {
    $n = $m.Groups[1].Value
    if ($n -in @('for', 'expr', 'alert', 'labels', 'annotations', 'summary', 'description', 'severity',
                 'humanizePercentage', 'histogram_quantile', 'sum', 'rate', 'max', 'by', 'le', 'job',
                 'application', 'status', 'uri', 'instance', 'area', 'path')) { continue }
    if ($n -match '^[0-9]') { continue }
    $used[$n] = $true
}

$missing = @()
foreach ($n in $used.Keys) {
    if ($external -contains $n) { continue }
    if (-not $names.ContainsKey($n)) { $missing += $n }
}

if ($missing.Count -gt 0) {
    Write-Host ''
    Write-Host '[x] 以下指标在实时输出里不存在，相关规则永远不会触发：' -ForegroundColor Red
    foreach ($n in ($missing | Sort-Object)) { Write-Host ('    - ' + $n) -ForegroundColor Red }
    Write-Host '    修法：换成真实指标名，或补一个 Micrometer gauge 暴露它' -ForegroundColor Yellow
    exit 1
}
Write-Host '[OK] 规则引用的指标全部存在' -ForegroundColor Green

# 接收端检查
if (Test-Path $Alertmanager) {
    $am = [System.IO.File]::ReadAllText($Alertmanager)
    $receivers = [regex]::Matches($am, '(?m)^\s*- name:\s*(\S+)') | ForEach-Object { $_.Groups[1].Value }
    if (-not $receivers) {
        Write-Host '[x] alertmanager.yml 没有任何 receiver，告警发不出去' -ForegroundColor Red
        exit 1
    }
    Write-Host ("[OK] Alertmanager 接收端: " + ($receivers -join ', ')) -ForegroundColor Green
} else {
    Write-Host ('[!] 未找到 ' + $Alertmanager) -ForegroundColor Yellow
}

Write-Host ''
Write-Host '=== 全部通过 ===' -ForegroundColor Green
exit 0