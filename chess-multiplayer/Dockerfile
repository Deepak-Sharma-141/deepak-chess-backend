# Step 1: Use Maven image to build the project
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom.xml first
COPY chess-multiplayer/pom.xml .

# Copy the entire source code
COPY chess-multiplayer/src ./src

# Build the project
RUN mvn clean package -DskipTests

# Step 2: Use lightweight JDK image to run the app
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
