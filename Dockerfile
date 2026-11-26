# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1: Build all 6 Spring Boot APIs at once (shared Maven reactor & cache)
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS api-builder
WORKDIR /workspace/swizima-api

# Copy only pom.xml files first to leverage Docker layer caching for dependencies
COPY swizima-api/apigateway/pom.xml apigateway/pom.xml
COPY swizima-api/clientapi/pom.xml clientapi/pom.xml
COPY swizima-api/configurationsapi/pom.xml configurationsapi/pom.xml
COPY swizima-api/paymentsapi/pom.xml paymentsapi/pom.xml
COPY swizima-api/clienttransactionsapi/pom.xml clienttransactionsapi/pom.xml
COPY swizima-api/usersapi/pom.xml usersapi/pom.xml
COPY swizima-api/stafftrackapi/pom.xml stafftrackapi/pom.xml
COPY swizima-api/filesapi/pom.xml filesapi/pom.xml

# Generate a reactor pom.xml to build all 8 APIs in a single Maven invocation
RUN cat <<'EOF' > pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.swizima</groupId>
    <artifactId>swizima-api-reactor</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <modules>
        <module>apigateway</module>
        <module>clientapi</module>
        <module>configurationsapi</module>
        <module>paymentsapi</module>
        <module>clienttransactionsapi</module>
        <module>usersapi</module>
        <module>stafftrackapi</module>
        <module>filesapi</module>
    </modules>
</project>
EOF

# Pre-fetch dependencies for all modules
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 1C dependency:go-offline -DskipTests || true

# Copy all API sources and package all 8 executable JARs at once
COPY swizima-api/apigateway/src apigateway/src
COPY swizima-api/clientapi/src clientapi/src
COPY swizima-api/configurationsapi/src configurationsapi/src
COPY swizima-api/paymentsapi/src paymentsapi/src
COPY swizima-api/clienttransactionsapi/src clienttransactionsapi/src
COPY swizima-api/usersapi/src usersapi/src
COPY swizima-api/stafftrackapi/src stafftrackapi/src
COPY swizima-api/filesapi/src filesapi/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 1C clean package -DskipTests

# =============================================================================
# Base Runtime Stage (Java 21 JRE Alpine + non-root user)
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS api-base
WORKDIR /app
RUN addgroup -S swizima && adduser -S swizima -G swizima
USER swizima

# =============================================================================
# Individual API Images (one target per service)
# =============================================================================

# 1. API Gateway (9080) -> fredrickkasoma/swizima-apigateway
FROM api-base AS apigateway
COPY --from=api-builder /workspace/swizima-api/apigateway/target/*.jar /app/app.jar
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 2. Client API (8081) -> fredrickkasoma/swizima-clientapi
FROM api-base AS clientapi
COPY --from=api-builder /workspace/swizima-api/clientapi/target/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 3. Configurations API (8082) -> fredrickkasoma/swizima-configurationsapi
FROM api-base AS configurationsapi
COPY --from=api-builder /workspace/swizima-api/configurationsapi/target/*.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 4. Payments API (8083) -> fredrickkasoma/swizima-paymentsapi
FROM api-base AS paymentsapi
COPY --from=api-builder /workspace/swizima-api/paymentsapi/target/*.jar /app/app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 5. Client Transactions API (8084) -> fredrickkasoma/swizima-clienttransactionsapi
FROM api-base AS clienttransactionsapi
COPY --from=api-builder /workspace/swizima-api/clienttransactionsapi/target/*.jar /app/app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 6. Users API (8085) -> fredrickkasoma/swizima-usersapi
FROM api-base AS usersapi
COPY --from=api-builder /workspace/swizima-api/usersapi/target/*.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 7. StaffTrack API (8086) -> fredrickkasoma/swizima-stafftrackapi
FROM api-base AS stafftrackapi
COPY --from=api-builder /workspace/swizima-api/stafftrackapi/target/*.jar /app/app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 8. Files API (8087) -> fredrickkasoma/swizima-filesapi
FROM api-base AS filesapi
COPY --from=api-builder /workspace/swizima-api/filesapi/target/*.jar /app/app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
