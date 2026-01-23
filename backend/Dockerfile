# 1. 构建阶段：使用 Maven 镜像进行编译
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# 先拷贝 pom.xml 下载依赖（利用缓存机制，加快后续构建）
COPY pom.xml .

RUN mvn dependency:go-offline -B

# 拷贝源代码并打包
COPY src ./src
# 跳过测试打包，加快速度
RUN mvn clean package -Dmaven.test.skip=true

# 2. 运行阶段：使用精简的 JRE 运行 jar 包
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 从构建阶段拷贝生成的 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 暴露后端端口 (对应 application.yml 中的 8080)
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]