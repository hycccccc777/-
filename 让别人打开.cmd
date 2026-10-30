@echo off
chcp 65001 >nul
cd /d "%~dp0"

:: 需要管理员权限来添加防火墙放行规则；不是管理员时自动提权重跑
net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

:: 兼容两种目录布局：仓库根（scripts\）与交付包（同目录）
set "PS1="
if exist "%~dp0scripts\share-public.ps1" set "PS1=%~dp0scripts\share-public.ps1"
if exist "%~dp0share-public.ps1" set "PS1=%~dp0share-public.ps1"
if not defined PS1 (
    echo [x] 找不到 share-public.ps1
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
echo.
echo 按任意键关闭窗口...
pause >nul