# 校易淘前端交接工程

交接对象：后端组员。交接日期：2026-10-19。

这是可独立运行的 Vue 前端，已完成商城页面与本地交互演示，并预留真实接口调用入口。尚未完成真实登录、后端商品/订单/支付联调。收到压缩包后请先阅读 docs/后端接收指南.md。

## 五分钟启动

已验证环境：Node.js 24.21.0、npm 11.19.0。不要双击 index.html，不需要安装 Java 即可浏览前端演示。

    npm ci
    npm run dev

访问 http://localhost:5174 。Windows 也可双击 start.cmd。首次安装需要联网。

连接其他后端地址时，复制 .env.example 为 .env.local，将 DEV_API_TARGET 改成后端地址，然后重启开发服务器。它只改变代理目标，不会自动把演示页面切换为真实业务。

    DEV_API_TARGET=http://127.0.0.1:8080

联调使用相对地址 /api/v1，浏览器先访问 Vite，再转发给后端；开发阶段通常无需后端另外开放跨域。正式部署需要 Nginx 代理，样例见 deploy/nginx.conf.example。

## 验证与构建

    npm test
    npm run build
    npm run preview

preview 默认端口 4174，用于预览 dist，不提供开发代理；正式 API 联调使用 npm run dev 或配置 Nginx。构建结果在 dist。

文件职责与联调修改入口详见 [文件作用说明](docs/文件作用说明.md)。

## 代码导航

| 路径 | 作用 |
| --- | --- |
| src/App.vue | 页面模板、组件组装、交互绑定 |
| src/composables/useMarketplace.js | 本地演示状态、筛选、购物车、发布和演示订单逻辑 |
| src/api/http.js | 通用请求、超时、Token 注入、统一响应解包和错误处理 |
| src/api/index.js | 按需求文档预留的真实接口调用入口，目前尚未接到页面 |
| src/components | SVG 图标和商品插画 |
| src/data/products.js | 示例商品与分类 |
| src/utils/storage.js | 本地存储容错 |
| src/style.css | 视觉和响应式样式 |
| tests/http.test.js | 无额外测试依赖的 HTTP 层单元测试 |
| docs/接口对接说明.md | 接口清单、字段映射、待确认事项 |
| docs/联调验收清单.md | 前后端联调顺序和验收条件 |
| scripts/package.mjs | 用白名单重新生成源码交接 ZIP，排除依赖和本地配置 |

## 完成范围与限制

已完成：商品展示、筛选排序、详情、收藏、购物车、演示昵称、发布本地闲置、个人中心、演示订单和移动布局。

未完成：真实认证、学号审核、图片上传、数据库商品分页、真实订单与库存、支付退款、配送、售后以及商家/运营/财务页面。预留 API 函数不代表对应后端已经实现，也不代表已完成前后端联调。

当前没有可用的 mock/api 自动切换。请按接口说明逐项替换 useMarketplace.js 的数据来源，真实接口失败时不能退回本地“成功”结果。切换真实认证时必须替换昵称弹窗，登录成功后设置 accessToken，退出或过期时清除；当前昵称不会生成 Token。

localStorage 是演示存储，不构成身份认证或服务端权限隔离；不要填写真实隐私信息。重置演示可在浏览器开发者工具中删除 ct. 开头的键，或使用新的浏览器配置。

## 如何发给组员

Windows 下执行 npm run handoff:pack，交付包/ 目录会生成带时间戳的 ZIP 和文件校验清单。发送 ZIP 即可，不需要发送 node_modules、dist 或 .env.local。压缩包不包含后端工程。

本次实际验证见 验证记录.md；后端未实现功能不能视为验收通过。