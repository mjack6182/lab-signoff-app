# Use Eclipse Temurin JDK 21 for building
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./gradlew clean bootJar --no-daemon -x test

# Verify JAR was created
RUN ls -la /app/build/libs/

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port (Railway will provide PORT env var)
EXPOSE 8080

# Set Java options for production
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Railway provides PORT environment variable - Spring Boot will use SERVER_PORT
# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
