@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0RuoYi-Vue-master\RuoYi-Vue-master"

echo ============================================
echo  校易淘 · 后端启动
echo ============================================
echo.

rem ---------- 1. 检查 MySQL / Redis 是否在跑 ----------
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [x] 没检测到 3306 端口，请先启动 MySQL 服务 MySQL267
  pause & exit /b 1
)
echo [OK] MySQL 3306 在监听
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [x] 没检测到 6379 端口，请先启动 Redis 服务 Redis
  pause & exit /b 1
)
echo [OK] Redis 6379 在监听

rem ---------- 2. 定位 java ----------
set "JAVA_BIN="
if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_BIN (
  for %%j in (java.exe) do if not "%%~$PATH:j"=="" set "JAVA_BIN=%%~$PATH:j"
)
if not defined JAVA_BIN (
  echo [x] 找不到 java，请安装 JDK 17+ 或设置 JAVA_HOME
  pause & exit /b 1
)
echo [OK] java: %JAVA_BIN%

rem ---------- 3. 定位 mvn（仅在需要构建时用）----------
set "MVN_CMD="
where mvn >nul 2>nul && set "MVN_CMD=mvn"
if not defined MVN_CMD (
  for %%m in ("%TEMP%\maven-dist\apache-maven-3.9.16\bin\mvn.cmd") do if exist "%%~m" set "MVN_CMD=%%~m"
)
if not defined MVN_CMD (
  for %%m in ("%USERPROFILE%\.m2\wrapper\dists\*.mvn") do set "MVN_CMD=%%~m\bin\mvn.cmd"
)

rem ---------- 4. 确保 jar 存在 ----------
if not exist "ruoyi-admin\target\ruoyi-admin.jar" (
  echo.
  echo [1/2] 未找到 jar，正在构建（首次约需 1-2 分钟）...
  if not defined MVN_CMD (
    echo [x] 找不到 mvn，请安装 Maven 或手动执行:
    echo     mvn -pl ruoyi-admin -am -DskipTests package
    pause & exit /b 1
  )
  call "%MVN_CMD%" -pl ruoyi-admin -am -DskipTests package
  if errorlevel 1 (
    echo [x] 构建失败，请检查上方错误
    pause & exit /b 1
  )
) else (
  echo [OK] jar 已存在，跳过构建
)

rem ---------- 5. 启动 ----------
set "CAMPUS_DB_PASSWORD=qazwsx@134567"
set "LOG_PATH=%~dp0logs"
if not exist "%LOG_PATH%" mkdir "%LOG_PATH%"

echo.
echo [2/2] 启动后端...
echo     接口   http://localhost:8080/api/v1/products?pageSize=1
echo     文档   http://localhost:8080/swagger-ui/index.html
echo     探活   http://localhost:8080/api/v1/system/health
echo     日志   %LOG_PATH%
echo     按 Ctrl+C 停止
echo.
"%JAVA_BIN%" -jar ruoyi-admin\target\ruoyi-admin.jar
pause
