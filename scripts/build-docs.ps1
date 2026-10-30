# 从 docs 下的 HTML 源重建 docx 与 pdf
#
# 为什么要这个脚本：
#   文档的正文源是 HTML（纯文本，便于 diff 与评审）。
#   Word 导出需要图片用**相对路径 + 短文件名**，直接指向 docs\screenshots\01-swagger-ui.png
#   这种长名字在部分 WPS 版本里会解析失败，所以导出前把图片摊平到临时目录的 img\NN.png。
#
# 用法（需要本机已装 WPS 或 Word）：
#   powershell -ExecutionPolicy Bypass -File scripts\build-docs.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\build-docs.ps1 -NoPdf
#   powershell -ExecutionPolicy Bypass -File scripts\build-docs.ps1 -SourceHtml docs\测试报告.html -OutName 测试报告
param(
    [switch] $NoPdf,
    [string] $SourceHtml = 'docs\校易淘-验收材料汇编.html',
    [string] $OutName = '校易淘-验收材料汇编'
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$srcHtml = if ([System.IO.Path]::IsPathRooted($SourceHtml)) { $SourceHtml } else { Join-Path $repo $SourceHtml }
$shots = Join-Path $repo 'docs\screenshots'
$build = Join-Path $env:TEMP 'campus-docbuild'
$imgDir = Join-Path $build 'img'

if (-not (Test-Path $srcHtml)) { throw "找不到源文件：$srcHtml" }

Write-Host ('=== 重建：' + $OutName + ' ===') -ForegroundColor Cyan

# ---------- 1) 准备构建目录 ----------
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Path $imgDir -Force | Out-Null

$utf8 = New-Object System.Text.UTF8Encoding($false)
$html = [System.IO.File]::ReadAllText($srcHtml, $utf8)

# ---------- 2) 复制图片并改写引用为 img/NN.png ----------
# 形如 screenshots\13-business-loop.png -> img\13.png
$count = 0
foreach ($m in [regex]::Matches($html, 'src="screenshots/(\d\d)-[^"]+\.png"')) {
    $nn = $m.Groups[1].Value
    $real = Get-ChildItem $shots -Filter ($nn + '-*.png') | Select-Object -First 1
    if ($real) { Copy-Item $real.FullName (Join-Path $imgDir ($nn + '.png')) -Force; $count++ }
    else { Write-Host ("  [!] 缺图：screenshots/" + $nn + "-*.png") -ForegroundColor Yellow }
}
$html = [regex]::Replace($html, 'src="screenshots/(\d\d)-[^"]+\.png"', 'src="img/$1.png"')

$buildHtml = Join-Path $build 'doc.html'
[System.IO.File]::WriteAllText($buildHtml, $html, $utf8)
Write-Host ("    图片 " + $count + " 张已摊平到 " + $imgDir)

# ---------- 3) 用 WPS/Word 导出 ----------
$docx = Join-Path $repo ('docs\' + $OutName + '.docx')
$pdf = Join-Path $repo ('docs\' + $OutName + '.pdf')
Remove-Item $docx -Force -ErrorAction SilentlyContinue

$app = $null
foreach ($progId in @('Kwps.Application', 'Word.Application')) {
    try { $app = New-Object -ComObject $progId; break } catch { }
}
if (-not $app) { throw '找不到 WPS 或 Word，无法导出 docx。请安装其一后重试。' }

$app.Visible = $false
try { $app.DisplayAlerts = 0 } catch { }
try {
    $doc = $app.Documents.Open($buildHtml, $false, $false, $false)
    # 页边距 1cm（56.7 twips），让内容区更宽，减少表格折行
    try {
        $doc.PageSetup.TopMargin = 56.7; $doc.PageSetup.BottomMargin = 56.7
        $doc.PageSetup.LeftMargin = 56.7; $doc.PageSetup.RightMargin = 56.7
    } catch { }
    $doc.SaveAs2($docx, 16)
    if (-not $NoPdf) { $doc.ExportAsFixedFormat($pdf, 17) }
    $pages = try { $doc.ComputeStatistics(2) } catch { 0 }
    $words = try { $doc.ComputeStatistics(0) } catch { 0 }
    Write-Host ("    docx=" + (Test-Path $docx) + "  pdf=" + (Test-Path $pdf) +
        "  pages=$pages words=$words tables=" + $doc.Tables.Count + " images=" + $doc.InlineShapes.Count) -ForegroundColor Green
    $doc.Close($false)
} finally {
    try { $app.Quit() } catch { }
    try { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($app) | Out-Null } catch { }
    [GC]::Collect()
    Get-Process wps, winword -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
}

Write-Host ''
Write-Host '完成。产物：' -ForegroundColor Green
Write-Host ('    ' + $docx)
if (Test-Path $pdf) { Write-Host ('    ' + $pdf) }
