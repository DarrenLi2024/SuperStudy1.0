# ---- Build Stage ----
FROM maven:3.8.5-openjdk-8-slim AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM openjdk:8-jre-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/logs /app/static

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
