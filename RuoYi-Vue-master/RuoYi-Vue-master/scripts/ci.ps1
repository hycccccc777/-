<#
    校易淘 · 统一 CI 脚本

    作用：把「编译 → 造测试库 → 跑测试 → 构建」这套流程固化下来，
    让本地、自建 Runner、GitHub Actions 跑的都是同一套逻辑，
    避免出现「本地全绿、CI 挂了」这种无法定位的分歧。

    用法：
        powershell -ExecutionPolicy Bypass -File scripts/ci.ps1
        powershell -ExecutionPolicy Bypass -File scripts/ci.ps1 -SkipFrontend
        powershell -ExecutionPolicy Bypass -File scripts/ci.ps1 -SkipTests

    环境变量（都有默认值，本地开箱即用）：
        CAMPUS_DB_USERNAME  默认 root
        CAMPUS_DB_PASSWORD  默认本地开发密码
#>
[CmdletBinding()]
param(
    [switch] $SkipTests,
    [switch] $SkipFrontend,
    [switch] $SkipDocker,
    [switch] $SkipBackend,
    [string] $Maven
)

$ErrorActionPreference = 'Stop'

# ---------- 定位项目根 ----------
$scriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Resolve-Path (Join-Path $scriptDir '..')
# 用 Parent 链而非 '..\..'：反斜杠在 Linux 上不是路径分隔符，会导致 CI 找不到前端目录
$repoRoot   = (Get-Item $backendDir).Parent.Parent.FullName
$frontendDir = Join-Path $repoRoot '校易淘前端-后端交接-2026-10-19T01-05-57-755Z'

# ---------- 环境变量兜底 ----------
if (-not $env:CAMPUS_DB_USERNAME) { $env:CAMPUS_DB_USERNAME = 'root' }
if (-not $env:CAMPUS_DB_PASSWORD) { $env:CAMPUS_DB_PASSWORD = 'qazwsx@134567' }
if (-not $env:MYSQL_HOST)         { $env:MYSQL_HOST         = '127.0.0.1' }

$testDb = 'shop_test'

# ---------- 结果收集 ----------
$results = [System.Collections.Generic.List[object]]::new()

function Invoke-Step {
    param(
        [string]   $Name,
        [scriptblock] $Action,
        [switch]   $Optional
    )

    Write-Host ''
    Write-Host ('=' * 70) -ForegroundColor DarkGray
    Write-Host "▶ $Name" -ForegroundColor Cyan
    Write-Host ('=' * 70) -ForegroundColor DarkGray

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & $Action
        $sw.Stop()
        $script:results.Add([pscustomobject]@{ Step = $Name; Status = 'PASS'; Seconds = [math]::Round($sw.Elapsed.TotalSeconds, 1) })
        Write-Host "✓ $Name 通过（$([math]::Round($sw.Elapsed.TotalSeconds,1))s）" -ForegroundColor Green
    }
    catch {
        $sw.Stop()
        if ($Optional) {
            $script:results.Add([pscustomobject]@{ Step = $Name; Status = 'SKIP'; Seconds = [math]::Round($sw.Elapsed.TotalSeconds, 1) })
            Write-Host "⚠ $Name 跳过：$($_.Exception.Message)" -ForegroundColor Yellow
        }
        else {
            $script:results.Add([pscustomobject]@{ Step = $Name; Status = 'FAIL'; Seconds = [math]::Round($sw.Elapsed.TotalSeconds, 1) })
            Write-Host "✗ $Name 失败：$($_.Exception.Message)" -ForegroundColor Red
            throw
        }
    }
}

function Assert-LastExitCode {
    param([string] $What)
    if ($LASTEXITCODE -ne 0) { throw "$What 返回非零退出码 $LASTEXITCODE" }
}

function Resolve-Maven {
    # 找不到 Maven 时给出可执行的指引，而不是抛出「术语无法识别」这种无法定位的错
    param([string] $Explicit)

    if ($Explicit) {
        if (Test-Path $Explicit) { return $Explicit }
        throw "指定的 Maven 路径不存在：$Explicit"
    }
    if ($env:MAVEN_HOME) {
        $candidate = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
        if (Test-Path $candidate) { return $candidate }
    }
    $onPath = Get-Command mvn -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    throw "找不到 Maven。三种解决方式：`n" +
          "  1) 把 Maven 的 bin 目录加入 PATH`n" +
          "  2) 设置环境变量 MAVEN_HOME 指向 Maven 安装目录`n" +
          "  3) 用参数显式指定：-Maven <mvn.cmd 的完整路径>"
}

$mvn = Resolve-Maven -Explicit $Maven

Write-Host ''
Write-Host '校易淘 CI 开始' -ForegroundColor Green
Write-Host ("  仓库根目录 : " + $repoRoot)
$prevPref = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$jdkInfo = ((& java -version 2>&1) | Out-String) -split "`r?`n" | Select-Object -First 1
$ErrorActionPreference = $prevPref
Write-Host ("  JDK        : " + $jdkInfo)
Write-Host ("  数据库     : " + $env:MYSQL_HOST + " / " + $testDb)

# ---------- 1. 编译 ----------
if (-not $SkipBackend) {
Invoke-Step '后端编译' {
    Push-Location $backendDir
    try { & $mvn -B -DskipTests clean package; Assert-LastExitCode 'mvn package' }
    finally { Pop-Location }
}
}

# ---------- 2. 测试库 ----------
if (-not $SkipTests -and -not $SkipBackend) {
    Invoke-Step '准备测试库' {
        & (Join-Path $scriptDir 'setup-test-db.ps1')
        Assert-LastExitCode 'setup-test-db.ps1'
    }

    # ---------- 3. 测试 ----------
    Invoke-Step '后端测试' {
        Push-Location $backendDir
        try { & $mvn -B -pl ruoyi-admin -am test; Assert-LastExitCode 'mvn test' }
        finally { Pop-Location }
    }

    # 汇总测试数量
    $reports = Join-Path (Join-Path $backendDir 'ruoyi-admin') 'target/surefire-reports/*.txt'
    $total = 0; $failed = 0
    Get-ChildItem $reports -ErrorAction SilentlyContinue | ForEach-Object {
        $line = Get-Content $_.FullName -Encoding UTF8 | Select-String -Pattern 'Tests run:' | Select-Object -First 1
        if ($line -and $line.Line -match 'Tests run: (\d+), Failures: (\d+), Errors: (\d+)') {
            $total  += [int]$Matches[1]
            $failed += [int]$Matches[2] + [int]$Matches[3]
        }
    }
    if ($failed -gt 0) { throw "有 $failed 个用例未通过（共 $total 个）" }
    Write-Host ("  用例总数: $total，全部通过") -ForegroundColor Green
}

# ---------- 4. 前端 ----------
if (-not $SkipFrontend) {
    Invoke-Step '前端依赖安装' {
        Push-Location $frontendDir
        try { & npm ci --registry=https://registry.npmmirror.com; Assert-LastExitCode 'npm ci' }
        finally { Pop-Location }
    }

    Invoke-Step '前端单元测试' {
        Push-Location $frontendDir
        try { & npm test; Assert-LastExitCode 'npm test' }
        finally { Pop-Location }
    }

    Invoke-Step '前端构建' {
        Push-Location $frontendDir
        try { & npm run build; Assert-LastExitCode 'npm run build' }
        finally { Pop-Location }
    }
}

# ---------- 5. 镜像（可选，需要 Docker） ----------
if (-not $SkipDocker) {
    Invoke-Step '构建后端镜像' {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw '未检测到 Docker' }
        Push-Location $backendDir
        try { & docker build -t school-easy-backend:local .; Assert-LastExitCode 'docker build' }
        finally { Pop-Location }
    } -Optional
}

# ---------- 汇总 ----------
Write-Host ''
Write-Host ('=' * 70) -ForegroundColor DarkGray
Write-Host 'CI 汇总' -ForegroundColor Green
Write-Host ('=' * 70) -ForegroundColor DarkGray
$results | Format-Table -AutoSize | Out-String | Write-Host

$hasFail = @($results | Where-Object { $_.Status -eq 'FAIL' }).Count -gt 0
if ($hasFail) {
    Write-Host 'CI 结果：失败' -ForegroundColor Red
    exit 1
}

Write-Host 'CI 结果：全部通过' -ForegroundColor Green
exit 0