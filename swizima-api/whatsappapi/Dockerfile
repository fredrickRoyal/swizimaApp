# syntax=docker/dockerfile:1
# =============================================================================
# Standalone Dockerfile for Swizima WhatsApp Relay Service (DigitalOcean)
# Build locally or on DigitalOcean App Platform / Droplet:
#   docker build -t fredrickkasoma/swizima-whatsappapi:latest .
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests || true
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S swizima && adduser -S swizima -G swizima
COPY --from=builder /build/target/*.jar /app/app.jar
USER swizima
EXPOSE 8088
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/app/app.jar"]

