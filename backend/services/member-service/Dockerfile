FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle gradle.properties ./
COPY services/config-server/build.gradle services/config-server/build.gradle
COPY services/gateway-service/build.gradle services/gateway-service/build.gradle
COPY services/core-service/build.gradle services/core-service/build.gradle
COPY services/member-service/build.gradle services/member-service/build.gradle

RUN chmod +x gradlew \
    && ./gradlew :services:member-service:dependencies --no-daemon

COPY services/member-service/src services/member-service/src

RUN ./gradlew :services:member-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder --chown=spring:spring \
    /workspace/services/member-service/build/libs/member-service-*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
