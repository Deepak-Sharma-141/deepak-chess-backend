# ========================
# 1. Build Stage
# ========================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies first (to use Docker cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the source code
COPY src ./src

# Package the application (skip tests for faster builds; remove -DskipTests if needed)
RUN mvn clean package -DskipTests

# ========================
# 2. Runtime Stage
# ========================
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy jar file from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose port (same as in application.yml, usually 8080)
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
