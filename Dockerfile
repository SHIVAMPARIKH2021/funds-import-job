# Stage 1 - Build the Spring Boot bootJar
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy only the build definition first (not the sources) so this layer stays
# cached as long as the build scripts do not change.
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# Warm the Gradle dependency cache (layer cache + persistent cache mount).
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies --console=plain

# Copy the sources and build the boot JAR. Tests are executed by CI
# (Jenkinsfile 'Gradle clean build' stage), so they are skipped here.
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test --console=plain

# Stage 2 - Minimal JRE runtime image (Alpine keeps the image lightweight)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /workspace/build/libs/*.jar app.jar
USER appuser
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]