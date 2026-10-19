# syntax=docker/dockerfile:1
# =====================================================================
# 校园二手交易系统 · 后端镜像（多阶段构建）
# 构建：docker build -t campus-backend .
# 运行：docker run -p 8080:8080 campus-backend
# =====================================================================

# ---------- 阶段一：Maven 编译 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只复制各模块的 pom 预热依赖缓存：代码变更时无需重新下载依赖
COPY pom.xml .
COPY ruoyi-admin/pom.xml      ruoyi-admin/
COPY ruoyi-common/pom.xml     ruoyi-common/
COPY ruoyi-framework/pom.xml  ruoyi-framework/
COPY ruoyi-generator/pom.xml  ruoyi-generator/
COPY ruoyi-quartz/pom.xml     ruoyi-quartz/
COPY ruoyi-system/pom.xml     ruoyi-system/
COPY ruoyi-campus/pom.xml     ruoyi-campus/
RUN mvn -B -q dependency:go-offline -DskipTests || true

# 再复制源码正式构建
COPY . .
RUN mvn -B clean package -DskipTests

# ---------- 阶段二：JRE 运行 ----------
FROM eclipse-temurin:17-jre-alpine AS runtime

# 时区与中文字体（字典/时间格式化依赖）
RUN apk add --no-cache tzdata \
 && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
 && echo "Asia/Shanghai" > /etc/timezone

# 以非 root 用户运行，避免容器逃逸时直接拿到 root
RUN addgroup -S campus && adduser -S campus -G campus
WORKDIR /app

COPY --from=build /build/ruoyi-admin/target/ruoyi-admin.jar app.jar
RUN chown -R campus:campus /app
USER campus

EXPOSE 8080

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 健康检查：alpine 自带 busybox nc，做一次 TCP 探测即可判断进程是否在监听
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD nc -z 127.0.0.1 8080 || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]