# Multi-stage Dockerfile for production
# Builder stage: use Maven with Temurin 25 to build the fat JAR
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
# Copy maven files first for better caching
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage: minimal Temurin JRE
FROM eclipse-temurin:25-jre

WORKDIR /app

# Install curl for healthcheck (kept minimal)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r app && useradd -r -g app app

# Copy the application JAR from the builder stage (match snapshot artifact)
ARG JAR_FILE=target/*-SNAPSHOT.jar
COPY --from=builder /workspace/${JAR_FILE} /app/app.jar

# Ensure correct ownership
RUN chown -R app:app /app

# Run as non-root
USER app

# JVM options configurable at runtime via environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

# Simple HTTP healthcheck against root (adjust to /actuator/health if actuator enabled)
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8081/ || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

