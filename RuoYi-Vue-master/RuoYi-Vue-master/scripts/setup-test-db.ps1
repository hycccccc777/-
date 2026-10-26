[CmdletBinding()]
param(
    [string] $MysqlBin = "",
    [string] $User     = "root",
    [string] $Password = $env:CAMPUS_DB_PASSWORD,
    [string] $TestDb   = "shop_test",
    [switch] $Keep
)

$ErrorActionPreference = "Stop"

# Windows 的 $env:TEMP 在 Linux/macOS 上不存在，统一改用 .NET 的临时目录
$tmpDir = [System.IO.Path]::GetTempPath()
# 必须用 Join-Path 拼接：Linux 上 `\` 不是路径分隔符，
# 直接拼 "$tmpDir\file" 会被当成根目录下的同名文件并 Permission denied
$errFile = Join-Path $tmpDir "mysql_err.txt"
$outFile = Join-Path $tmpDir "mysql_out.txt"
if ($MysqlBin) {
    # 显式指定时兼容 Windows 的 mysql.exe 与类 Unix 的 mysql
    $mysql = @((Join-Path $MysqlBin 'mysql.exe'), (Join-Path $MysqlBin 'mysql')) |
             Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $mysql) { throw "在 $MysqlBin 下找不到 mysql 客户端" }
} else {
    # 先看 PATH（Linux/CI 上由系统包提供），再回退到 Windows 默认安装位置
    $onPath = Get-Command mysql -ErrorAction SilentlyContinue
    if ($onPath) {
        $mysql = $onPath.Source
    } else {
        $guess = Get-ChildItem 'C:\Program Files\MySQL' -Filter 'mysql.exe' -Recurse -ErrorAction SilentlyContinue |
                 Select-Object -First 1
        if (-not $guess) { throw "找不到 mysql 客户端：请安装到 PATH，或用 -MysqlBin 指定目录" }
        $mysql = $guess.FullName
    }
}

$files = @("ry_20260417.sql", "quartz.sql", "campus_trade.sql",
           "campus_menu.sql", "campus_seed_products.sql", "campus_test_data.sql")

function Invoke-Sql {
    param([string] $ArgString, [string] $InputFile, [string] $Label)
    $p = @{ FilePath = $mysql; ArgumentList = $ArgString
            NoNewWindow = $true; PassThru = $true; Wait = $true
            RedirectStandardError = $errFile }
    if ($InputFile) { $p["RedirectStandardInput"] = $InputFile }
    $r = Start-Process @p
    if ($r.ExitCode -ne 0) {
        $e = if (Test-Path $errFile) { Get-Content $errFile -Raw } else { "" }
        throw "$Label failed (exit=$($r.ExitCode)) :: $e"
    }
}

Push-Location (Join-Path $PSScriptRoot "..\sql")
try {
    if (-not $Keep) {
        Write-Host "[1/4] drop database $TestDb"
        Invoke-Sql -ArgString "-u$User -p$Password -e `"drop database if exists $TestDb`"" -Label "drop"
    }
    Write-Host "[2/4] create database $TestDb (utf8mb4)"
    Invoke-Sql -ArgString "-u$User -p$Password -e `"create database if not exists $TestDb default character set utf8mb4 collate utf8mb4_general_ci`"" -Label "create"

    Write-Host "[3/4] import scripts"
    for ($i = 0; $i -lt $files.Count; $i++) {
        $f = $files[$i]
        if (-not (Test-Path $f)) { throw "missing script: $f" }
        Invoke-Sql -ArgString "-u$User -p$Password --default-character-set=utf8mb4 $TestDb" -InputFile $f -Label ("import " + $f)
        Write-Host ("      [{0}/{1}] {2} OK" -f ($i + 1), $files.Count, $f)
    }

    Write-Host "[4/4] verify"
    $sql = "select concat('buyers=',(select count(*) from sys_user where user_id between 2001 and 2020),' seller=',(select count(*) from sys_user where user_id=2100),' test_products=',(select count(*) from ct_product where product_id>=9001),' limit_stock=',(select stock from ct_product where product_id=9001));"
    $out = $outFile
    Start-Process -FilePath $mysql -ArgumentList "-u$User -p$Password --default-character-set=utf8mb4 -N -B $TestDb -e `"$sql`"" -NoNewWindow -PassThru -Wait -RedirectStandardOutput $out | Out-Null
    Write-Host ("      " + (Get-Content $out -Raw).Trim())
}
finally { Pop-Location }

Write-Host ""
Write-Host "test db ready: $TestDb"
Write-Host "run tests   : mvn -pl ruoyi-admin -am test"