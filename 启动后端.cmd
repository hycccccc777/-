@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0RuoYi-Vue-master\RuoYi-Vue-master"

set "PORT=%~1"
if not defined PORT set "PORT=8080"

set "MVN_DIR=%LOCALAPPDATA%\campus-maven"
set "MVN_VER=3.9.9"
set "MVN_HOME=%MVN_DIR%\apache-maven-%MVN_VER%"

echo ============================================
echo  校易淘 · 后端启动    端口 %PORT%
echo ============================================
echo.

rem ---------- 1. 检查 MySQL / Redis ----------
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [x] 没检测到 3306 端口，请先启动 MySQL 服务（默认服务名 MySQL267）
  pause & exit /b 1
)
echo [OK] MySQL 3306 在监听
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [x] 没检测到 6379 端口，请先启动 Redis 服务
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

rem ---------- 3. 已有 jar 就直接跑，不需要 Maven ----------
if exist "ruoyi-admin\target\ruoyi-admin.jar" (
  echo [OK] jar 已存在，跳过构建
  goto :run
)

echo.
echo [1/3] 未找到 jar，需要构建

rem ---------- 4. 定位 Maven，找不到就自动下载 ----------
set "MVN_CMD="
where mvn >nul 2>nul && set "MVN_CMD=mvn"
if not defined MVN_CMD if exist "%MVN_HOME%\bin\mvn.cmd" set "MVN_CMD=%MVN_HOME%\bin\mvn.cmd"
if not defined MVN_CMD for /d %%d in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
  for /d %%e in ("%%d\*") do if exist "%%e\bin\mvn.cmd" set "MVN_CMD=%%e\bin\mvn.cmd"
)

if not defined MVN_CMD (
  echo     未找到 Maven，自动下载 %MVN_VER% 到 %MVN_HOME%
  if not exist "%MVN_DIR%" mkdir "%MVN_DIR%"
  set "DL_OK="
  for %%u in (
    https://archive.apache.org/dist/maven/maven-3/%MVN_VER%/binaries/apache-maven-%MVN_VER%-bin.zip
    https://dlcdn.apache.org/maven/maven-3/%MVN_VER%/binaries/apache-maven-%MVN_VER%-bin.zip
    https://mirrors.aliyun.com/apache/maven/maven-3/%MVN_VER%/binaries/apache-maven-%MVN_VER%-bin.zip
  ) do (
    if not defined DL_OK (
      echo     尝试镜像: %%u
      curl.exe -L --max-time 180 -s -o "%MVN_DIR%\maven.zip" "%%u"
      if exist "%MVN_DIR%\maven.zip" for %%s in ("%MVN_DIR%\maven.zip") do if %%~zs GTR 5000000 set "DL_OK=1"
    )
  )
  if not defined DL_OK (
    echo [x] 所有镜像下载失败，请手动下载并解压到 %MVN_HOME%
    echo     https://archive.apache.org/dist/maven/maven-3/%MVN_VER%/binaries/apache-maven-%MVN_VER%-bin.zip
    pause & exit /b 1
  )
  powershell -NoProfile -Command "Expand-Archive -LiteralPath '%MVN_DIR%\maven.zip' -DestinationPath '%MVN_DIR%' -Force" >nul
  del /q "%MVN_DIR%\maven.zip" 2>nul
  if exist "%MVN_HOME%\bin\mvn.cmd" (
    set "MVN_CMD=%MVN_HOME%\bin\mvn.cmd"
  ) else (
    echo [x] Maven 自动下载失败。
    echo     可选：手动安装 Maven 并加入 PATH，或用镜像手动下载：
    echo     https://archive.apache.org/dist/maven/maven-3/%MVN_VER%/binaries/apache-maven-%MVN_VER%-bin.zip
    pause & exit /b 1
  )
)
echo [OK] mvn: %MVN_CMD%

echo.
echo [2/3] 正在构建（首次约 1-2 分钟）...
call "%MVN_CMD%" -q -pl ruoyi-admin -am -DskipTests package
if errorlevel 1 (
  echo [x] 构建失败，请检查上方错误
  pause & exit /b 1
)

:run
rem 数据库口令：优先用已存在的环境变量，其次读仓库根的 .env（已 gitignore）
rem 仓库里不保存任何真实口令；.env 缺失时会明确报错而不是静默用默认值
if not defined CAMPUS_DB_PASSWORD if exist "%~dp0.env" (
  for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0.env") do (
    if /i "%%a"=="CAMPUS_DB_PASSWORD" set "CAMPUS_DB_PASSWORD=%%b"
  )
)
if not defined CAMPUS_DB_PASSWORD (
  echo.
  echo [x] 未找到数据库口令。
  echo     请在仓库根创建 .env，内容一行：CAMPUS_DB_PASSWORD=你的密码
  echo     模板见 .env.example
  pause & exit /b 1
)
set "LOG_PATH=%~dp0logs"
if not exist "%LOG_PATH%" mkdir "%LOG_PATH%"

echo.
echo [3/3] 启动后端...
echo     接口   http://localhost:%PORT%/api/v1/products?pageSize=1
echo     文档   http://localhost:%PORT%/swagger-ui/index.html
echo     探活   http://localhost:%PORT%/api/v1/system/health
echo     日志   %LOG_PATH%
echo     按 Ctrl+C 停止（换端口：启动后端.cmd 8090）
echo.
"%JAVA_BIN%" -jar ruoyi-admin\target\ruoyi-admin.jar --server.port=%PORT%
pause