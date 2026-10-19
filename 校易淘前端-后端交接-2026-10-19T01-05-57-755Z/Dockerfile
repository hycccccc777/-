# syntax=docker/dockerfile:1
# 校园二手交易系统 - 前端镜像（Vite 构建 + Nginx 托管 + 反向代理）

FROM node:20-alpine AS build
WORKDIR /app

# 只复制清单文件，让 npm ci 这一层能被缓存
COPY package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com

COPY . .
# .env.local 会覆盖后端地址；容器内统一走 Nginx 代理，故先移除再构建
RUN rm -f .env.local && npm run build

FROM nginx:1.27-alpine AS runtime
RUN apk add --no-cache tzdata \
 && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
 && echo "Asia/Shanghai" > /etc/timezone

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD nc -z 127.0.0.1 80 || exit 1

CMD ["nginx", "-g", "daemon off;"]