FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --chown=spring:spring config-server-*.jar app.jar

USER spring:spring

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
