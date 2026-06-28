# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY src ./src
COPY pom.xml .
RUN mvn package -Dmaven.test.skip=true -B

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/logs /app/static

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
