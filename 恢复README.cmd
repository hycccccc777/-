@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo 正在从 git 还原 README.md（丢弃我误改坏的那份，改动未提交过，所以还原后即为最后一次提交的正确版本）...
echo.
git checkout -- README.md
if errorlevel 1 (
  echo [x] git checkout 失败，请把上方错误发我
) else (
  echo [OK] README.md 已还原
)
echo.
echo === git status（应只剩这 5 项改动）===
git status --short
echo.
echo 说明：
echo   M  RuoYi-Vue-master/.../application.yml        springdoc 开关（正常，要保留）
echo   M RuoYi-Vue-master/.../DatabaseMigration.java  Flyway 日志修正（正常，要保留）
echo   ?? 启动后端.cmd / 启动前端.cmd / 检查服务.cmd    新增脚本（正常，要保留）
echo.
pause
