FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle gradle.properties ./
COPY services/config-server/build.gradle services/config-server/build.gradle

RUN chmod +x gradlew \
    && ./gradlew :services:config-server:dependencies --no-daemon

COPY services/config-server/src services/config-server/src

RUN ./gradlew :services:config-server:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder --chown=spring:spring \
    /workspace/services/config-server/build/libs/config-server-*.jar app.jar

USER spring:spring

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
