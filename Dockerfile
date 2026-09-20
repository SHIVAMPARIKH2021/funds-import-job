# Stage 1: Build JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Make gradlew executable and build the bootJar (skipping tests for quick container build)
RUN chmod +x ./gradlew && ./gradlew bootJar -x test --no-daemon

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]