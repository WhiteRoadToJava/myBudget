# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy build files
COPY . .

# Build the JAR (skip tests for speed)
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the production image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the built JAR from the first stage
COPY --from=build /app/target/*.jar app.jar

# Run as a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]