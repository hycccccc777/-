-- =====================================================================
-- MySQL 容器初始化引导脚本
-- MySQL 官方镜像只会按「文件名顺序」执行 /docker-entrypoint-initdb.d 下的脚本，
-- 而 sql/ 目录里文件名的字典序与真实执行顺序不一致（campus_* 会被排到 ry 前面），
-- 因此这里用 source 显式声明执行顺序，保证 SQL 目录仍是唯一事实来源。
-- =====================================================================
source /scripts/ry_20260417.sql;
source /scripts/quartz.sql;
source /scripts/campus_trade.sql;
source /scripts/campus_menu.sql;
source /scripts/campus_seed_products.sql;