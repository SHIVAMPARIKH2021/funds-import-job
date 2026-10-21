FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user setup
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy directly with target ownership (eliminates separate RUN chown layer)
COPY --chown=appuser:appgroup build/libs/*.jar app.jar

USER appuser

# Container-friendly JVM memory limits and OOM safety
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]