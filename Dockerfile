# ==================== Stage 1: Maven Build ====================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# 先复制 pom.xml，利用 Docker 层缓存加速
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

# 复制源码并构建（跳过测试，测试在 CI 环节已完成）
COPY src ./src
RUN mvn package -DskipTests -B -q

# ==================== Stage 2: Runtime ====================
FROM eclipse-temurin:21-jre-alpine AS runtime

# 安全：非 root 运行
RUN addgroup -g 1000 jinfu && \
    adduser -u 1000 -G jinfu -D jinfu

# 日志目录
RUN mkdir -p /var/log/jinfu-sys && chown -R jinfu:jinfu /var/log/jinfu-sys

WORKDIR /app

# 从构建阶段复制 fat jar
COPY --from=build /build/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

USER jinfu:jinfu

EXPOSE 8080

# JVM 参数可通过 JAVA_OPTS 环境变量覆盖
ENTRYPOINT exec java \
    ${JAVA_OPTS:--Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200} \
    -Djava.security.egd=file:/dev/./urandom \
    -jar app.jar
