@echo off
chcp 65001 >nul
cd /d "%~dp0校易淘前端-后端交接-2026-10-19T01-05-57-755Z"

if not exist "node_modules" (
  echo 正在安装依赖...
  call npm install --registry=https://registry.npmmirror.com || goto :fail
)

echo.
echo 启动前端开发服务器：http://127.0.0.1:5174
echo 后端需另行启动（双击 启动后端.cmd）
echo 按 Ctrl+C 停止
echo.
call npm run dev
goto :eof

:fail
echo 依赖安装失败，请检查网络。
pause
