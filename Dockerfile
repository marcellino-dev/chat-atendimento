# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q clean package -DskipTests 2>/dev/null || \
    (apt-get install -y maven 2>/dev/null || apk add --no-cache maven) && \
    mvn -q clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
