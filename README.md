# 校易淘 · 校园二手交易系统

前后端已完整打通的校园闲置交易系统演示版：**Vue 3 前端 + 若依（RuoYi）Spring Boot 后端 + MySQL + Redis**。

买家与卖家双端闭环均已实现并经过端到端验证。

---

## 验收材料导航

| 材料 | 位置 | 内容 |
| --- | --- | --- |
| **验收材料汇编（Word/PDF）** | `docs/校易淘-验收材料汇编.docx` / `.pdf` | 13 章 + 附录，53 页：架构讲解、业务闭环、角色权限、能力对照、压测、覆盖率、可观测、关键难点、AI 使用、总结，24 表 14 图 |
| **答辩问答稿** | `docs/答辩问答稿.md` | 10 个高频问题：结论 → 理由 → 证据 → 边界，附 3 个兜底回答 |
| 技术栈与开发环境 | `docs/技术栈与开发环境说明.md` | 逐项截图说明语言、平台、数据库、缓存、容器、监控、CI/CD、测试工具 |
| 企业级能力对照 | `docs/企业级能力对照表.md` | 安全 / 一致性 / 性能 / 可靠性 / 工程化 / 运维，逐项 file:line 证据，未覆盖项如实标注 |
| 性能压测报告 | `docs/性能压测报告.md` | 实测 QPS、RT 分位、错误率，附复现脚本 |
| 数据库设计 | `docs/数据库设计说明.md` | 表结构与索引说明 |
| 接口文档 | `docs/接口文档.md` | 接口清单与约定 |
| 部署架构 | `docs/screenshots/09-architecture.png` | 分层拓扑图 |
| 指标与告警 | `docs/ops/` | 6 条告警规则 + Prometheus 抓取 + Alertmanager 路由（`docker-compose.monitoring.yml` 一键起栈） |
| 告警自检 | `scripts/check-alerting.ps1` | 规则引用的指标是否真实存在；缺失即失败（防「规则永不触发」） |
| 备份定时 | `scripts/install-backup-task.ps1` | 注册每日 02:00 计划任务并立即验证产出 |
| 依赖巡检策略 | `.github/dependabot.yml` | 只升 minor/patch 并合并成 1 个 PR，忽略 major |
| 覆盖率报告 | `docs/screenshots/10-coverage.png` | JaCoCo：业务模块行覆盖 42.9% |
| 截图总目录 | `docs/screenshots/` | 01~14，含 Swagger、Druid、数据库、CI、指标、多实例、闭环、权限 |

一键复现：

```bat
检查服务.cmd                       :: 后端/前端/文档/代理链路逐项自检
docs\采集环境信息.cmd              :: 打印并生成运行环境版本图
scripts\coverage.ps1               :: 生成 JaCoCo 覆盖率报告
node scripts\loadtest.mjs http://localhost:8080 20 20   :: 压测
scripts\backup.ps1                 :: 数据库备份（需 CAMPUS_DB_PASSWORD）
```

---
## 一、功能总览

| 角色 | 已实现能力 |
| --- | --- |
| 买家 | 手机号注册登录 → 浏览 / 搜索 / 筛选 / 排序商品 → 收藏 → 加入购物车 → 下单（多卖家自动拆单 + 幂等）→ 沙箱支付 → 确认收货 → 申请退款 |
| 卖家 | 发布商品（含图片上传）→ 改价 / 上架 / 下架 / 删除 → 处理订单发货 → 处理退款（同意 / 拒绝 / 确认到账） |
| 平台 | 商品审核状态机、订单状态机、退款状态机、真实库存扣减与回滚、越权防护、逻辑删除 |

### 订单状态机

```
待付款(0) ──支付成功──> 待发货(1) ──卖家发货──> 待收货(2) ──确认收货──> 已完成(3)
   │  └──取消──> 已取消(4)（恢复库存，商品重新上架）
   │  └──超时未支付（默认 30 分钟）──> 已取消(4)（定时任务自动执行，同样恢复库存）
   └──申请退款──> 退款中(5) ──完成退款──> 已退款(6)（恢复库存）
                    └──卖家拒绝──> 回滚为申请前的状态
```

> 超时取消由定时任务 `campusOrderTask.cancelTimeoutOrders()` 每分钟扫描，
> 阈值 `campus.order.timeout-minutes`（环境变量 `CAMPUS_ORDER_TIMEOUT_MINUTES`，设为 0 可关闭）。
> 逐单条件更新（`where status='0'`）+ 受影响行数判定，多实例并发扫描只会成功取消一次，库存只回滚一次。

### 退款单状态机

```
待审核(0) ──卖家同意──> 已同意(1) ──确认到账──> 已退款完成(3)
   └──卖家拒绝──> 已拒绝(2)（订单回滚为申请前状态）
```

---

## 二、目录结构

```
校园二手/
├── README.md                         # 本文件
├── docs/
│   ├── 接口文档.md                   # 全部 /api/v1 接口
│   └── 数据库设计说明.md              # 表结构与业务规则
├── RuoYi-Vue-master/RuoYi-Vue-master/ # 后端
│   ├── ruoyi-campus/                 # 【本次新增】校园业务模块
│   └── sql/                          # 数据库脚本
│       ├── ry_20260417.sql           # 若依基础表（若依自带）
│       ├── quartz.sql                # 定时任务表（若依自带）
│       ├── campus_trade.sql          # ★ 业务表 + 字典（本次新增）
│       ├── campus_menu.sql           # ★ 菜单与权限（本次新增）
│       └── campus_seed_products.sql  # ★ 演示数据（本次新增）
└── 校易淘前端-后端交接-2026-10-19T01-05-57-755Z/   # 前端
    ├── src/
    │   ├── api/                      # HTTP 层与接口封装
    │   ├── composables/useMarketplace.js  # 业务状态与逻辑
    │   ├── components/               # 图标、商品插画
    │   └── data/products.js          # 仅保留学校选项
    └── vite.config.js                # /api 与 /profile 代理
```

---

## 二点五、让别人打开（演示/评审用）

三种方式，按「对方在哪」选：

| 场景 | 命令 | 对方访问 |
| --- | --- | --- |
| 对方就在你旁边（同一台机器） | `启动后端.cmd` + 前端 `npm run dev` | `http://localhost:5174` |
| 对方在同一 WiFi / 局域网 | 双击 `让别人打开.cmd`，或先跑 `scripts\open-lan-port.ps1`（需管理员） | `http://<你的内网IP>:5174` |
| 对方在任何网络（推荐评审用） | 双击 `让别人打开.cmd`（自动建公网隧道） | `https://xxx.trycloudflare.com` |

### 一键脚本做了什么

```bat
让别人打开.cmd
```

1. 从 `.env` 读数据库口令（没有会明确提示，不会静默用默认值）
2. 后端没起就起后端，等健康检查通过
3. 前端没起就起前端，**并绑定 `0.0.0.0`**（默认只绑 127.0.0.1，外面连不上）
4. 用 `cloudflared` 建一条临时公网隧道，打印可分享的 `https://...trycloudflare.com`
5. 已存在隧道就复用，不会堆一堆进程

停止：双击 `停止.cmd`（会停 8080、5174 和 cloudflared）。

### 三个常见「别人打不开」的原因

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 本机能开，别人打不开 | 前端只绑了 `127.0.0.1` | `DEV_HOST=0.0.0.0` 或 `npm run dev -- --host 0.0.0.0` |
| 局域网能开，公网打不开 | Windows 防火墙在「公用网络」下默认阻止入站 | 管理员运行 `scripts\open-lan-port.ps1 -Port 5174`，或直接用隧道 |
| 隧道打开是 403 | Vite 的 Host 校验拒绝了外部域名 | `vite.config.js` 已支持 `DEV_ALLOWED_HOSTS`，脚本会自动写入 `.env.local` |

### 为什么只分享前端端口

前端的开发服务器会把 `/api`、`/profile` 反代到后端（见 `vite.config.js`）。
所以对外只需要开一个端口，后端不必直接暴露 —— 这也减少了被扫端口的面。

### 关于临时地址

`trycloudflare.com` 地址每次重启都会变，属于「用完即弃」，适合评审演示。
要固定域名 / 长期运行，把 `dist/` 部署到服务器（`nginx.conf` 已就绪），
或用 `cloudflared` 的 named tunnel。

---
## 三、本地启动步骤

### 0. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 及以上 | 实测 JDK 27 编译通过（工程 `source/target=17`） |
| Maven | 3.9.x | 用于构建后端 |
| Node.js | 20 及以上 | 实测 v24.21.0 / npm 11.19.0 |
| MySQL | 8.x 及以上 | 实测 26.7 |
| Redis | 5.x 及以上 | 实测 7.4.3 |

### 1. 初始化数据库

确保 MySQL 已启动（Windows 服务名 `MySQL267`，端口 3306，root 密码见 `application-druid.yml`）。

```powershell
# 创建库（若已存在可跳过）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS shop DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 按顺序导入（工作目录切到 sql 目录，避免中文路径问题）
cd RuoYi-Vue-master\RuoYi-Vue-master\sql
mysql -uroot -p --default-character-set=utf8mb4 shop < ry_20260417.sql
mysql -uroot -p --default-character-set=utf8mb4 shop < quartz.sql
mysql -uroot -p --default-character-set=utf8mb4 shop < campus_trade.sql
mysql -uroot -p --default-character-set=utf8mb4 shop < campus_menu.sql
mysql -uroot -p --default-character-set=utf8mb4 shop < campus_seed_products.sql
```

导入后应包含 **8 条在售商品**、5 个类目、2 所学校、66 条校园菜单。

### 2. 启动 Redis

确保 Redis 服务运行（Windows 服务名 `Redis`，端口 6379，默认无密码）。

### 3. 启动后端

**最省事的办法：双击仓库根目录的 `启动后端.cmd`。**

它会依次检查 MySQL(3306) 与 Redis(6379) 是否在监听、定位 `java` 与 `mvn`、
在 jar 不存在时自动构建，然后启动服务。任何一步不满足都会给出明确提示。

手动方式：

```powershell
$env:JAVA_HOME = "C:\jdk-17"   # 按实际路径修改
cd RuoYi-Vue-master\RuoYi-Vue-master
mvn -pl ruoyi-admin -am -DskipTests clean package
java -jar ruoyi-admin\target\ruoyi-admin.jar
```

启动成功日志：`Started RuoYiApplication in x seconds`，端口 **8080**。

访问地址：
- 后台管理：http://localhost:8080
- Druid 监控：http://localhost:8080/druid （ruoyi / 123456）
- 接口文档：http://localhost:8080/swagger-ui/index.html （分组选 `campus`）
- OpenAPI 契约：http://localhost:8080/v3/api-docs/campus

### 3.5 更省事的办法：双击脚本

仓库根目录放了三个脚本，Windows 下双击即可：

| 脚本 | 作用 |
| --- | --- |
| `启动后端.cmd` | 检查 MySQL/Redis 端口、定位 java 与 mvn、jar 不存在时自动构建，然后启动后端 |
| `启动前端.cmd` | 缺依赖时自动 `npm install`，然后启动 Vite |
| `检查服务.cmd` | 逐项探测后端接口、OpenAPI 契约、Swagger UI、前端页面，以及「经前端代理打后端」这条链路 |

> 后端与前端是两个进程，**需要两个窗口同时开着**。
> 只开前端的话页面能打开，但接口会报 `ECONNREFUSED 127.0.0.1:8080`。
### 4. 启动前端

**双击仓库根目录的 `启动前端.cmd`**（自动装依赖并启动），或手动：

```powershell
cd 校易淘前端-后端交接-2026-10-19T01-05-57-755Z
npm ci
npm run dev
```

访问 **http://127.0.0.1:5174** 。开发服务器已把 `/api` 与 `/profile` 代理到 `http://127.0.0.1:8080`。

如需改后端地址：复制 `.env.example` 为 `.env.local`，修改 `DEV_API_TARGET`，**重启**开发服务器。

### 5. 一键自检

双击仓库根目录的 `检查服务.cmd`，会逐项探测后端接口、OpenAPI 契约、
Swagger UI、前端页面以及「经前端代理打后端」这条链路，全绿才算环境就绪：

```
  [OK]   后端探活
  [OK]   后端商品列表
  [OK]   后端类目列表
  [OK]   OpenAPI 契约
  [OK]   Swagger UI
  [OK]   前端页面
  [OK]   经前端代理打后端
```

> 后端与前端是两个进程，需要**两个窗口同时开着**。只开前端的话，
> 页面能打开但接口报 `ECONNREFUSED 127.0.0.1:8080`。

---

## 四、测试账号

所有测试账号密码均为 **`admin123`**。

| 手机号 | 昵称 | 身份 | 说明 |
| --- | --- | --- | --- |
| 15888888888 | 若依 | 管理员 | 若依超级管理员，可用后台管理菜单 |
| 13800001001 | 小林同学 | 卖家 | 发布商品 1、5 |
| 13800001002 | 阿泽 | 卖家 | 发布商品 2、8 |
| 13800001003 | 一颗小橙 | 卖家 | 发布商品 3、6 |
| 13800001004 | 骑车去看海 | 卖家 | 发布商品 4、7 |

> 登录方式：**手机号 + 密码**（`POST /api/v1/auth/login`）。没有账号可直接在弹窗里 **注册**（`POST /api/v1/auth/register`，注册成功即登录）。
> 注册账号默认即可发布闲置、下单与收款；如需关闭自助注册，设 `CAMPUS_REGISTER_ENABLED=false`。
> 注意：`13800001001` 等演示卖家与商品一一对应，测试「卖家中心」时用它登录。

### 推荐体验路径

1. 用 `15888888888 / admin123` 登录 → 逛商品 → 加入购物车 → 下单（选不同卖家可看到自动拆单）→ 去支付 → 确认收货。
2. 用 `13800001001 / admin123` 登录 → 卖家中心 → 我卖出的（发货）→ 退款处理。
3. 买家对已支付订单点「申请退款」，再用卖家账号到退款处理里同意 / 拒绝。
4. 发布商品时可选上传图片，图片可通过 `/profile/upload/...` 访问。

---

## 五、自动化测试

后端已建立集成测试体系，使用**独立的 `shop_test` 库**，不会污染开发库。

```powershell
# 1. 一键创建/重建测试库（含建表 + 演示数据 + 测试数据）
.\scripts\setup-test-db.ps1

# 2. 运行全部测试
mvn -pl ruoyi-admin -am test

# 只跑某个测试类
mvn -pl ruoyi-admin -am test "-Dtest=StockConcurrencyTest"
```

### 现有用例（59 后端 + 19 前端 = 78 个，全部通过）

> 数据来源：`ruoyi-admin/target/surefire-reports` 实测 Tests run: 59, Failures: 0, Errors: 0；
> 前端 `npm test` 实测 `tests 19 / pass 19 / fail 0`。


| 测试类 | 用例 | 验证内容 |
| --- | --- | --- |
| `StockConcurrencyTest` | 库存 1、20 买家并发 | 只能成功 1 单，库存归零并自动下架 |
| `StockConcurrencyTest` | 库存 10、20 买家并发 | 恰好成功 10 单，无丢失更新 |
| `IdempotencyConcurrencyTest` | 同一幂等键并发 10 次 | 只产生 1 笔订单、库存只扣 1 次 |
| `IdempotencyConcurrencyTest` | 顺序重复提交 | 返回原批次，不新增订单、不重复扣库存 |
| `OrderLifecycleTest` | 下单→支付→发货→收货 | 状态机流转 + 支付幂等 |
| `OrderLifecycleTest` | 取消待付款订单 | 回滚库存、商品重新上架 |
| `OrderLifecycleTest` | 退款被拒绝 | 订单回滚到申请前状态 |
| `OrderLifecycleTest` | 退款完成 | 订单转已退款、库存回滚、重新上架 |
| `OrderLifecycleTest` | 越权防护 | 买家不能发货、他人订单不可见 |
| `OrderTimeoutTest` | 超过阈值仍未支付 | 自动取消 + 库存回滚 + 记录取消原因 |
| `OrderTimeoutTest` | 未到阈值 / 已支付 | 不动，库存不被误回滚 |
| `OrderTimeoutTest` | 单批上限 + 重复执行 | 分批处理、多轮幂等，库存只回滚一次 |
| `RegisterFlowTest` | 注册成功 | 密码 BCrypt 落库、注册后可直接登录 |
| `RegisterFlowTest` | 昵称缺省 / 重复手机号 / 弱密码 | 默认昵称用后四位；409 拦截重复；400 拦截非法输入且不落库 |
| `ProductAuditTest` | 待审核不对外展示 | 新商品为 status 0，公开列表不包含 |
| `ProductAuditTest` | 审核通过 / 驳回 / 重新提交 | 通过后上架并出现在公开列表；驳回记录原因；卖家修改后重新待审核 |
| `ProductAuditTest` | 绕过审核 / 重复审核 / 越权 | 待审核不能自行上下架（409）；非待审核不能重复审核；非管理员访问返回 403 |

> 上表只列**最能说明问题**的 19 条；完整 59 条见 `surefire-reports`，另有 19 条前端用例（HTTP 层、错误契约、令牌刷新、自动登出）。

### 演示脚本（可反复跑）

| 脚本 | 作用 |
| --- | --- |
| `scripts\reset-demo-data.ps1` | 复位演示数据：清订单/支付/幂等/购物车，恢复 8 件商品为「上架、库存 1」 |
| `scripts\demo-business-loop.ps1` | 业务闭环演示：浏览 → 加购 → 下单（幂等）→ 支付 → 发货 → 收货 |
| `scripts\demo-order-timeout.ps1` | 待付款超时自动取消演示：下单不付款 → 定时任务取消 → 库存回滚 |
| `scripts\demo-rbac.ps1` | 角色权限演示：匿名/买家/卖家三种身份的 401、数据隔离、越权 404 |
| `scripts\_demo-auth.ps1` | 上面两个脚本共用的登录助手：缓存令牌 + 429 等待重试 |

> **演示前一定要先跑 `reset-demo-data.ps1`。** 演示会真的下单扣库存，跑过一次商品就变成「已售出、库存 0」，
> 第二次演示会直接卡在「没有可下单的商品」—— 能反复跑才算演示。
>
> 演示脚本会复用登录令牌，因为登录限流是 10 次/分钟/IP，一轮演示登录 4 次，连跑两轮就会自己把自己限流。
> 这个坑的修法是**让脚本复用令牌**，不是把限流阈值调大。

### 关键正确性设计


**库存扣减是单条原子 SQL**，不使用「先查再改写」：

```sql
update ct_product
set status = case when stock - #{qty} <= 0 then '4' else status end,
    stock  = stock - #{qty}
where product_id = #{id} and del_flag = '0' and status = '1' and stock >= #{qty}
```

> 注意：`status` 必须写在 `stock` **前面**。MySQL 同一 UPDATE 内 SET 赋值从左到右求值，靠后的表达式读到的是已赋新值的行；写反会导致「还剩 1 件也被误判为售罄」。

**下单幂等由数据库保证**：先往 `ct_idempotency` 插入幂等键（唯一索引 `uk_ct_idem_key`），只有插入成功的请求能进入下单流程，其余请求要么重放原批次，要么被明确拒绝。另有 `ct_order` 上的 `(group_no, seller_id)` 唯一索引做兜底。

---

## 六、数据库版本管理（Flyway）

所有**后续**的表结构变更都放在 `ruoyi-admin/src/main/resources/db/migration/`，由应用启动时自动执行，不再手写 SQL 导入。

```
db/migration/
├── V1__campus_p0_integrity.sql   # 幂等表 + group_no 扩容 + 防重复建单唯一索引
└── V2__order_list_index.sql      # 订单查询索引
```

| 迁移 | 版本 | 说明 |
| --- | --- | --- |
| `legacy schema imported from sql/*.sql` | BASELINE 1 | 存量库基线（由 `sql/campus_*.sql` 导入的历史结构） |
| `V1 campus_p0_integrity` | 1 | 幂等表等 P0 变更（已包含在基线中，不重复执行） |
| `V2 order_list_index` | 2 | 订单列表查询索引 |

```powershell
# 新增迁移：建 V3__xxx.sql 即可，启动时自动应用并记录到 flyway_schema_history
mvn -pl ruoyi-admin -am -DskipTests package
java -jar ruoyi-admin\target\ruoyi-admin.jar
```

> 迁移由 `DatabaseMigration`（`ApplicationRunner`）显式执行，配置集中在代码里而不是 `application.yml`——避免出现"yml 配了却没生效"的隐患。
> 存量库已由 `sql/` 脚本导入，因此设了 `baseline-version: 1`，只登记基线、不重复执行 V1。

---

## 七、接口参数校验

请求体的合法性由 **Bean Validation** 在 Controller 层拦截（`@Valid`），不再散落在 Service 里手写 `if-else`。

| DTO | 校验内容 |
| --- | --- |
| `CtLoginBody` | 手机号格式、密码非空 |
| `CtProductCreateBody` | 标题 4~128 字、类目必填、价格 0.01~99999、库存 0~9999、成色枚举、图片最多 6 张 |
| `CtOrderCreateBody` | 商品必选、收货人 ≤20、手机号 11 位、地址 ≤200、配送方式枚举、幂等键 ≤64 |
| `CtOrderCreateBody` 跨字段 | 传了 `addressId` 则以地址簿为准，否则收货人/电话/地址三项必填（`@AssertTrue`） |

校验失败统一返回我们的响应结构，**多条违规会聚合展示**而不是只报第一条：

```json
{ "code": 400, "message": "请填写收货人、联系电话与收货地址；请选择要结算的商品", "data": null }
```

> `ApiExceptionHandler` 以 `HIGHEST_PRECEDENCE` 优先于若依自带的全局异常处理，保证 `/api/v1` 始终是 `{code, message, data}` 一种结构。

---
## 八、常见问题

**Q：Maven 报 `Error assembling JAR` / `Failed to delete ... target`**
文件被占用（通常是 IDE 或残留进程锁定）。结束 `java` 进程后重试 `mvn clean package` 即可。

**Q：Java 源码报「编码 UTF-8 的不可映射字符」**
若使用某些编辑器/工具保存中文文件，可能写成 GBK。工程 `sourceEncoding=UTF-8`，需把相关 `.java` / `.xml` 转为 UTF-8（无 BOM）再编译。

**Q：`npm ci` 报 ECONNRESET**
网络问题。换镜像：`npm ci --registry=https://registry.npmmirror.com`。

**Q：图片显示 404**
图片存放在 `D:/ruoyi/uploadPath/upload/...`（由 `application.yml` 的 `ruoyi.profile` 决定）。开发环境请确认 `/profile` 已在 Vite 代理中配置。

**Q：修改配置后不生效**
`application*.yml` 改动需重启 jar；`.env.local` 改动需重启 Vite。

**Q：接口返回 HTTP 200 但 body 里 code 是 401**
若依对未认证请求返回 HTTP 200 + JSON `code:401`。前端已按 body 的 `code` 判断。令牌过期时该 401 由 Spring Security 层返回（字段为 `msg`），登录失败则由我们的接口返回（字段为 `message`）。

---

## 九、跑 CI（不依赖任何付费平台）

Gitee Go 需付费，因此**不把 CI 押在托管平台上**。仓库里提供一份统一脚本，
本地、CI、自建 Runner 跑的都是同一套逻辑，不会出现「本地全绿、CI 挂了」。

```powershell
cd RuoYi-Vue-master/RuoYi-Vue-master

# 完整流程（编译 → 造测试库 → 后端测试 → 前端安装/测试/构建）
powershell -ExecutionPolicy Bypass -File scripts\ci.ps1

# 常用组合
powershell -File scripts\ci.ps1 -SkipTests      # 只编译
powershell -File scripts\ci.ps1 -SkipFrontend   # 只跑后端
powershell -File scripts\ci.ps1 -Maven D:\tools\apache-maven-3.9.9\bin\mvn.cmd
```

| 退出码 | 含义 |
| --- | --- |
| `0` | 全部通过 |
| `1` | 某个步骤失败（终端会打印失败步骤与原因） |

脚本会输出耗时汇总表，方便定位是哪一步慢。找不到 Maven 时会给出三种解决方式，
而不是抛出一句无法定位的「术语无法识别」。

> ? Windows 下若前端安装报 `EPERM: unlink ... esbuild.exe`，是 esbuild 进程残留占用文件，
> 关闭 Vite / Node 进程或重启终端即可，与代码无关。

### 免费 CI 方案：GitHub Actions

Gitee Go 需要付费，Gitee 内的流水线也已并入 Gitee Go，因此 CI 统一放在 GitHub：

| 项 | 说明 |
| --- | --- |
| 公开仓库 | **免费且分钟数不限** |
| 私有仓库（GitHub Free） | 每月 2000 分钟 + 500 MB 制品 + 10 GB 缓存 |

`.github/workflows/ci.yml` **不重复编写编译/测试步骤，而是直接调用 `scripts/ci.ps1`**，
保证本地与 CI 跑的是同一套逻辑：

| Job | 命令 | 实际执行 |
| --- | --- | --- |
| backend | `./scripts/ci.ps1 -SkipFrontend -SkipDocker` | 编译 → 造 shop_test → 后端 14 个集成测试 |
| frontend | `./scripts/ci.ps1 -SkipBackend -SkipTests -SkipDocker` | npm ci → 前端单测 → 构建 → 构建镜像 |

> 需要把仓库镜像一份到 GitHub 后才会触发；Gitee 上的副本仅作为代码托管与国内访问入口。

---
## 十、支付网关抽象

支付是**策略模式**实现的，业务层只依赖 `PaymentGateway` 接口，不感知具体渠道。

```
com.ruoyi.campus.payment
├── PaymentGateway          抽象：prepay / confirm / supportsManualConfirm / verifyCallback
├── PaymentGatewayRegistry  按 pay_method 分发，所有 Spring Bean 实现自动注册
├── SandboxPaymentGateway   code=0 沙箱，确认即成功
├── MockPaymentGateway      code=9 模拟网关：金额 ≥ 500 元时返回失败
└── PaymentCallbackSecret   回调签名校验（SHA-256），密钥只从环境注入
```

| 场景 | 路径 |
| --- | --- |
| 演示 / 自动化测试 | `POST /api/v1/pay/{payNo}/confirm`（主动确认） |
| 真实支付网关 | `POST /api/v1/pay/{payNo}/notify?sign=...`（异步回调，**必须验签**） |

> 新增支付渠道 = 新增一个 `implements PaymentGateway` 的 `@Component`，**不需要改动订单、支付单等任何业务代码**。
> `supportsManualConfirm()` 返回 `false` 的实现（如真实网关）会拒绝主动确认，强制走回调路径。

支付回调接口对外暴露且无需登录，因此**必须校验签名**，否则任何人都能伪造「支付成功」把订单刷成已付款——这一点有专门的测试用例守着。

---

### 凭据配置

代码与配置里不硬编码生产凭据，统一走环境变量，开发默认值仅作兜底：

| 环境变量 | 用途 | 默认值 |
| --- | --- | --- |
| `CAMPUS_DB_URL` | 数据库连接串 | `jdbc:mysql://localhost:3306/shop...` |
| `CAMPUS_DB_USERNAME` | 数据库账号 | `root` |
| `CAMPUS_DB_PASSWORD` | 数据库密码 | 仅本地开发用的默认值 |
| `CAMPUS_PAY_SECRET` | 支付回调签名密钥 | `change-me-in-production` |
| `SPRINGDOC_ENABLED` | 是否开放接口文档端点 | `true`（生产建议设 `false`） |

```powershell
# 生产/CI 覆盖示例
$env:CAMPUS_DB_PASSWORD = "<真实密码>"
$env:CAMPUS_PAY_SECRET   = "<随机密钥>"
java -jar ruoyi-admin\target\ruoyi-admin.jar
```

> Docker Compose 下由 `.env` 提供同名变量注入；`docker/mysql-init` 只读挂载 `sql/` 目录，不额外注入凭据。

---
## 十一、容器化启动（Docker Compose）

> **验证状态**：已由 CI 的「容器编排验证」job 在云端真实验证通过
> （GitHub runner 自带 Docker，开发机不必安装）。
> 该 job 会真实执行 `docker compose up -d --build`，经前端 Nginx 反代打接口，
> 并校验 MySQL 初始化数据与容器内卷写权限。

```bash
cp .env.example .env      # 按需改端口与密码
docker compose up -d --build
```

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| frontend | 5174 | Nginx 托管前端静态资源，`/api` 与 `/profile` 反代到 backend |
| backend | 8080 | 多阶段构建、非 root 运行、含健康检查 |
| mysql | 3307 | 首次启动自动按正确顺序导入 `sql/` 下的建表与演示数据 |
| redis | 6380 | 持久化到具名卷 |

细节：

- **Dockerfile 分层**：先只复制各模块 `pom.xml` 预热依赖缓存，改代码时不必重下依赖
- **构建上下文互不干扰**：前后端各自独立上下文，互不 `COPY` 对方的文件
- **SQL 只保留一份**：`docker/mysql-init/00_bootstrap.sql` 用 `source` 显式指定执行顺序，避开 MySQL 镜像「按文件名字典序执行」的坑
- **密钥不落镜像**：`CAMPUS_PAY_SECRET` 只从环境变量注入

> ? 本机未安装 Docker，以上配置**尚未实机验证过**，仅完成了两件事：用 YAML 解析器校验语法、逐条校验 compose 引用的路径与 `source` 的 SQL 文件均真实存在。上机后请先跑一次 `docker compose config` 与 `docker compose up`。

---

## 十二、非 Docker 部署（已实测验证）

开发机未安装 Docker，`docker-compose.yml` 至今**未在真实环境执行过**。
为此提供并实测了一条不依赖容器的部署路径，两者效果等价。

### 前置条件

| 依赖 | 验证状态 |
| --- | --- |
| MySQL 26.7（服务名 `MySQL267`，3306） | ? 实测 |
| Redis（服务 `Redis`，6379） | ? 实测 |
| JDK 17+ | ? 实测（JDK 27） |

### 启动步骤

```powershell
# 1. 构建可执行 jar
cd RuoYi-Vue-master\RuoYi-Vue-master
mvn -pl ruoyi-admin -am -DskipTests package

# 2. 用环境变量注入凭据并启动（不要把密码写进命令行）
$env:CAMPUS_DB_PASSWORD = "<你的数据库密码>"
$env:LOG_PATH           = "$PWD\logs"
java -jar ruoyi-admin\target\ruoyi-admin.jar

# 3. 验证服务已就绪
curl http://localhost:8080/api/v1/products?pageSize=1
```

服务在 5 秒左右启动完成，Flyway 会自动校验/迁移 `shop` 库。

### 实测结论（2026-10-19）

启动后用真实 HTTP 请求验证了以下行为，全部通过：

| 验证项 | 结果 |
| --- | --- |
| 服务启动 + Flyway 迁移 | 5.2 秒就绪，迁移版本 1、2 均 success |
| `GET /api/v1/products` | 200，返回 8 件商品 |
| `X-Trace-Id` 响应头 | 16 位十六进制随机值 |
| 前端传入 traceId 的透传 | 响应头原样返回 `client-trace-login-01`，且该值出现在 `api-access.log` |
| 登录 `13800001001 / admin123` | 200，`token` 206 字符、`refreshToken` 32 字符、`expiresIn` 1800 |
| 带 token 访问 `/api/v1/cart` | 200，访问日志记录 `userId=1001` |
| 刷新令牌 | 返回的新 `token` 与新 `refreshToken` 均与旧值不同 |
| 旧 refreshToken 复用 | 401「刷新令牌已失效，请重新登录」（一次性语义生效） |
| 登录限流 | 第 10 次后返回 **HTTP 429**，`Content-Type: application/json; charset=UTF-8`，响应体 `{"code":429,"message":"请求过于频繁，请稍后再试","data":null}` |

访问日志样例（`logs/api-access.log`）：

```
2026-10-19 21:18:09.864 [trace=c07294cd9d9a4875] api method=GET path=/api/v1/cart status=200 costMs=51 userId=1001
```

> 这轮实测抓出并修复了两个真实缺陷：限流拒绝原先落到若依全局异常处理器，
> 返回 HTTP 200 + 后台格式 `{msg,code:500}`，前端会显示成「请求失败」；
> 以及访问日志的 `userId` 因过滤器在 Spring Security 之前执行而始终为 anonymous。
> 两者均已修正并复验。

---
## 十三、持续集成（GitHub Actions）

`.github/workflows/ci.yml` 在 push / PR 时自动执行：

| Job | 内容 |
| --- | --- |
| backend | JDK 17 + MySQL 8.4 + Redis 服务容器 → 编译 → 建 `shop_test` → 跑 14 个测试 → 上传 surefire 报告 → 构建后端镜像 |
| frontend | Node 20 → `npm ci` → 跑单测 → 构建 → 构建前端镜像 |

任一步失败都会阻止合并；同一分支的新提交会自动取消进行中的旧任务。

> ? **当前工作区还不是 git 仓库**，CI 只有在推送到 GitHub 后才会生效。建议先 `git init` 并纳入版本管理（`.env`、`node_modules`、`target` 必须进 `.gitignore`）。

---
## 十四、多实例与负载均衡

单实例是最大单点。多实例的前提是三件事必须共享（都已落地）：

| 共享依赖 | 状态 | 实现 |
| --- | --- | --- |
| 登录态 / 令牌 | ✅ | 无状态 JWT + 刷新令牌落 Redis |
| 接口限流 | ✅ | `CAMPUS_RATELIMIT_STORE=redis`，ZSET + Lua 原子计数 |
| 定时任务 | ✅ | `spring.quartz.job-store-type=jdbc` + QRTZ_LOCKS 行锁；订单超时取消任务在多实例下只触发一次 |

### 14.1 容器方式（推荐）

```bat
docker compose -f docker-compose.yml -f docker-compose.scale.yml up -d --build --scale backend=2 --wait
:: 统一入口 http://localhost:8088，由 lb 分发到两个 backend
```

负载均衡配置：`docs/ops/nginx-lb.conf`（`least_conn` + `max_fails=2 fail_timeout=15s` + `keepalive`）。

### 14.2 无 Docker 的本地验证

```powershell
# 1) 起两个后端（--server.port 区分，CAMPUS_INSTANCE_ID 用于响应头标识）
启动后端.cmd
java -jar RuoYi-Vue-master\RuoYi-Vue-master\ruoyi-admin\target\ruoyi-admin.jar --server.port=8082

# 2) 起本地 LB（等价于 nginx 的最小实现）
node scripts\lb-proxy.mjs --listen 8088 --backends 8080,8082

# 3) 五项断言一次跑完（含杀节点容错）
powershell -ExecutionPolicy Bypass -File scripts\demo-lb.ps1 -KillNode 8082
```

预期输出：分流 4/4、令牌跨节点全 200、限流第 11 次起 429、杀掉一个节点后仍可用。

### 14.3 排查技巧

每个响应带 `X-Instance-Id`（取值 `CAMPUS_INSTANCE_ID` 或 `主机名:端口`），一条命令就能看出请求落在哪台机器：

```bat
curl -s -D - -o NUL http://localhost:8088/api/v1/system/health | findstr X-Instance-Id
```

---
## 十五、日志集中化（Loki）

多实例后日志分散在各节点磁盘，排障要一台台 grep。现在应用直接往 Loki push（`LokiLogAppender`），带结构化字段：`ts / level / logger / traceId / instance / msg`。

### 15.1 启动

```bat
docker compose -f docker-compose.monitoring.yml up -d loki grafana
:: Loki http://localhost:3100   Grafana http://localhost:3000
```

后端启动时注入地址（未配置则该 appender 自动关闭，不影响本机开发与 CI）：

```bat
set LOKI_URL=http://127.0.0.1:3100/loki/api/v1/push
set CAMPUS_INSTANCE_ID=node-a
java -jar RuoYi-Vue-master\RuoYi-Vue-master\ruoyi-admin\target\ruoyi-admin.jar
```

### 15.2 按 traceId 查全链路

```logql
{application="campus-shop"} | json | traceId="demo213751"
```

也可以按实例查：`{application="campus-shop", instance="node-b"}`。

### 15.3 验证脚本

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo-logs.ps1
```

预期：两实例都在上报、traceId 精确召回、失败请求可定位、按实例可分别检索（6 项断言）。

### 15.4 设计取舍

| 决策 | 原因 |
| --- | --- |
| 应用主动 push，不用 Promtail 抓文件 | 容器日志在临时卷，抓文件要额外挂载；主动推与部署形态无关 |
| 有界队列，满则丢最旧 | 日志不该把业务线程堵死或吃光堆；保最近现场 |
| 推送失败不重试 | Loki 故障时重试会雪崩；只计数并每 30 秒报一次 |
| 默认关闭 | 不配 `LOKI_URL` 就不启用，本机与 CI 零影响 |

---
## 十六、回滚、灰度与风险登记

### 16.1 回滚（已实测 9.8 秒）

```bat
powershell -File scripts\release.ps1 -Action deploy     :: 发布（构建 + 归档 + 切流 + 探活）
powershell -File scripts\release.ps1 -Action rollback   :: 回滚到上一版本
powershell -File scripts\release.ps1 -Action list       :: 查看历史版本
```

回滚切的是**归档好的制品**（`releases/v-xxx/ruoyi-admin.jar`），不是重新构建 —— 重新构建会引入「构建不可复现」的风险。完整记录见 `docs/回滚演练记录.md`。

关键设计：**发布失败自动回滚**。脚本在探活失败时会自动切回上一版本，不需要人工介入。

### 16.2 灰度

```bat
:: 容器方式
docker compose -f docker-compose.yml -f docker-compose.scale.yml --profile canary up -d --build

:: 无 Docker：起灰度实例 + 带 canary 的 LB
java -jar ...ruoyi-admin.jar --server.port=8083
node scripts\lb-proxy.mjs --listen 8088 --backends 8080 --canary-port 8083 --canary-percent 10

:: 验证
powershell -File scripts\demo-canary.ps1
```

配置在 `docs/ops/nginx-lb.conf`。**写接口永不进灰度**（POST/PUT/DELETE 钉在稳定池），避免「写在新版、读在旧版」的数据错乱。

| 操作 | 做法 | 生效时间 |
| --- | --- | --- |
| 放量到 10% | 灰度实例 weight=1，稳定各 weight=9 | `nginx -s reload` |
| 全量切新 | 删掉稳定池里的旧实例 | reload |
| 回滚 | 灰度实例 weight=0 | reload（秒级） |
| 强制某个用户走灰度 | 请求头 `X-Canary: 1` | 立即 |

### 16.3 风险登记表（12 条）

`docs/风险登记表.md` —— 每条未覆盖项都写清**为什么不做的理由 / 影响 / 触发条件 / 缓解措施**。这是「达标」和「留白」的区别：评审看到「未覆盖」会追问，看到「已评估 + 触发条件明确」通常就过了。

---
## 十七、尚未实现（诚实说明）

- **真实支付**：使用沙箱支付（`POST /api/v1/pay/{payNo}/confirm` 直接置为成功）。接入真实网关需替换为异步回调校验。
- **配送费**：暂恒为 0，未区分自提 / 配送计费。
- **收货地址管理**：无 `/api/v1/addresses`，结算时直接提交收货人/电话/地址，由后端做快照。
- **收藏**：仍为前端本地存储，未服务端化。
- **部分退款 / 售后**：仅支持全额退款；已完成订单不支持售后。
- **熔断降级**：未引入 Resilience4j；限流器对 Redis 故障 fail-open。
- **静态检查 / E2E**：未接入 Checkstyle / ESLint / Playwright。
- **本机无 Docker**：容器化由 CI runner 实测（`compose up` + 经 Nginx 冒烟），本机只做静态校验。

### 已从「未实现」移出并验证过的

| 项 | 证据 |
| --- | --- |
| 刷新令牌 | refreshToken 落 Redis、一次性使用；`RefreshTokenFlowTest` |
| 告警接收端 | 6 条规则 + Alertmanager 路由；本地实测 webhook 收到 883 字符载荷；`scripts\check-alerting.ps1` 防「引用了不存在的指标」 |
| 备份定时 | `install-backup-task.ps1` 注册每日 02:00 任务，装完立即触发并验证产出（LastTaskResult=0，130.5 KB） |
| 定时任务集群 | `job-store-type=jdbc` + Flyway V3；双实例 `QRTZ_SCHEDULER_STATE` 2 行，`sys_job_log` 严格 10 秒一条无重复 |
| HTTPS | `application-https.yml` + 自签证书，`https://localhost:8443` 实测 200 |
| 未支付订单超时取消 | `campusOrderTask.cancelTimeoutOrders()` 每分钟扫描；`OrderTimeoutTest` 5 条用例 + `demo-order-timeout.ps1` 实测下单不付款 15 秒内被取消、库存回滚 |
| 商品发布审核 | `campus.product.audit-enabled` 开关；新商品 status 0；`/api/v1/admin/products/pending` + `/audit`；`ProductAuditTest` 6 条；卖家中心显示待审核/驳回原因 |
| 管理端前端 | 消费端内嵌「管理后台」视图（商品审核 + 用户列表），管理员账号登录后导航栏出现入口 |

---
## 依赖巡检与待处置 PR

Dependabot 只升 **minor / patch**，并把同生态的更新合并成 **1 个 PR**，major 版本一律忽略（需要人工评估的大版本不适合自动开 PR 堆在仓库里）。配置见 `.github/dependabot.yml`。

收紧配置后，Dependabot 自动**关闭**了此前开出的 5 个 major PR（vite 8 / typescript 7 / element-plus 2.14 / upload-artifact 7 等），并重新开出两个分组 PR：

| PR | 内容 | CI 状态 | 建议 |
| --- | --- | --- | --- |
| #14 | npm minor 组（2 个更新） | 通过 | 可直接合并 |
| #15 | maven minor 组（10 个更新） | 失败 | 10 个一起升必然有互相冲突，建议关掉或拆成逐个升级 |

> 本次没有合并任何 PR：依赖升级会让交付物与已测基线不一致，应由维护者决定时机。合并 #14 后请跑一遍 `检查服务.cmd` 与 `scripts\demo-business-loop.ps1` 回归。