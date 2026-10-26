@echo off
chcp 65001 >nul
echo.
echo 停止 后端 / 前端 / 公网隧道...
for %%p in (8080 5174) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%p " ^| findstr LISTENING') do (
    taskkill /f /pid %%a >nul 2>nul && echo   已停止端口 %%p - pid %%a
  )
)
taskkill /f /im cloudflared.exe >nul 2>nul && echo   已停止 cloudflared
echo.
echo 完成。
pause