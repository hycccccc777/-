@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================================
echo  校易淘 · 服务自检
echo ============================================
echo.

rem 用 curl 测状态码；curl 不存在时退回 powershell
set "PASS=0"
set "FAIL=0"

call :check "后端探活"            "http://localhost:8080/api/v1/system/health"
call :check "后端商品列表"        "http://localhost:8080/api/v1/products?pageSize=1"
call :check "后端类目列表"        "http://localhost:8080/api/v1/categories"
call :check "OpenAPI 契约"        "http://localhost:8080/v3/api-docs/campus"
call :check "Swagger UI"          "http://localhost:8080/swagger-ui/index.html"
call :check "前端页面"            "http://127.0.0.1:5174/"
call :check "经前端代理打后端"    "http://127.0.0.1:5174/api/v1/products?pageSize=1"

echo.
echo ============================================
echo  通过 %PASS% 项，失败 %FAIL% 项
echo ============================================
if %FAIL% gtr 0 (
  echo.
  echo 常见原因：
  echo   - 前端代理失败 -> 后端没起，先跑 启动后端.cmd
  echo   - 全部失败     -> MySQL/Redis 没启动
  echo   - 401/403      -> 接口需要登录，属正常
)
echo.
pause
exit /b 0

:check
set "CODE="
for /f "delims=" %%c in ('curl -s -o NUL -w "%%{http_code}" --max-time 10 %2 2^>nul') do set "CODE=%%c"
if "%CODE%"=="200" (
  set /a PASS+=1
  echo   [OK]   %~1
) else (
  set /a FAIL+=1
  echo   [FAIL] %~1  ^(HTTP %CODE%^)  %2
)
goto :eof
