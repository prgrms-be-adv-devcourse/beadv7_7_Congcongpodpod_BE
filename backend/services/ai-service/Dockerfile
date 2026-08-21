FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle gradle.properties ./
COPY services/config-server/build.gradle services/config-server/build.gradle
COPY services/gateway-service/build.gradle services/gateway-service/build.gradle
COPY services/core-service/build.gradle services/core-service/build.gradle
COPY services/member-service/build.gradle services/member-service/build.gradle
COPY services/payment-service/build.gradle services/payment-service/build.gradle
COPY services/ai-service/build.gradle services/ai-service/build.gradle
COPY modules/api-common/build.gradle modules/api-common/build.gradle
COPY modules/mvc-common/build.gradle modules/mvc-common/build.gradle
COPY modules/event-common/build.gradle modules/event-common/build.gradle
COPY modules/outbox/build.gradle modules/outbox/build.gradle
COPY modules/inbox/build.gradle modules/inbox/build.gradle
COPY modules/s3-storage/build.gradle modules/s3-storage/build.gradle

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew :services:ai-service:dependencies --no-daemon

COPY modules/api-common/src modules/api-common/src
COPY modules/mvc-common/src modules/mvc-common/src
COPY modules/event-common/src modules/event-common/src
COPY modules/outbox/src modules/outbox/src
COPY modules/s3-storage/src modules/s3-storage/src
COPY services/ai-service/src services/ai-service/src

RUN ./gradlew :services:ai-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder --chown=spring:spring \
    /workspace/services/ai-service/build/libs/ai-service-*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
