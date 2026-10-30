# 允许同网段的其它设备访问前端端口（Windows 默认在「公用网络」下阻止入站）
#
# 为什么单独一个脚本：加防火墙规则需要管理员权限，混在一键脚本里会让
# 普通用户卡在 UAC 弹窗上；这里显式提示「右键以管理员身份运行」。
#
# 用法（必须以管理员身份运行 PowerShell）：
#   powershell -ExecutionPolicy Bypass -File scripts\open-lan-port.ps1 -Port 5174
param([int]$Port = 5174)

$ErrorActionPreference = 'Stop'
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host '[x] 需要管理员权限：请右键「以管理员身份运行」PowerShell 再执行本脚本' -ForegroundColor Red
    exit 1
}

$name = "校易淘前端 $Port"
$existing = Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host ("[=] 规则已存在：" + $name) -ForegroundColor Yellow
} else {
    New-NetFirewallRule -DisplayName $name -Direction Inbound -Protocol TCP -LocalPort $Port `
        -Action Allow -Profile Any -Description '校易淘演示：允许同网段访问前端开发服务器' | Out-Null
    Write-Host ("[OK] 已放行 TCP $Port（所有网络配置文件）") -ForegroundColor Green
}

$lan = (Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.IPv4Address } | Select-Object -First 1).IPv4Address.IPAddress
if ($lan) { Write-Host ("     同 WiFi 访问：http://${lan}:$Port") -ForegroundColor Green }
Write-Host '     撤销：Remove-NetFirewallRule -DisplayName "校易淘前端 5174"' -ForegroundColor DarkGray