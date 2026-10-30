# 生成本地 HTTPS 自签名证书（PKCS12），供 application-https.yml 使用
#
# 用法：powershell -ExecutionPolicy Bypass -File scripts\gen-cert.ps1
# 产物：RuoYi-Vue-master\RuoYi-Vue-master\certs\campus-dev.p12（私钥，已在 .gitignore 中排除）、certs\campus-dev.cer（公钥，可导入信任）
#
# 生产环境不要用自签证书：应由 ACME（Let's Encrypt）签发，
# 或在云负载均衡 / Nginx 上终止 TLS。
param(
    [string]$Password = 'changeit',
    [string]$Subject  = 'CN=localhost'
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$certDir = Join-Path $root 'RuoYi-Vue-master\RuoYi-Vue-master\certs'
New-Item -ItemType Directory -Force -Path $certDir | Out-Null

$p12 = Join-Path $certDir 'campus-dev.p12'
if (Test-Path $p12) {
    Write-Host ("已存在：" + $p12 + "（如需重建请先删除）") -ForegroundColor Yellow
    exit 0
}

Write-Host '→ 生成自签名证书（2 年有效期，SAN: localhost / 127.0.0.1）'
$cert = New-SelfSignedCertificate `
    -Subject $Subject `
    -DnsName @('localhost', '127.0.0.1') `
    -CertStoreLocation 'Cert:\CurrentUser\My' `
    -NotAfter (Get-Date).AddYears(2) `
    -KeyExportPolicy Exportable `
    -KeyAlgorithm RSA `
    -KeyLength 2048

$secure = ConvertTo-SecureString -String $Password -Force -AsPlainText
Export-PfxCertificate -Cert $cert -FilePath $p12 -Password $secure | Out-Null
Export-Certificate -Cert $cert -FilePath (Join-Path $certDir 'campus-dev.cer') | Out-Null

# 从用户证书存储里移除，避免污染系统证书列表（p12 已导出，够用）
Remove-Item ("Cert:\CurrentUser\My\" + $cert.Thumbprint) -Force -ErrorAction SilentlyContinue

Write-Host ('[OK] ' + $p12 + '  (' + [math]::Round((Get-Item $p12).Length / 1KB, 1) + ' KB)') -ForegroundColor Green
Write-Host ('[OK] ' + (Join-Path $certDir 'campus-dev.cer')) -ForegroundColor Green
Write-Host ''
Write-Host '启动 HTTPS：' -ForegroundColor Cyan
Write-Host '  java -jar ruoyi-admin\target\ruoyi-admin.jar --spring.profiles.active=druid,https'
Write-Host '验证：'
Write-Host '  curl -k https://localhost:8443/api/v1/system/health'
