@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 正在本地构建并打包镜像（linux/amd64）...
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\package-images.ps1" -Only all
echo.
echo 按任意键关闭窗口...
pause >nul