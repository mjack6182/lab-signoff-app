# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build configuration
COPY gradle gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy application source
COPY src src/

# Build the application
RUN ./gradlew clean bootJar --no-daemon -x test

# Verify build output
RUN echo "Build artifacts:" && ls -lh build/libs/

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks (optional but useful)
RUN apk add --no-cache curl

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Verify JAR exists
RUN ls -lh app.jar && echo "JAR file ready"

# Expose port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport"

# Run application
# Use exec form to ensure proper signal handling
CMD exec java $JAVA_OPTS \
  -Dserver.port=${PORT:-8080} \
  -jar app.jar
