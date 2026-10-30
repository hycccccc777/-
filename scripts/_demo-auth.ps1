# 演示脚本共用的登录助手：令牌缓存 + 429 等待重试
#
# 为什么需要：
#   登录接口限流是 10 次/分钟/IP（这是产品该有的行为，不能改）。
#   但演示时「reset → 闭环 → 权限」连续跑两轮，光登录就超过 10 次，
#   第二轮的 Login 会吃到 429，脚本直接红字退出 —— 看起来像系统坏了，
#   实际是演示脚本把自己限流了。
#
# 做法：
#   1) 把登录拿到的 token 缓存到临时文件，同一手机号在有效期内复用，
#      重复演示不再重复登录；缓存里不落密码。
#   2) 真吃到 429 时，按提示等待窗口过去再试一次，并打印明确原因，
#      而不是抛一个看不出所以然的异常。
#
# 用法（在脚本里 dot-source 后调用）：
#   . "$PSScriptRoot\_demo-auth.ps1"
#   $buyer = Get-DemoToken -BaseUrl $BaseUrl -Phone '13800001001'

$script:DemoTokenCacheFile = Join-Path $env:TEMP 'campus-demo-tokens.json'
$script:DemoTokenCache = $null

function Load-DemoTokenCache {
    if ($script:DemoTokenCache) { return }
    $script:DemoTokenCache = @{}
    if (Test-Path $script:DemoTokenCacheFile) {
        try {
            $obj = Get-Content $script:DemoTokenCacheFile -Raw -Encoding UTF8 | ConvertFrom-Json
            foreach ($p in $obj.PSObject.Properties) { $script:DemoTokenCache[$p.Name] = $p.Value }
        } catch {
            # 缓存文件坏了就当没有，不要因此让演示失败
            $script:DemoTokenCache = @{}
        }
    }
}

function Save-DemoTokenCache {
    try {
        ($script:DemoTokenCache | ConvertTo-Json -Depth 4) | Set-Content $script:DemoTokenCacheFile -Encoding UTF8
    } catch { }
}

function Clear-DemoTokenCache {
    Remove-Item $script:DemoTokenCacheFile -Force -ErrorAction SilentlyContinue
    $script:DemoTokenCache = @{}
}

# 返回与 RuoYi 登录接口 data 部分同结构的对象：{ token, user = { id, nickname, ... } }
# 缓存有效期取保守的 20 分钟（实际 access token 是 30 分钟），避免刚好在过期边界上失效。
function Get-DemoToken {
    param(
        [Parameter(Mandatory)] [string] $BaseUrl,
        [Parameter(Mandatory)] [string] $Phone,
        [string] $Password = 'admin123',
        [switch] $ForceRefresh
    )

    Load-DemoTokenCache
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    if (-not $ForceRefresh -and $script:DemoTokenCache.ContainsKey($Phone)) {
        $cached = $script:DemoTokenCache[$Phone]
        if ($cached.issuedAt -and ($now - $cached.issuedAt) -lt 1200) {
            return $cached.data
        }
    }

    $attempt = 0
    while ($true) {
        $attempt++
        try {
            $resp = Invoke-RestMethod -Method Post -Uri ($BaseUrl + '/api/v1/auth/login') `
                -Headers @{ 'Content-Type' = 'application/json' } `
                -Body (@{ phone = $Phone; password = $Password } | ConvertTo-Json) `
                -TimeoutSec 20
            $script:DemoTokenCache[$Phone] = @{ issuedAt = $now; data = $resp.data }
            Save-DemoTokenCache
            return $resp.data
        } catch {
            $status = 0
            try { $status = [int]$_.Exception.Response.StatusCode } catch { }
            $body = ''
            try {
                $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $body = $sr.ReadToEnd(); $sr.Close()
            } catch { }

            if ($status -eq 429 -and $attempt -lt 3) {
                Write-Host ("      [登录限流] 触发 10 次/分钟上限，等待 60 秒后重试（这是预期行为，不是故障）") -ForegroundColor Yellow
                Start-Sleep -Seconds 60
                $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
                continue
            }
            if ($status -eq 429) {
                Write-Host '      [登录限流] 等待后仍未放行：1 分钟内登录次数确实过多。' -ForegroundColor Red
                Write-Host '      限流计数在进程内（campus.ratelimit.store 默认 memory），因此无法从外部清除。' -ForegroundColor Red
                Write-Host '      处理：等 1 分钟再演示，或重启后端让计数归零。' -ForegroundColor Red
            }
            throw ("登录失败（" + $Phone + "）：HTTP " + $status + " " + $body)
        }
    }
}
