# Multi-stage build — secrets chỉ từ Railway Variables lúc RUN (không ARG/ENV nhạy cảm lúc build).
# Tránh cảnh báo Docker SecretsUsedInArgOrEnv từ Nixpacks auto-Dockerfile.

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw -DskipTests package -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/sales-0.0.1-SNAPSHOT.jar app.jar

# Alpine mặc định không UTF-8 — cần cho tiếng Việt (.env, mail, JSON).
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70.0", "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-jar", "app.jar"]
