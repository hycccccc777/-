@echo off
chcp 65001 >nul
cd /d "%~dp0"

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
echo ============================================================
echo  如果别人还是打不开，请以管理员身份运行本脚本（右键 -^> 以管理员身份运行）
echo  或手动放行防火墙：控制面板 -^> 防火墙 -^> 高级设置 -^> 入站规则 -^> 新建端口 TCP 5174
echo ============================================================
echo 按任意键关闭窗口...
pause >nul