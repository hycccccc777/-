@echo off
chcp 65001 >nul
cd /d "%~dp0.."

if not defined CAMPUS_DB_PASSWORD for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0..\.env") do if /i "%%a"=="CAMPUS_DB_PASSWORD" set "CAMPUS_DB_PASSWORD=%%b"

echo ================================================
echo  校易淘 · 环境与版本采集
echo  会在 docs\screenshots 生成 07-runtime-versions.png
echo ================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0采集环境信息.ps1"

echo.
echo 采集完成，可直接对本窗口截图作为原始终端证据。
pause
