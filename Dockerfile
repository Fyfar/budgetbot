# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -B package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
# Warm JVM class-share archive to cut class-loading overhead at startup
RUN java -Xshare:dump 2>/dev/null || true
COPY --from=build /app/target/budgetbot-2.0.0.jar /app/app.jar
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
