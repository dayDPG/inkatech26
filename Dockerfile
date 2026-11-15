# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=build --chown=app:app \
    /workspace/target/detective-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
ENV PORT=10000 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
