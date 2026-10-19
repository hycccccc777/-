[CmdletBinding()]
param(
    [string] $MysqlBin = "C:\Program Files\MySQL\MySQL Server 26.7\bin",
    [string] $User     = "root",
    [string] $Password = "qazwsx@134567",
    [string] $TestDb   = "shop_test",
    [switch] $Keep
)

$ErrorActionPreference = "Stop"
$mysql = Join-Path $MysqlBin "mysql.exe"
if (-not (Test-Path $mysql)) { throw "mysql client not found: $mysql" }

$files = @("ry_20260417.sql", "quartz.sql", "campus_trade.sql",
           "campus_menu.sql", "campus_seed_products.sql", "campus_test_data.sql")

function Invoke-Sql {
    param([string] $ArgString, [string] $InputFile, [string] $Label)
    $p = @{ FilePath = $mysql; ArgumentList = $ArgString
            NoNewWindow = $true; PassThru = $true; Wait = $true
            RedirectStandardError = "$env:TEMP\mysql_err.txt" }
    if ($InputFile) { $p["RedirectStandardInput"] = $InputFile }
    $r = Start-Process @p
    if ($r.ExitCode -ne 0) {
        $e = if (Test-Path "$env:TEMP\mysql_err.txt") { Get-Content "$env:TEMP\mysql_err.txt" -Raw } else { "" }
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
    $out = "$env:TEMP\mysql_out.txt"
    Start-Process -FilePath $mysql -ArgumentList "-u$User -p$Password --default-character-set=utf8mb4 -N -B $TestDb -e `"$sql`"" -NoNewWindow -PassThru -Wait -RedirectStandardOutput $out | Out-Null
    Write-Host ("      " + (Get-Content $out -Raw).Trim())
}
finally { Pop-Location }

Write-Host ""
Write-Host "test db ready: $TestDb"
Write-Host "run tests   : mvn -pl ruoyi-admin -am test"