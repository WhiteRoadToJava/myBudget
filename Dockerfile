# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY . .

RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Stage 2: Production
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]