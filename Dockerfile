# syntax=docker/dockerfile:1

# Build stage: compile the Spring Boot app
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

# Build sources
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# Runtime stage: slim JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built JAR and static assets
COPY --from=build /workspace/target/*.jar /app/app.jar
COPY src/main/resources/static /app/static

# Container runtime options
ENV JAVA_OPTS=""

# Render/Heroku-like platforms inject $PORT; default to 8081 locally
EXPOSE 8081

# Start the app; serve static files from /app/static; disable Mongo sync for faster boot
CMD ["bash","-lc","java $JAVA_OPTS -jar /app/app.jar --server.port=${PORT:-8081} --mongo.sync.enabled=false --spring.web.resources.static-locations=file:/app/static/,classpath:/static/"]
