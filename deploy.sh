#!/usr/bin/env bash
# 生产上线一键脚本（Linux 服务器，已装 Docker + docker compose）
# 用法：chmod +x deploy.sh && ./deploy.sh
set -euo pipefail
cd "$(dirname "$0")"

green() { printf '\033[32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[33m%s\033[0m\n' "$*"; }
red() { printf '\033[31m%s\033[0m\n' "$*"; }
fail() { red "[FAIL] $*"; exit 1; }

echo "==== 校易淘 · 生产上线 ===="

if [ ! -f .env ]; then
  cp .env.example .env
  yellow "已生成 .env，请先编辑填好 MYSQL_ROOT_PASSWORD / CAMPUS_JWT_SECRET / SPRING_PROFILES_ACTIVE=druid,prod 后重新运行"
  exit 0
fi

# 读取 .env（不 source，避免特殊字符被执行）
getenv() { grep -E "^$1=" .env | tail -n1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' || true; }
is_placeholder() { [ -z "$1" ] || printf '%s' "$1" | grep -Eq '^<.*>$|change-me'; }

PROFILES="$(getenv SPRING_PROFILES_ACTIVE)"
echo "$PROFILES" | grep -q prod || fail "SPRING_PROFILES_ACTIVE 必须包含 prod（建议 druid,prod）"

JWT="$(getenv CAMPUS_JWT_SECRET)"
{ is_placeholder "$JWT" || [ ${#JWT} -lt 32 ]; } && fail "CAMPUS_JWT_SECRET 必须是 >=32 位随机串（openssl rand -hex 48）"

DBP="$(getenv MYSQL_ROOT_PASSWORD)"
is_placeholder "$DBP" && fail "MYSQL_ROOT_PASSWORD 未设置"

# 管理员初始密码：为空则自动生成并回填 .env（admin 仍为默认密码时由后端应用）
ADMIN_PWD="$(getenv CAMPUS_ADMIN_PASSWORD)"
if [ -z "$ADMIN_PWD" ]; then
  ADMIN_PWD="$(LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 16)"
  printf '\nCAMPUS_ADMIN_PASSWORD=%s\n' "$ADMIN_PWD" >> .env
  green "已生成管理员初始密码并写入 .env"
elif [ ${#ADMIN_PWD} -lt 8 ]; then
  fail "CAMPUS_ADMIN_PASSWORD 少于 8 位"
fi

PAY_METHOD="$(getenv CAMPUS_PAY_METHOD)"; PAY_METHOD="${PAY_METHOD:-4}"
PAY_SECRET="$(getenv CAMPUS_PAY_SECRET)"
if [ "$PAY_METHOD" != "4" ] && is_placeholder "$PAY_SECRET"; then
  fail "已选择在线支付（CAMPUS_PAY_METHOD=$PAY_METHOD），必须配置 CAMPUS_PAY_SECRET"
fi

CORS="$(getenv CAMPUS_CORS_ORIGINS)"
if echo "$CORS" | grep -Eq 'localhost|127\.0\.0\.1|\*'; then
  fail "CAMPUS_CORS_ORIGINS 含 localhost/127.0.0.1/通配符，生产不允许"
fi
[ -z "$CORS" ] && yellow "CAMPUS_CORS_ORIGINS 为空：仅同源访问（前端经 Nginx 反代 /api 时没问题）"

if [ "$(getenv CAMPUS_SMS_ENABLED)" = "true" ]; then
  is_placeholder "$(getenv TENCENT_SMS_SECRET_ID)" && fail "开启短信但缺少 TENCENT_SMS_SECRET_ID；或先设 CAMPUS_SMS_ENABLED=false"
  is_placeholder "$(getenv TENCENT_SMS_TEMPLATE_ID)" && fail "开启短信但缺少 TENCENT_SMS_TEMPLATE_ID；或先设 CAMPUS_SMS_ENABLED=false"
  green "短信验证已启用"
else
  yellow "短信验证未启用：注册为账号密码模式"
fi

if [ "$(getenv CAMPUS_STORAGE_TYPE)" = "cos" ] && is_placeholder "$(getenv TENCENT_COS_SECRET_ID)"; then
  yellow "CAMPUS_STORAGE_TYPE=cos 但缺少 COS 凭据，将回退本地磁盘"
fi
green "生产配置前置校验通过"

command -v docker >/dev/null 2>&1 || fail "未检测到 docker"
docker compose version >/dev/null 2>&1 || fail "未检测到 docker compose 插件"

echo "== 构建并启动全部服务（首次构建可能 5-15 分钟）=="
docker compose up -d --build || fail "docker compose 启动失败"

PORT="$(getenv BACKEND_PORT)"; PORT="${PORT:-8080}"
echo "== 等待后端就绪（端口 $PORT）=="
ready=0
for i in $(seq 1 60); do
  sleep 3
  if curl -fsS -m 4 "http://127.0.0.1:$PORT/api/v1/config" >/dev/null 2>&1; then ready=1; break; fi
done
[ "$ready" = "1" ] || { yellow "后端 90 秒未就绪，执行 docker compose logs backend 查看"; exit 1; }

if [ "$(getenv CAMPUS_CLEAN_DEMO)" != "false" ]; then
  echo "== 清理演示数据 =="
  DB="$(getenv MYSQL_DATABASE)"; DB="${DB:-shop}"
  if docker compose exec -T mysql mysql -uroot "-p$DBP" "$DB" < scripts/clean-demo.sql; then
    green "演示卖家/商品已清理"
  else
    yellow "清理演示数据失败，可稍后手动执行 scripts/clean-demo.sql"
  fi
fi

FE="$(getenv FRONTEND_PORT)"; FE="${FE:-5174}"
green "==== 上线完成 ===="
echo "  本机访问： http://127.0.0.1:$FE"
echo "  公网访问： 配置域名 + HTTPS 反代到 $FE（见 docs/上线部署.md）"
echo "  支付方式： 线下面交"
echo "  管理员：   admin / $ADMIN_PWD"
echo "  关闭：     docker compose down"
